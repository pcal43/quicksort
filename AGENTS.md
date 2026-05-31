# Quicksort

Quicksort is a Minecraft mod that provides lightweight, low-effort item
sorting for vanilla-style storage systems.

A Quicksorter automatically distributes items into nearby chests that already
contain matching items. The goal is to make workshop organization easier
without requiring complex redstone item sorter designs or introducing new
storage blocks. Quicksort is designed to be simple, intuitive, and compatible
with vanilla clients. :contentReference[oaicite:0]{index=0}

## Design Philosophy

When making changes, prioritize:

1. Simplicity over complexity.
2. Vanilla-friendly gameplay.
3. Ease of use.
4. Server performance.
5. Predictable item routing behavior.

Quicksort exists to reduce the effort required to organize storage systems.

Avoid turning the mod into a comprehensive logistics, storage, or automation
framework.

Favor intuitive behavior that players can understand without extensive
documentation.

Prefer solutions that reduce player effort while preserving the feel of
vanilla Minecraft. :contentReference[oaicite:1]{index=1}

## Repository Scope

This repository contains the source code for Quicksort.

Only inspect files tracked by git.

Ignore:

- `build/`
- `.gradle/`
- `run/`
- `logs/`
- generated resources
- IDE metadata
- temporary files
- crash reports

Do not spend time analyzing generated files or build outputs.

## Cost-Aware Development

Repository-wide scans are expensive and should be avoided.

Before exploring the repository:

- Prefer targeted analysis.
- Read only files likely to be relevant.
- Start from files explicitly mentioned in the task.
- Follow references outward only as needed.
- Do not read entire directory trees unless necessary.

When discovering files, prefer:

```bash
git ls-files