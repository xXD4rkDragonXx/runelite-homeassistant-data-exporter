# Gotchas

0 traps, grouped into zero files, found while building <!-- FILL: project name -->. Each is written up
once under a stable ID and referenced by ID from everywhere else, so there is exactly one place to edit
when something changes. <!-- FILL: one sentence naming the shared layer the traps are true of, e.g.
"Plugin-agnostic: things that are true of the client or the API and will bite the next plugin as well as
this one." -->

**Where a fact goes.** This is the one statement of the routing rule; skills and CLAUDE.md cite it.

| The behaviour is… | Goes in |
|---|---|
| True of <!-- FILL: the shared layer (framework, API, platform, toolchain) -->, and will bite the next <!-- FILL: unit, e.g. plugin / service / module --> | a numbered entry here |
| True of one <!-- FILL: unit --> only | <!-- FILL: that unit's own doc, e.g. its `ARCHITECTURE.md` § "Expect these" --> |
| Read in the docs but never seen live | [open-questions.md](open-questions.md), no ID, not indexed |
| Something that simply works, confirmed live | <!-- FILL: where confirmations go, e.g. that unit's `RUNS.md`, dated --> |

**How to use the index.** Match the symptom in the table at the bottom, then open the one file that entry
points to. The symptom column is deliberately specific enough to rule an entry out without opening anything.
Do not read this directory wholesale: the index is the discovery mechanism, and loading every area file
defeats the point of them being separate.

| File | Covers | Entries |
|---|---|---|
| [open-questions.md](open-questions.md) | read from docs, not yet observed — no IDs, not in the index | — |

**Source key.** Every entry ends with the source it was settled from:
`` `DOCS` `` = official documentation, `` `SOURCE` `` = read off the source code or shipped binary,
`` `OBSERVED` `` = seen live, which says where and on what date. An entry without `` `OBSERVED` `` is read
from docs and not yet confirmed in practice. <!-- FILL: keep, rename or add keys for this project, then
delete this marker -->

**Adding one.** IDs are permanent: a new entry takes the next free number in its prefix
(`python3 tools/check_gotchas.py next PREFIX`), and nothing is ever renumbered. Put it in the area file in
ID order, opening with its bold symptom sentence: that entry is the one statement of the trap. The area
file's table, the index below, the count in the first sentence of this file and the file table's counts
are written from the entries by `python3 tools/check_gotchas.py write`, in the same change. Then
`python3 tools/check_gotchas.py` must print `OK`. The `gotcha` skill walks this, including a trap that
fits no existing file, which does need a row in the file table above by hand.

**Citations.** Cite an entry by its ID wherever the trap is worked around: a code comment
(`// See PREFIX-N: what the trap is`), a commit message, another doc. The check reads every file git tracks
and fails on a retired ID cited anywhere, except in `.claude/skills/gotcha/` (the skill's own examples
belong to no project).

## Index

| ID | Area | Symptom |
|---|---|---|

## Retired IDs

Not traps, and never to be cited: these numbers were merged into the entry named, or moved out, and
`tools/check_gotchas.py` fails any mention of them outside this table. The table exists so a citation in
git history or an old chat can still be resolved. A retired number is never reused.

| Retired | Now |
|---|---|
