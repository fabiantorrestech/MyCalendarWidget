package com.fabiantorrestech.mycalendarwidget.widget.density

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.fabiantorrestech.mycalendarwidget.data.density.LaneRect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Integer rectangle, left/top inclusive and right/bottom exclusive.
 *
 * Not [android.graphics.Rect]: under `testDebugUnitTest` the stubbed android.jar accepts
 * `Rect(0, 8, 600, 64)` and then reports every field as 0, so the geometry would be
 * untestable. All arithmetic happens here and [DensityCanvas] converts with [toRect]
 * only at draw time.
 */
data class IntRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    fun toRect(): Rect = Rect(left, top, right, bottom)
}

/** What fills the track: merged shapes today, per-event lanes in Tonal/Detail. */
sealed interface StripContent {
    /** Merged busy blocks as fractions of the window, all in one accent. */
    data class Shape(
        val blocks: List<ClosedFloatingPointRange<Float>>,
        val colorInt: Int
    ) : StripContent

    /**
     * One rect per event slice. [widthFractionOf] maps an epoch-millis instant to its
     * fraction of the window. [laneOutlineColor] strokes every rect at `depth > 1` (the
     * background-coloured split line Tonal draws between overlapping events);
     * [edgeColors] strokes the Detail contrast guard instead, keyed by the rect's index
     * in [rects] rather than carried on [LaneRect] itself, which stays a pure, mode-free
     * value from `data/density`.
     */
    data class Lanes(
        val rects: List<LaneRect>,
        val widthFractionOf: (Long) -> Float,
        val laneOutlineColor: Int?,
        val edgeColors: Map<Int, Int> = emptyMap()
    ) : StripContent
}

/** The dimmed, blurred backdrop behind the peek sheet. Consumed by the peek task. */
data class GhostSpec(
    val backgroundColor: Int,
    val dimToward: Float = 0.6f,
    val blurFactor: Int = 8
)

/**
 * Everything [DensityCanvas.renderStrip] needs, in device pixels.
 *
 * [overhangPx] is the caret's break-out above and below the track (0 until the caret
 * task turns it on) and [haloPx] the background-coloured margin that keeps the caret
 * legible where it crosses a busy block.
 */
data class StripSpec(
    val widthPx: Int,
    val trackHeightPx: Int,
    val overhangPx: Int,
    val haloPx: Int,
    val content: StripContent,
    val nowFraction: Float?,
    val freeColor: Int,
    val nowColor: Int,
    val nowMarkerWidthPx: Int,
    val backgroundColor: Int,
    /** Device pixels per dp, for lane metrics that are specified in dp (G7). */
    val pxPerDp: Float,
    val ghost: GhostSpec? = null
) {
    val heightPx: Int get() = trackHeightPx + 2 * overhangPx
}

/** The look-ahead day bars. Consumed by the day-bars task. */
data class LoadBarsSpec(
    val widthPx: Int,
    val heightPx: Int,
    val loads: List<Float>,
    val gutterPx: Int,
    val fillColor: Int,
    val trackColor: Int
)

/**
 * Draws the density strip with plain [android.graphics] — no Glance and no Compose — so
 * the widget and the settings preview render byte-identical bitmaps from one spec.
 *
 * Rect paints are deliberately un-antialiased and every x edge is rounded to a whole
 * pixel: the strip is scaled to its view box with `FillBounds`, and a fractional edge
 * would smear into a grey seam.
 */
object DensityCanvas {

    /** Above this the bitmap is scaled down rather than risking a RemoteViews rejection. */
    const val MAX_BITMAP_BYTES = 512 * 1024

    private const val LANE_GAP_DP = 2f
    private const val LANE_MIN_WIDTH_DP = 2f
    private const val LANE_END_INSET_DP = 1f

    /** Both the Tonal lane-split outline and the Detail contrast edge are 1dp (G7). */
    private const val OUTLINE_INSET_DP = 1f

    /** `(0, overhang, width, overhang + track)`. */
    fun trackRect(spec: StripSpec): IntRect =
        IntRect(0, spec.overhangPx, spec.widthPx, spec.overhangPx + spec.trackHeightPx)

    /**
     * The now-marker as `(halo, caret)`, both spanning the full bitmap height, or null
     * when now is outside the window. The caret is nudged inwards at the very edges so
     * its halo never falls off the bitmap.
     */
    fun markerRects(spec: StripSpec): Pair<IntRect, IntRect>? {
        val fraction = spec.nowFraction?.coerceIn(0f, 1f) ?: return null
        val marker = max(1, spec.nowMarkerWidthPx)
        val centre = fraction * spec.widthPx
        val maxLeft = max(spec.haloPx, spec.widthPx - spec.haloPx - marker)
        val left = (centre - marker / 2f).roundToInt().coerceIn(spec.haloPx, maxLeft)
        val caret = IntRect(left, 0, left + marker, spec.heightPx)
        val halo = IntRect(
            (caret.left - spec.haloPx).coerceAtLeast(0),
            0,
            (caret.right + spec.haloPx).coerceAtMost(spec.widthPx),
            spec.heightPx
        )
        return halo to caret
    }

    /**
     * The strip as a transparent-backed ARGB bitmap: free track, then the busy content
     * clipped to the track, then the now-marker on top. Oversized specs are scaled down
     * uniformly so the result stays inside [MAX_BITMAP_BYTES].
     */
    fun renderStrip(spec: StripSpec): Bitmap {
        val scaled = withinBudget(spec)
        val bitmap = Bitmap.createBitmap(
            max(1, scaled.widthPx),
            max(1, scaled.heightPx),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        val track = trackRect(scaled)
        paint.color = scaled.freeColor
        canvas.drawRect(track.toRect(), paint)

        val saved = canvas.save()
        canvas.clipRect(track.toRect())
        when (val content = scaled.content) {
            is StripContent.Shape -> drawShape(canvas, paint, scaled, track, content)
            is StripContent.Lanes -> drawLanes(canvas, paint, scaled, track, content)
        }
        canvas.restoreToCount(saved)

        markerRects(scaled)?.let { (halo, caret) ->
            paint.color = scaled.backgroundColor
            canvas.drawRect(halo.toRect(), paint)
            paint.color = scaled.nowColor
            canvas.drawRect(caret.toRect(), paint)
        }
        return bitmap
    }

    private fun drawShape(
        canvas: Canvas,
        paint: Paint,
        spec: StripSpec,
        track: IntRect,
        content: StripContent.Shape
    ) {
        paint.color = content.colorInt
        content.blocks.forEach { block ->
            val left = xOf(block.start, spec.widthPx)
            val right = max(left + 1, xOf(block.endInclusive, spec.widthPx))
            canvas.drawRect(IntRect(left, track.top, right, track.bottom).toRect(), paint)
        }
    }

    private fun drawLanes(
        canvas: Canvas,
        paint: Paint,
        spec: StripSpec,
        track: IntRect,
        content: StripContent.Lanes
    ) {
        if (content.rects.isEmpty()) return
        val unit = spec.pxPerDp
        val gap = (LANE_GAP_DP * unit).roundToInt()
        val minWidth = max(1, (LANE_MIN_WIDTH_DP * unit).roundToInt())
        val endInset = max(1, (LANE_END_INSET_DP * unit).roundToInt())
        val outlineInset = max(1, (OUTLINE_INSET_DP * unit).roundToInt())

        // Deeper lanes (higher `lane`) are drawn first so a shallower lane's fill and
        // strokes always win any pixel their 1dp insets might otherwise share.
        content.rects.withIndex()
            .sortedByDescending { (_, rect) -> rect.lane }
            .forEach { (index, rect) ->
                val depth = max(1, rect.depth)
                val laneHeight = max(1, (spec.trackHeightPx - (depth - 1) * gap) / depth)
                val top = track.top + rect.lane * (laneHeight + gap)
                val bottom = min(track.bottom, top + laneHeight)

                val left = xOf(content.widthFractionOf(rect.startMillis), spec.widthPx)
                var right = xOf(content.widthFractionOf(rect.endMillis), spec.widthPx)
                if (rect.isEventEnd) right -= endInset
                right = max(left + minWidth, right)

                val bounds = IntRect(left, top, right, bottom)

                paint.style = Paint.Style.FILL
                paint.color = rect.colorInt
                canvas.drawRect(bounds.toRect(), paint)

                if (content.laneOutlineColor != null && depth > 1) {
                    strokeInset(canvas, paint, bounds, outlineInset, content.laneOutlineColor)
                }
                content.edgeColors[index]?.let { edgeColor ->
                    strokeInset(canvas, paint, bounds, outlineInset, edgeColor)
                }
            }
        paint.style = Paint.Style.FILL
    }

    /** Strokes [bounds] shrunk by [insetPx] on every side, in [color]. */
    private fun strokeInset(canvas: Canvas, paint: Paint, bounds: IntRect, insetPx: Int, color: Int) {
        val left = min(bounds.left + insetPx, bounds.right)
        val top = min(bounds.top + insetPx, bounds.bottom)
        val right = max(bounds.right - insetPx, left)
        val bottom = max(bounds.bottom - insetPx, top)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = insetPx.toFloat()
        paint.color = color
        canvas.drawRect(IntRect(left, top, right, bottom).toRect(), paint)
        paint.style = Paint.Style.FILL
    }

    /** Fractions land on whole pixels: `FillBounds` would otherwise blur a half pixel. */
    private fun xOf(fraction: Float, widthPx: Int): Int =
        (fraction.coerceIn(0f, 1f) * widthPx).roundToInt().coerceIn(0, widthPx)

    /**
     * The N look-ahead day bars: [LoadBarsSpec.loads.size] equal-width track rects, each
     * a [LoadBarsSpec.trackColor] column filled from its left edge by a
     * [LoadBarsSpec.fillColor] rect [width][IntRect.width] `round(load * columnWidth)`
     * wide. A transparent 1×1 bitmap stands in for "nothing to draw" (no lookahead days,
     * or a zero/negative width) rather than a crashing `Bitmap.createBitmap(0, …)`.
     */
    fun renderLoadBars(spec: LoadBarsSpec): Bitmap {
        val scaled = withinBudget(spec)
        if (scaled.loads.isEmpty() || scaled.widthPx <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val bitmap = Bitmap.createBitmap(
            max(1, scaled.widthPx),
            max(1, scaled.heightPx),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        paint.color = scaled.trackColor
        loadBarTrackRects(scaled).forEach { canvas.drawRect(it.toRect(), paint) }

        paint.color = scaled.fillColor
        loadBarFillRects(scaled).forEach { canvas.drawRect(it.toRect(), paint) }

        return bitmap
    }

    /**
     * [LoadBarsSpec.loads.size] equal-width columns spanning the bitmap, [gutterPx]
     * apart — pure integer arithmetic (no [Bitmap]) so it can be unit-tested directly,
     * the same split [renderStrip] uses for [trackRect] and [markerRects]. Empty when
     * there are no loads or no width to divide.
     */
    fun loadBarTrackRects(spec: LoadBarsSpec): List<IntRect> {
        val n = spec.loads.size
        if (n <= 0 || spec.widthPx <= 0) return emptyList()
        val colW = (spec.widthPx - spec.gutterPx * (n - 1)) / n
        return (0 until n).map { i ->
            val left = i * (colW + spec.gutterPx)
            IntRect(left, 0, left + colW, spec.heightPx)
        }
    }

    /**
     * One fill rect per [loadBarTrackRects] column: same bounds, but clipped from the
     * left to `round(load * columnWidth)` — a load of 0 draws nothing, 1 fills the whole
     * column. [LoadBarsSpec.loads] outside `0f..1f` are clamped rather than over/under
     * filling the column.
     */
    fun loadBarFillRects(spec: LoadBarsSpec): List<IntRect> =
        loadBarTrackRects(spec).zip(spec.loads).map { (track, load) ->
            val fillWidth = (load.coerceIn(0f, 1f) * track.width).roundToInt()
            IntRect(track.left, track.top, track.left + fillWidth, track.bottom)
        }

    /** The load-bars analogue of [withinBudget]: scales width/height/gutter together. */
    private fun withinBudget(spec: LoadBarsSpec): LoadBarsSpec {
        val bytes = spec.widthPx.toFloat() * spec.heightPx.toFloat() * 4f
        if (bytes <= MAX_BITMAP_BYTES || bytes <= 0f) return spec
        val scale = min(1f, sqrt(MAX_BITMAP_BYTES / bytes))
        return spec.copy(
            widthPx = max(1, (spec.widthPx * scale).toInt()),
            heightPx = max(1, (spec.heightPx * scale).toInt()),
            gutterPx = (spec.gutterPx * scale).toInt()
        )
    }

    /** Scales every dimension by the same factor when the bitmap would be too large. */
    private fun withinBudget(spec: StripSpec): StripSpec {
        val bytes = spec.widthPx.toFloat() * spec.heightPx.toFloat() * 4f
        if (bytes <= MAX_BITMAP_BYTES || bytes <= 0f) return spec
        val scale = min(1f, sqrt(MAX_BITMAP_BYTES / bytes))
        return spec.copy(
            widthPx = max(1, (spec.widthPx * scale).toInt()),
            trackHeightPx = max(1, (spec.trackHeightPx * scale).toInt()),
            overhangPx = (spec.overhangPx * scale).toInt(),
            haloPx = (spec.haloPx * scale).toInt(),
            nowMarkerWidthPx = max(1, (spec.nowMarkerWidthPx * scale).toInt()),
            pxPerDp = spec.pxPerDp * scale
        )
    }
}
