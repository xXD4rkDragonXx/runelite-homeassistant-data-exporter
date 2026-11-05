---
name: gotcha
description: Record a trap in the project's gotcha registry (docs/gotchas/) — a numbered, symptom-first entry with a stable ID, so the next person or session recognises it from what they see and skips the lost time. Use whenever behaviour of a shared framework, API, library, platform or toolchain has cost real time (roughly ten minutes or more) and would bite the next component too, not just the one being worked on. Also use when the project has no registry yet and such a trap turns up (bootstrap one), when an open question is confirmed or refuted live, when two entries overlap and should be merged, and whenever the user says "gotcha", "trap", "footgun", "note this for next time", "that bit us again" or "write that down".
metadata:
  version: "2026-11-05"
  upstream: https://github.com/RedFirebreak/agent-forge/tree/main/plugins/gotcha
---

# Gotcha

The point of a gotcha is that **the next person matches it from the symptom alone** and does not lose the
time again. Everything below serves that: one entry per trap, one stable ID, the symptom phrased as it
looks from outside, and an index that lets someone rule entries out without opening them.

The registry lives in `docs/gotchas/`. Each trap is stated once: its `### ID` entry in its area file,
opening with the bold symptom sentence. Everything else that mentions it (the area file's table,
`docs/gotchas/README.md`'s index and counts) is written from the entries by `tools/check_gotchas.py
write`, so nothing has to be kept in step by hand. A `PostToolUse` hook runs the check after every
markdown edit, so a malformed entry announces itself immediately.

**IDs are permanent.** They get cited from code comments, commit messages, other docs and other entries.
A new entry takes the next free number in its prefix. Nothing is ever renumbered, and a retired number is
never reused.

**The project's README is the source of truth for everything project-specific**: the routing rule, which
area file owns which prefix, the source keys, how the tables get written. This skill deliberately holds no
copy of any of that, so nothing here can drift from it. Read the README's header (everything above
`## Index`) before starting.

Commands below say `python3`; use whichever Python 3.10 or later the machine has (`python` or `py` on
Windows).

## 0. Find the registry, or bootstrap one

If `docs/gotchas/README.md` exists, go to step 1.

If not, and the trap is worth recording, bootstrap. Run the skill's own copy of the script, from the
project root (`${CLAUDE_SKILL_DIR}` is the directory this file is in):

```bash
python3 "${CLAUDE_SKILL_DIR}/scripts/check_gotchas.py" init
```

That creates `docs/gotchas/README.md` and `open-questions.md` from templates and copies the validator to
`tools/check_gotchas.py`, where the project owns it from then on. Then:

1. **Fill every `<!-- FILL: ... -->` in the README.** The validator refuses to pass while any remain. Work
   out, from the codebase and the conversation, what the *shared layer* is (the framework, API or platform
   every component depends on), what the *unit* is (plugin, service, module, package…), and where facts
   about one unit and confirmations of things that work belong. If a guess would be shaky, ask the user one
   question rather than inventing a convention.
2. **Offer the hook.** `init` prints a `.claude/settings.json` snippet. Ask before merging it into the
   user's settings; it is what makes drift visible without anyone remembering to run the check.
3. **Only if several branches will add entries at the same time** (a team, or agent sessions working in
   parallel): offer "Written on merge" from the options below. On a solo project the default is right; do
   not ask.
4. Create the first area file (step 3, rung 3) for the trap in hand.
5. If the project has a `CLAUDE.md`, add one line pointing at `docs/gotchas/README.md` as the place to
   check before debugging something that smells like a framework quirk.

## 1. Does it belong here at all

Apply the routing table at the top of the README. The test for a numbered entry: **would it bite a
different unit doing a different job?** If it is only true of this one component, it goes in that
component's own doc as the README says. Something read in the docs but never seen happen goes in
`open-questions.md` as a bullet, with no ID. Something that simply works, confirmed live, goes wherever the
README sends confirmations.

## 2. Is it already there

Before minting an ID, scan the index's symptom column (it is written so you can rule entries out without
opening them), and grep the area files for the API names involved. If an existing entry is the same trap:

- **Extend it** instead: sharpen the mechanism, add the new case, add `OBSERVED` to its source if it was
  docs-only. If its symptom sentence was what failed to match what you saw, improve the symptom in the
  entry; the tables follow at the next `write`.
- If it is a *different face* of the same mechanism, keep one entry and describe both faces in it.

A duplicate is worse than no entry: the next reader finds one and trusts it while the other holds the fix.

## 3. Pick the area and prefix

The README's file table lists every area file, the prefixes it owns and what it covers. Take the lowest
honest rung:

1. **An existing prefix.** Read the `Covers` cells before deciding nothing fits; files are usually broader
   than their names.
2. **A new prefix in an existing file.** Right when the subject is distinct but the area is not. Numbering
   starts at 1, and that file's `Covers` cell in the README gains the prefix in backticks, before the `—`.
3. **A new area file.** Right when the subject belongs to no existing area and you expect siblings; a
   category that holds one entry for ever was a mistake. The validator discovers area files by globbing, so
   nothing in the script changes. Copy `assets/area.template.md` to `docs/gotchas/<area>.md`, fill its
   title (the index shows it as the area word) and its one-line description, and add its row to the
   README's file table (link, `PREFIX` — what it covers, entry count; `write` keeps the count). The index
   group and the file count in the opening sentence are written.

A trap forced into the least-bad file is worse than a new category: the index only works while the area
word predicts the contents. If you take rung 2 or 3, say so when you report back. A new category is a claim
about the shape of the problem space, and the human should get to disagree with it.

Prefixes are short uppercase words (`HTTP`, `AUTH`, `DB`), one owner file each.

## 4. Take the next number

```bash
python3 tools/check_gotchas.py next PREFIX     # e.g. HTTP-4; counts retired numbers too
python3 tools/check_gotchas.py ids             # highest in use for every prefix
```

With product prefixes (see the options below), the prefix includes the product: `next W-HTTP`.

## 5. Write the entry

Bold first sentence = **the symptom, as the person actually sees it**: the log line, the behaviour, the
thing that "doesn't work". Not the cause, and not the API name unless the API name is what they would be
looking at. Then what is really happening underneath and why the obvious reading is wrong. Then the fix,
concretely enough to write: the call that works, the state that is distinct, the check that must come first.
Close with a source line whose keys come from the README's source key.

```markdown
### HTTP-4
**A retry fires while the first request is still in flight, and the server records both.** The client's
timeout covers the connect phase only; a slow response never trips it, so the retry wrapper sees
"no response yet" and sends again. Set the read timeout explicitly, and make the retry idempotent with a
request key the server de-duplicates on. See also ([HTTP-2](#http-2)).

*Source: `OBSERVED` (billing-service, 2026-09-28)*
```

The quality bar: someone who has never seen this code, looking at only the symptom sentence, can say
"that's what I've got" or "not mine". If the symptom only makes sense once you know the cause, rewrite it.

Cross-reference by ID and link: `([HTTP-2](#http-2))` in the same file, `([DB-3](database.md#db-3))`
across files. Use `####` for any sub-heading inside an entry; `###` is reserved for IDs.

## 6. The edit

One: the `### ID` entry in the area file, placed in ID order (prefix alphabetically, then number). Then
write everything that follows from it, the file's table, the README's index row, the total in the opening
sentence and the file table's count:

```bash
python3 tools/check_gotchas.py write
```

If the README has a **Written on merge.** paragraph, skip `write` in a branch that becomes a pull
request: the default branch writes the tables after the merge, and leaving them out is what keeps two
pull requests from conflicting in them. Keep each file's existing line endings.

## 7. Validate

```bash
python3 tools/check_gotchas.py
```

It must print `OK`. Every failure names the file and what to change. Tables behind the entries are a
failure too (run `write`), unless the README says they are written on merge.

## Promoting an open question

When a live run settles an `open-questions.md` bullet:

- **It was a trap** → write it up as an entry with `` `OBSERVED` (<where>, <date>) ``, then delete the bullet.
- **It simply works** → record it where the README sends confirmations, dated. Delete the bullet.
- **It was wrong** → delete the bullet.

Either way the bullet goes, and check whether any other bullet refers to it.

## Merging, moving, retiring

When two entries turn out to be one trap, or an entry turns out to be about one unit only:

1. Merge the content into the surviving entry (the lower number, unless the other is clearly better
   written), or move it to the unit's doc.
2. Delete the retired entry's `###` block, then `write`; the tables and counts follow.
3. Add a row to the README's `## Retired IDs` table: `| OLD-ID | NEW-ID |`, or
   `| OLD-ID | moved: <where>, <date> |`.
4. Repoint every citation of the old ID in the **whole repository**. When the README has a
   **Citations.** paragraph (the template starts with one), the check scans every file git tracks and lists
   each one left. Without it the check only sees `docs/gotchas/`, so grep.

## Options for bigger repositories

Each is a paragraph in the project's README; the validator reads it and changes what it checks. Add one
only when the user agrees, and update the README's "Adding one" paragraph to match in the same edit.

**Written on merge.** For a repository where several branches add entries at the same time. The written
tables are where their pull requests would conflict, so the default branch writes them instead. Add to
the README, after "Adding one":

```markdown
**Written on merge.** The default branch writes the tables after every merge (CI runs
`python3 tools/check_gotchas.py write`), so a pull request adds its entry and leaves the tables alone, and
two pull requests never conflict in them.
```

and copy `assets/gotchas-write.yml` to `.github/workflows/` (or the equivalent for the project's CI). It
pushes to the default branch, so tell the user that branch protection must allow it. With the paragraph,
the check notes tables that are behind instead of failing on them.

**Product prefixes.** For a monorepo whose products share one registry. Every ID gains a one-letter
product prefix, `P-AREA-N`, and the check fails on any ID without one:

```markdown
**Product prefixes.** Every ID starts with the product whose work found it: `A` api, `W` web. So
`W-HTTP-3` is web's. A trap a second product meets is the same entry, extended, under its first ID; a new
one takes the prefix of the product that found it, and an area file can hold the prefixes of several
products.
```

Best adopted while the registry is new. On one that already has entries it renames every ID: add one
sentence to the paragraph saying how an old ID maps to its new one (for citations in git history), and the
citation scan lists every citation still in the old form (`HTTP-3` for `W-HTTP-3`).

## Keeping this skill honest

This skill is meant to change as the practice does. When the process itself changes, change the places
that encode it **in the same edit**: this `SKILL.md`, the validator (`scripts/check_gotchas.py` here and
the project's `tools/check_gotchas.py`), the README template in `assets/`, and the project README's
"Adding one" paragraph. Specifically:

- **A new kind of mistake cost time** (the validator passed but something was still wrong, or it failed in
  a way that took a while to understand): add a row to the table below. If it is mechanically checkable,
  add the check to the validator too, and say so when you report back.
- **The entry format or a convention changes** (a new column, a new required line, a new source key):
  update the template in `assets/`, the validator, and the relevant section here.
- **The project's copy of the validator gained a check that is not project-specific**: port it back to
  this skill's `scripts/check_gotchas.py` so the next project starts with it.
- **The registry is outgrowing its shape** (an area file past roughly 25 entries, or the index hard to scan):
  propose a split to the user instead of just doing it. Splitting moves entries between files but never
  changes their IDs.

**The version date.** `metadata.version` at the top of this file and `VERSION` in the validator hold the
date of the last change, and every copy keeps the date of the reference it came from. Whoever changes the
skill or the validator, here or in a project's copy, sets both dates to that day. Comparing a copy with the
reference at https://github.com/RedFirebreak/agent-forge then tells which way it differs: an older date has
updates to take, and a newer one has changes to port back (or to keep, if they are project-specific).
`python3 tools/check_gotchas.py --version` prints a project's.

This skill may live outside the repository: in the user's own skills directory, or in a plugin cache that
the next update replaces. Tell the user what you changed and where. If it was installed as a plugin, edit
nothing there; suggest the change upstream instead, at https://github.com/RedFirebreak/agent-forge.

Never edit this file to hold project facts (prefix lists, keys, paths beyond `docs/gotchas/`): those live
in the project's README, which the validator keeps honest.

## Report back

One or two lines: the ID, the file, the symptom sentence, and anything structural (a new prefix, a new area
file, a merged or retired ID, a change to this skill or the validator).

## Common mistakes

| Mistake | What happens |
|---|---|
| Reusing a freed or retired number | Breaks every existing citation of the old one. `next` always takes the next number up. |
| Minting a new entry for a known trap | Two half-answers; the reader finds one and trusts it. Search the index first (step 2). |
| Symptom describes the cause | Nobody matches it before they already know the answer. Phrase what is seen. |
| Entry appended to the end of the file | Validator fails on "entries are not in ID order". |
| Forgetting `write` | Validator fails on "tables are behind the entries", unless they are written on merge. |
| Hand-editing the index or a table | The next `write` overwrites it. Change the entry; the tables follow. |
| A new area file without its row in the README's file table | Validator fails on "no row in the file table". That row is the one table written by hand. |
| A unit-specific fact written up as a gotcha | Belongs in the unit's own doc. The test: would it bite a different unit doing a different job? |
| Retiring an ID while code still cites it | Code comments cite a number that now means nothing. The citation scan catches it when the README has a **Citations.** paragraph; without one, grep. |
