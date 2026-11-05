#!/usr/bin/env python3
"""Keep a gotcha registry (docs/gotchas/) internally consistent.

Part of the `gotcha` skill: https://github.com/RedFirebreak/agent-forge (MIT licence).
VERSION below is the date of the last change, the same as the skill's `metadata.version`; a copy keeps
the date of the reference it came from, and whoever changes a copy sets it to that day.

    python3 check_gotchas.py [DIR]          validate; prints OK or every problem found
    python3 check_gotchas.py [DIR] write    write the tables from the entries: each area file's
                                            table, and the README's counts and index
    python3 check_gotchas.py [DIR] next PFX print the next free ID for a prefix (retired numbers count)
    python3 check_gotchas.py [DIR] ids      print the highest ID in use per prefix
    python3 check_gotchas.py init           scaffold docs/gotchas/ and copy this script to tools/
    python3 check_gotchas.py --version      print this copy's version date
    python3 check_gotchas.py --hook         PostToolUse hook mode: reads the event JSON from stdin
                                            and, after an edit to a Markdown file inside a registry
                                            (a `gotchas/` directory with a README.md), validates
                                            that registry; exit 2 + stderr on failure

DIR (or --dir DIR) is the registry directory; without it, docs/gotchas, searched for upward from
the project directory and the working directory. One copy of this script can serve every registry
of a repository. Area files are discovered by globbing *.md in the registry, minus README.md and
open-questions.md, so a new area needs no change here. Stdlib only; Python 3.10 or later.

An entry is the one statement of its trap: its `### ID` heading and the bold sentence that opens
it, the symptom. The area file's table and the README's index, counts and file-table entry counts
are written from the entries by `write`; the area word in the index is the area file's own title.
The check validates the entries and the hand-written parts of the README, and fails when a table is
behind the entries (run `write`), unless the README says the tables are written on merge.

Paragraphs of the README, when it has them, change what the check does:

- **Written on merge.** says the default branch writes the tables after every merge, so a pull
  request adds its entry and leaves the tables alone and two pull requests never conflict in them.
  Then a table behind the entries is only noted, not a failure.
- **Citations.** turns on a scan of every file git tracks in the repository (not in hook mode):
  a retired ID cited anywhere is an error, and so is a live ID written without its product prefix.
  The paragraph names, in backticks, the paths the scan leaves out (a file, or a directory ending
  in /).
- **Product prefixes.** names one-letter product prefixes in backticks (`A` api, `W` web, ...), for
  a repository whose products share one registry. Then every ID is `P-AREA-N` with P one of them:
  an un-prefixed ID is an error, in an area file, the file table or the retired table.
"""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

VERSION = "2026-11-05"

DEFAULT_DIR = Path("docs/gotchas")
NON_AREA = {"README.md", "open-questions.md"}
# An ID is AREA-N, or P-AREA-N where P is a product prefix the README names.
ID_RE = r"[A-Z][A-Z0-9]*(?:-[A-Z][A-Z0-9]*)?-\d+"
ID_FULL = re.compile(rf"^{ID_RE}$")
ANCHOR_ID = re.compile(r"^[a-z][a-z0-9]*(?:-[a-z][a-z0-9]*)?-\d+$")
PREFIX_RE = r"[A-Z][A-Z0-9]*(?:-[A-Z][A-Z0-9]*)?"
LINK = re.compile(r"\]\(([^)\s#]*)(?:#([^)\s]+))?\)")

ONES = ("zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen "
        "fifteen sixteen seventeen eighteen nineteen").split()
TENS = "_ _ twenty thirty forty fifty sixty seventy eighty ninety".split()


def number_word(n: int) -> str:
    if n < 20:
        return ONES[n]
    if n < 100:
        t, o = divmod(n, 10)
        return TENS[t] + ("" if o == 0 else "-" + ONES[o])
    return str(n)


def id_key(i: str) -> tuple[str, int]:
    p, n = i.rsplit("-", 1)
    return p, int(n)


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8").replace("\r\n", "\n")


def cells(line: str) -> list[str] | None:
    s = line.strip()
    if not s.startswith("|") or re.fullmatch(r"\|[\s:|-]*\|", s):
        return None
    return [c.strip() for c in re.split(r"(?<!\\)\|", s)[1:-1]]


def sections(text: str) -> dict[str, str]:
    """Split on '## ' headings. Key '' is everything before the first one."""
    out, key, buf = {}, "", []
    for line in text.split("\n"):
        m = re.match(r"^## (.+?)\s*$", line)
        if m:
            out[key] = "\n".join(buf)
            key, buf = m.group(1), []
        else:
            buf.append(line)
    out[key] = "\n".join(buf)
    return out


def find_dir(explicit: str | None) -> Path | None:
    if explicit:
        d = Path(explicit)
        return d if (d / "README.md").is_file() else None
    starts = [Path(os.environ["CLAUDE_PROJECT_DIR"])] if os.environ.get("CLAUDE_PROJECT_DIR") else []
    starts.append(Path.cwd())
    for start in starts:
        for base in [start, *start.resolve().parents]:
            if (base / DEFAULT_DIR / "README.md").is_file():
                return base / DEFAULT_DIR
    return None


# ---------------------------------------------------------------- parsing

class Area:
    def __init__(self, path: Path):
        self.path, self.name = path, path.name
        self.text = read(path)
        self.headings: list[str] = re.findall(r"^### (.+?)\s*$", self.text, re.M)
        head = self.text.split("\n### ", 1)[0]
        self.table: list[tuple[str, str, str]] = []  # (id, anchor, symptom)
        for line in head.split("\n"):
            c = cells(line)
            if c and len(c) >= 2:
                m = re.fullmatch(rf"\[({ID_RE})\]\(#([^)]+)\)", c[0])
                if m:
                    self.table.append((m.group(1), m.group(2), c[1]))
        self.bodies: dict[str, str] = {}
        parts = re.split(r"^### (.+?)\s*$", self.text, flags=re.M)
        for i in range(1, len(parts), 2):
            self.bodies[parts[i]] = parts[i + 1]


def symptom(body: str) -> str:
    """The bold sentence an entry opens with, on one line, its pipes escaped for a table cell."""
    m = re.match(r"\s*\*\*(.+?)\*\*", body, re.S)
    return " ".join(m.group(1).split()).replace("|", "\\|") if m else ""


class Readme:
    def __init__(self, path: Path):
        self.path = path
        self.text = read(path)
        sec = sections(self.text)
        self.head = sec.get("", "")
        self.has_index = "Index" in sec
        self.index_text = sec.get("Index", "")
        self.retired_text = sec.get("Retired IDs", "")
        self.opening = re.search(r"(\d+) traps?, grouped into ([\w-]+) files?", self.head)

        self.files: dict[str, tuple[list[str], str]] = {}  # name -> (prefixes, count cell)
        for line in self.head.split("\n"):
            c = cells(line)
            if c and len(c) == 3:
                m = re.fullmatch(r"\[([^\]]+\.md)\]\(([^)]+)\)", c[0])
                if m:
                    owned = c[1].split("—", 1)[0]
                    self.files[m.group(2)] = (re.findall(rf"`({PREFIX_RE})`", owned), c[2])

        m = re.search(r"\*\*Source key\.\*\*(.*?)(?:\n\s*\n|\Z)", self.head, re.S)
        self.source_keys = set(re.findall(r"`([A-Z][A-Z_]*)`", m.group(1))) if m else set()

        m = re.search(r"\*\*Product prefixes\.\*\*(.*?)(?:\n\s*\n|\Z)", self.head, re.S)
        self.products = set(re.findall(r"`([A-Z])`", m.group(1))) if m else set()
        m = re.search(r"\*\*Citations\.\*\*(.*?)(?:\n\s*\n|\Z)", self.head, re.S)
        self.scan = m is not None
        self.unscanned = re.findall(r"`([^`\s]+)`", m.group(1)) if m else []
        self.written_on_merge = re.search(r"\*\*Written on merge\.\*\*", self.head) is not None

        self.index: list[tuple[str, list[tuple[str, str, str, str]]]] = []  # (file, [(id, target, area, symptom)])
        for line in self.index_text.split("\n"):
            c = cells(line)
            if not c or len(c) < 3:
                continue
            g = re.fullmatch(r"\*\*\[([^\]]+\.md)\]\(([^)]+)\)\*\*", c[0])
            if g:
                self.index.append((g.group(2), []))
                continue
            e = re.fullmatch(rf"\[({ID_RE})\]\(([^)]+)\)", c[0])
            if e:
                if not self.index:
                    self.index.append(("", []))
                self.index[-1][1].append((e.group(1), e.group(2), c[1], c[2]))

        self.retired: dict[str, str] = {}
        for line in self.retired_text.split("\n"):
            c = cells(line)
            if c and len(c) >= 2 and ID_FULL.match(c[0]):
                self.retired[c[0]] = c[1]


def load(d: Path) -> tuple[Readme, list[Area]]:
    areas = sorted((Area(p) for p in d.glob("*.md") if p.name not in NON_AREA), key=lambda a: a.name)
    return Readme(d / "README.md"), areas


# ---------------------------------------------------------------- tables

def render(d: Path) -> dict[Path, str]:
    """Every file's text with its tables written from the entries; only the changed ones."""
    rd, areas = load(d)
    out: dict[Path, str] = {}

    for a in areas:
        ids = [h for h in a.headings if ID_FULL.match(h)]
        rows = ["| ID | Symptom |", "|---|---|"] + [f"| [{i}](#{i.lower()}) | {symptom(a.bodies.get(i, ''))} |" for i in ids]
        head, sep, rest = a.text.partition("\n### ")
        lines = head.split("\n")
        table = [n for n, line in enumerate(lines) if line.strip().startswith("|")]
        if table:
            lines[table[0]: table[-1] + 1] = rows
        else:
            while lines and not lines[-1].strip():
                lines.pop()
            lines += ["", *rows, ""]
        text = "\n".join(lines) + sep + rest
        if text != a.text:
            out[a.path] = text

    total = sum(len([h for h in a.headings if ID_FULL.match(h)]) for a in areas)
    text = rd.text
    text = re.sub(r"\d+ traps?, grouped into [\w-]+ files?",
                  f"{total} trap{'s' if total != 1 else ''}, grouped into {number_word(len(areas))} file{'s' if len(areas) != 1 else ''}",
                  text, count=1)
    by_name = {a.name: a for a in areas}
    lines = text.split("\n")
    for n, line in enumerate(lines):
        c = cells(line)
        if c and len(c) == 3:
            m = re.fullmatch(r"\[([^\]]+\.md)\]\(([^)]+)\)", c[0])
            if m and m.group(2) in by_name:
                count = len([h for h in by_name[m.group(2)].headings if ID_FULL.match(h)])
                lines[n] = f"| {c[0]} | {c[1]} | {count} |"
    index = ["| ID | Area | Symptom |", "|---|---|---|"]
    for a in areas:
        ids = [h for h in a.headings if ID_FULL.match(h)]
        if not ids:
            continue
        title = re.match(r"# (.+?)\s*$", a.text.split("\n", 1)[0])
        area = title.group(1) if title else a.name[:-3]
        index.append(f"| **[{a.name}]({a.name})** | | |")
        index += [f"| [{i}]({a.name}#{i.lower()}) | {area} | {symptom(a.bodies.get(i, ''))} |" for i in ids]
    try:
        start = next(n for n, line in enumerate(lines) if re.match(r"^## Index\s*$", line))
    except StopIteration:
        start = None
    if start is not None:
        end = next((n for n in range(start + 1, len(lines)) if lines[n].startswith("## ")), len(lines))
        lines[start + 1: end] = ["", *index, ""]
    text = "\n".join(lines)
    if text != rd.text:
        out[rd.path] = text
    return out


def write(d: Path) -> int:
    changed = render(d)
    for path, text in changed.items():
        newline = "\r\n" if "\r\n" in path.read_bytes().decode("utf-8", "replace") else "\n"
        path.write_text(text.replace("\n", newline), encoding="utf-8", newline="")
    print("wrote: " + (", ".join(p.name for p in changed) if changed else "nothing, the tables were current"))
    return 0


# ---------------------------------------------------------------- checks

def check(d: Path) -> list[str]:
    errs: list[str] = []
    rd, areas = load(d)
    by_name = {a.name: a for a in areas}
    live: dict[str, str] = {}
    # The citation and link checks read each file as `write` would leave it. Otherwise a row the
    # next `write` removes (say, of an entry just retired) fails the check, and `write`, which
    # refuses to run on a failing check, could never remove it. Staleness is reported on its own.
    rendered = render(d)
    readme_text = rendered.get(rd.path, rd.text)
    area_text = {a.name: rendered.get(a.path, a.text) for a in areas}

    # --- each area file on its own
    for a in areas:
        bad = [h for h in a.headings if not ID_FULL.match(h)]
        for h in bad:
            errs.append(f"{a.name}: heading '### {h}' is not an ID like ABC-12 (use #### for sub-headings)")
        ids = [h for h in a.headings if ID_FULL.match(h)]
        if ids != sorted(ids, key=id_key):
            errs.append(f"{a.name}: entries are not in ID order: {', '.join(ids)}")
        for i in ids:
            if i in live:
                errs.append(f"{i}: defined twice, in {live[i]} and {a.name}")
            live[i] = a.name
        for i in ids:
            body = a.bodies.get(i, "").strip()
            if not body.startswith("**"):
                errs.append(f"{a.name}: {i} does not open with a bold symptom sentence")
            src = re.findall(r"^\*Source:(.*)$", body, re.M)
            if not src:
                errs.append(f"{a.name}: {i} has no '*Source: ...*' line")
            elif rd.source_keys and not any(k in s for s in src for k in rd.source_keys):
                errs.append(f"{a.name}: {i} source names none of the README's keys "
                            f"({', '.join(sorted(rd.source_keys))})")

    # --- README: opening sentence (its numbers are written by `write`)
    if not rd.opening:
        errs.append("README.md: opening sentence 'N traps, grouped into <word> files' not found")

    # --- README: file table (hand-written but its counts: a row per area file, each prefix one owner)
    owner: dict[str, str] = {}
    for name, (prefixes, count) in rd.files.items():
        if not (d / name).is_file():
            errs.append(f"README.md: file table links to missing file {name}")
            continue
        if name in NON_AREA:
            continue
        for p in prefixes:
            if p in owner:
                errs.append(f"README.md: prefix {p} claimed by both {owner[p]} and {name}")
            owner[p] = name
    for a in areas:
        if a.name not in rd.files:
            errs.append(f"README.md: no row in the file table for {a.name}")
    for i, f in live.items():
        p = id_key(i)[0]
        if p not in owner:
            errs.append(f"{f}: prefix {p} ({i}) is not listed in README's file table")
        elif owner[p] != f:
            errs.append(f"{f}: {i} uses prefix {p}, which README's file table gives to {owner[p]}")

    # --- README: index and the area tables are written from the entries by `write`; whether a
    # stale one fails is decided in main(), since it depends on **Written on merge.**
    # Only the heading is checked: an empty index is a table `write` fills, and failing on it here
    # would stop `write` (which refuses to run on errors) from ever writing the first entry.
    if not rd.has_index:
        errs.append("README.md: no '## Index' heading; add one above '## Retired IDs' for `write` to fill")

    # --- retired IDs
    for r in rd.retired:
        if r in live:
            errs.append(f"{r}: listed as retired but still defined in {live[r]}")
    if rd.retired:
        pat = re.compile(r"(?<![A-Za-z0-9-])(" + "|".join(map(re.escape, rd.retired)) + r")(?!\d)")
        scan = {"README.md": readme_text.replace(rd.retired_text, "")}
        scan.update(area_text)
        oq = d / "open-questions.md"
        if oq.is_file():
            scan[oq.name] = read(oq)
        for name, text in scan.items():
            for m in sorted(set(pat.findall(text))):
                errs.append(f"{name}: cites retired {m} (now {rd.retired[m]})")

    # --- links inside the registry
    anchors = {a.name: {h.lower() for h in a.headings} for a in areas}
    files_to_scan = list(area_text.items()) + [("README.md", readme_text)]
    if (d / "open-questions.md").is_file():
        files_to_scan.append(("open-questions.md", read(d / "open-questions.md")))
    for name, text in files_to_scan:
        for target, anchor in LINK.findall(text):
            if target.startswith(("http:", "https:", "mailto:")) or "/" in target:
                continue
            tfile = target or name
            if target and not (d / target).is_file():
                errs.append(f"{name}: link to missing file {target}")
                continue
            if anchor and ANCHOR_ID.match(anchor) and tfile in anchors and anchor not in anchors[tfile]:
                errs.append(f"{name}: link to {tfile}#{anchor}, which has no such entry")

    # --- unfinished bootstrap
    for name, text in files_to_scan:
        if "<!-- FILL:" in text:
            errs.append(f"{name}: still has '<!-- FILL: ... -->' placeholders from the template")

    # --- product prefixes
    if rd.products:
        def prefixed(i: str) -> bool:
            parts = i.split("-")
            return len(parts) == 3 and parts[0] in rd.products
        for i, f in live.items():
            if not prefixed(i):
                errs.append(f"{f}: {i} has no product prefix ({', '.join(sorted(rd.products))}): "
                            f"write it P-{i}")
        for name, (prefixes, _) in rd.files.items():
            for p in prefixes:
                if not prefixed(p + "-1"):
                    errs.append(f"README.md: file table gives {name} the prefix {p}, which has no "
                                "product prefix")
        for r in rd.retired:
            if not prefixed(r):
                errs.append(f"README.md: retired {r} has no product prefix")

    # de-duplicate, keep order
    seen, out = set(), []
    for e in errs:
        if e not in seen:
            seen.add(e)
            out.append(e)
    return out


def scan_repo(d: Path) -> tuple[list[str], str]:
    """Citations across the repository: retired IDs, and live IDs without their product prefix.
    Returns the problems and a note to print beside OK (empty when there is nothing to say)."""
    rd, areas = load(d)
    if not rd.scan:
        return [], ""
    live = {h for a in areas for h in a.headings if ID_FULL.match(h)}
    try:
        root = Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=d, check=True,
                                   capture_output=True, text=True).stdout.strip())
        listed = subprocess.run(["git", "ls-files", "-z"], cwd=root, check=True,
                                capture_output=True, text=True).stdout.split("\0")
    except (OSError, subprocess.CalledProcessError):
        return [], "citation scan skipped: not a git repository"
    registry = d.resolve().relative_to(root).as_posix() + "/"
    skip = [registry, *rd.unscanned]
    bare: dict[str, str] = {}
    if rd.products:
        for i in live | set(rd.retired):
            parts = i.split("-")
            if len(parts) == 3:
                bare[f"{parts[1]}-{parts[2]}"] = i
    token = re.compile(rf"(?<![A-Za-z0-9-])({ID_RE})(?!\d)")
    errs: list[str] = []
    for rel in listed:
        if not rel or any(rel == s or (s.endswith("/") and rel.startswith(s)) for s in skip):
            continue
        path = root / rel
        try:
            if path.stat().st_size > 4_000_000:
                continue
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        if not any(c.isdigit() for c in text):
            continue
        for n, line in enumerate(text.split("\n"), 1):
            for t in token.findall(line):
                if t in rd.retired:
                    errs.append(f"{rel}:{n}: cites retired {t} (now {rd.retired[t]})")
                elif t in bare:
                    errs.append(f"{rel}:{n}: cites {t} without its product prefix: {bare[t]}")
    return errs, ""


def highest(d: Path) -> dict[str, int]:
    rd, areas = load(d)
    n: dict[str, int] = {}
    for i in [h for a in areas for h in a.headings if ID_FULL.match(h)] + list(rd.retired):
        p, k = id_key(i)
        n[p] = max(n.get(p, 0), k)
    return n


# ---------------------------------------------------------------- init

PYTHON = "python" if os.name == "nt" else "python3"
HOOK = {"hooks": {"PostToolUse": [{"matcher": "Edit|MultiEdit|Write",
                                   "hooks": [{"type": "command",
                                              "command": f'{PYTHON} "$CLAUDE_PROJECT_DIR/tools/check_gotchas.py" --hook'}]}]}}


def init(dir_arg: str | None) -> int:
    assets = Path(__file__).resolve().parent.parent / "assets"
    if not assets.is_dir():
        print("init needs the skill's own copy of this script (the one next to assets/).", file=sys.stderr)
        return 1
    d = Path(dir_arg) if dir_arg else DEFAULT_DIR
    d.mkdir(parents=True, exist_ok=True)
    made = []
    for src, dst in (("README.template.md", "README.md"), ("open-questions.template.md", "open-questions.md")):
        if not (d / dst).exists():
            shutil.copyfile(assets / src, d / dst)
            made.append(str(d / dst))
    tool = Path("tools/check_gotchas.py")
    if not tool.exists():
        tool.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(Path(__file__).resolve(), tool)
        made.append(str(tool))
    print("created: " + (", ".join(made) if made else "nothing (already present)"))
    print(f"next: fill every '<!-- FILL: ... -->' in {d / 'README.md'}, then run "
          f"`{PYTHON} tools/check_gotchas.py`.")
    print("hook (merge into .claude/settings.json if the user agrees):")
    print(json.dumps(HOOK, indent=2))
    return 0


# ---------------------------------------------------------------- main

def registry_of(path: str) -> Path | None:
    """The registry a file lies in: its nearest `gotchas/` directory that has a README.md."""
    if not path:
        return None
    p = Path(path)
    if not p.is_absolute() and os.environ.get("CLAUDE_PROJECT_DIR"):
        p = Path(os.environ["CLAUDE_PROJECT_DIR"]) / p
    for base in p.parents:
        if base.name == "gotchas" and (base / "README.md").is_file():
            return base
    return None


def main(argv: list[str]) -> int:
    if "--version" in argv:
        print(f"check_gotchas.py {VERSION} (gotcha skill, https://github.com/RedFirebreak/agent-forge)")
        return 0
    dir_arg = None
    if "--dir" in argv:
        i = argv.index("--dir")
        dir_arg = argv[i + 1]
        del argv[i:i + 2]
    elif argv and argv[0] not in ("init", "ids", "next", "write") and not argv[0].startswith("-"):
        dir_arg = argv.pop(0)

    if argv[:1] == ["init"]:
        return init(dir_arg)

    hook = "--hook" in argv
    if hook:
        try:
            event = json.load(sys.stdin)
        except Exception:
            event = {}
        path = str((event.get("tool_input") or {}).get("file_path", ""))
        if not path.endswith(".md"):
            return 0
        found = registry_of(path)
        if found is None:
            return 0  # not a registry's file; nothing to police
        dir_arg = str(found)

    d = find_dir(dir_arg)
    if d is None:
        if hook:
            return 0  # project has no registry; nothing to police
        print("no docs/gotchas/README.md found (run `init` from the skill, or pass --dir)", file=sys.stderr)
        return 1

    if argv[:1] == ["write"]:
        errs = check(d)
        if errs:
            print(f"gotchas: {len(errs)} problem(s) in {d}; fix them before the tables are written")
            for e in errs:
                print("  - " + e)
            return 1
        return write(d)
    if argv[:1] == ["ids"]:
        for p, n in sorted(highest(d).items()):
            print(f"{p}-{n}")
        return 0
    if argv[:1] == ["next"]:
        if len(argv) < 2 or not re.fullmatch(PREFIX_RE, argv[1]):
            print("usage: check_gotchas.py next PREFIX", file=sys.stderr)
            return 1
        print(f"{argv[1]}-{highest(d).get(argv[1], 0) + 1}")
        return 0

    errs = check(d)
    notes: list[str] = []
    if not hook:
        found, note = scan_repo(d)
        errs += found
        if note:
            notes.append(note)
        # A table behind its entries is half-finished work, unless the default branch writes the
        # tables after every merge (**Written on merge.**), in which case it is expected in a branch.
        # Not checked in hook mode: between adding an entry and running `write` it is always behind.
        stale = ", ".join(p.name for p in render(d))
        if stale and Readme(d / "README.md").written_on_merge:
            notes.append(f"tables behind the entries in {stale}; the default branch writes them")
        elif stale:
            errs.append(f"tables are behind the entries in {stale}: run `check_gotchas.py write`")
    if not errs:
        if not hook:
            print("OK" + (f" ({'; '.join(notes)})" if notes else ""))
        return 0
    stream = sys.stderr if hook else sys.stdout
    print(f"gotchas: {len(errs)} problem(s) in {d}", file=stream)
    for e in errs:
        print("  - " + e, file=stream)
    return 2 if hook else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
