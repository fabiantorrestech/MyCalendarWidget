# 1. Record architecture decisions

Date: 2026-10-01

## Status

Accepted

## Context

BridgeCal has grown from one agenda widget into four widget styles, a peek sheet, automation hooks and, now, a persistent notification. The reasons behind the costlier choices (how the app stays alive, what it may read, which Android mechanisms it leans on) have so far lived in plan files outside the repository and in commit messages, where a future reader is unlikely to find them.

## Decision

We will keep Architecture Decision Records in `doc/adr/`, in the format Michael Nygard describes in "Documenting Architecture Decisions": one numbered Markdown file per decision, append-only, with superseded records linked to their replacements and an index generated into `doc/adr/README.md`.

## Consequences

The reasons for a decision that is expensive to undo travel with the code and can be reviewed in the same pull request. Each record costs a few minutes to write, and records must be superseded rather than edited, which takes some discipline. Cheap, easily reversed choices stay out of the log.
