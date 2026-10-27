# HANDOFF — Modernized Application Settings System (#1345)

**Read this first if you are resuming the work on another machine.** This file is the
durable, tracked record of progress. The plan and spec are also tracked and are the
authorities this work argues from:

- **Spec (binding authority):** `docs/superpowers/specs/2026-10-27-modern-settings-system-design.md`
- **Plan (the argument):** `docs/superpowers/plans/2026-10-27-modern-settings-system.md`

Work is executed **subagent-driven** per the `superpowers:subagent-driven-development`
skill. If that skill is available to you, follow it. If not, the manual below is self-sufficient.

## What this build is

Replacing JIPipe's three settings systems (parameter sheets in `settings.json`, registry
files `settings-db-*.json`, `recent-projects.txt`) with one scoped, version-stable,
multi-instance-safe settings document (`application` / `project` / `runtime`), a key + binding
API, and a new non-modal settings tab UI. 23 tasks in 3 phases. **The first 6 tasks (the
storage core) are complete and reviewed.**

## Current state

- **Branch:** `1345-modern-settings-system` (off `master`)
- **Test baseline:** `jipipe-core` suite = **609 run, 0 failures, 0 errors, 1 pre-existing skip** at the current head. (The skip is the property-gated `TaxonomyConversionReportTest`, unrelated.)
- **Java:** the box default is Java 25; the project needs Java 21. Point `JAVA_HOME` at Java 21 before running Maven (an implementer already did this in Task 4).
- **Build gate:** `mvn test -pl jipipe-core -q` must stay green at every commit.

## Completed tasks (do NOT re-dispatch)

| Task | Component | Commit(s) | Status |
|---|---|---|---|
| 1 | Scopes, kinds, block-path model | `5257729e96` | complete, review clean |
| 2 | In-memory `SettingsDocument` (pass-through) | `0869ef5f6f` | complete, review clean |
| 3 | `SettingsDocumentIO` serialization + isolation | `e52778cbd9`, `ff82e25e20` | complete (1 fix round) |
| 4 | `SettingsFileStore` atomic write + corrupt recovery | `3d67bd97bc`, `9f93eefa44` | complete, review clean |
| 5 | `JIPipeSettingKey` + `SettingsBinding` + document path listeners | `2c0ce9ddef`, `74b542d22d` | complete (1 fix round) |
| 6 | `SettingsSheetBridge` (loop-safe sheet ⇄ doc) | `a47a67bd12`, `cd7fb6f4b5` | complete (1 fix round) |

New types all live in `org.hkijena.jipipe.api.settings` (main + test). Read those classes
before you start Task 7 — do not guess their signatures.

## Rulings made this session (binding — re-read before you "improve" them)

These were decided by the controller with the spec as authority. Do not re-litigate them;
if you disagree, that is itself a finding to surface, not a silent change.

1. **`fromJson` must deep-copy its input.** Task 3's first pass stored raw Jackson node
   references (aliased), silently mutating the caller's tree. Fixed; an isolation test pins it.
2. **Profile carry-over already exists.** The spec initially (and I) wrongly claimed there was
   no migration code. `JIPipeServiceInitializer.applyProfileUpgrades()` (JIPipeServiceInitializer.java:74)
   copies the newest *older* profile dir into an empty current one on first launch. Keep it.
   The new system ADDS targeted settings-level migration (Task 10) + a reverse/recovery merge
   (Task 11) for the shared-version-string and downgrade cases the existing copy cannot handle.
3. **Three live scopes, not "global + future server."** The user redirected: scopes are
   `application` / `project` / `runtime`, all live now. `runtime` has its own file
   (`runtime-settings.json`). The #1102 process split later moves *where* the runtime document
   lives, never its shape. Permission gating is a per-key `access` policy
   (`USER_EDITABLE`/`RUNTIME_ADMIN`/`READ_ONLY`).
4. **The sync model is Approach A:** watched file + atomic replace (temp+rename) + per-block
   merge with foreign-newer-wins. No D-Bus, no IPC, no single-writer election. Fingerprint
   (size + last-modified) is how a process ignores its own writes.
5. **Binding lifetime is owned by the service (Task 7).** `SettingsBinding` has `close()`
   (Task 5). Any binding a feature creates must be closed, else it leaks a listener + subscribers
   on the long-lived document. **Task 13's registry delegates must NOT create a fresh binding
   per `getFromRegistry`/`putIntoRegistry` call** — that would leak; they should use document-level
   `get`/`set`, not a binding, for the one-shot compatibility path.

## Carry-forward notes (things later tasks must honor)

- **Task 7** (`JIPipeSettingsServiceComponent`) must: (a) call `SettingsBinding.close()` on
  bindings it hands out and tracks; (b) use `ScheduledExecutorService` (not `javax.swing.Timer`)
  for the debounce when headless; (c) re-attach sheet bridges after a document reload (the bridge
  holds a document reference); (d) `getById`/`getByType`/`getRegisteredSheets` must keep returning
  the registered sheets (used at `JIPipe.java:484` for the runtime temp dir).
- **Task 13** delegates: `putIntoRegistry`/`getFromRegistry`/`getListFromRegistry` keep their
  exact signatures and the `destructive` flag semantics (true = write default when absent;
  false = leave absent). Route them through the document, not through new bindings.
- **Task 14** adds the access-policy seam (`canWrite`, `setWriteGuard`, `SettingsAccessDeniedException`)
  onto the Task 7 component.
- **Sheet key caveat** (Task 6 Javadoc): nested (child-collection) sheet parameters are skipped in
  document→sheet sync, and unknown nested sub-keys inside a known group are replaced by
  `flushToDocument`. The example sheets are flat, so this is acceptable; if a real sheet uses a
  nested collection, decide whether Task 7 needs per-segment handling.
- **Deferred minor findings** (cosmetic, not blocking; fix opportunistically or at final review):
  - Task 4: `fingerprint()` returns null on any IOException (docs promise null only for absence)
    — Task 8's watcher reads null as "no known fingerprint" and treats the next change as foreign
    (conservative, one spurious reload max).
  - Task 4: `System.err` used instead of SLF4J in the new `api/settings` package.
  - Task 5: `set()` Javadoc misstates the `set(null)` mechanism for null-default keys; dead null
    branch in `set()`.
  - Task 6: `applying` flag is not volatile (single-threaded EDT usage is safe; document if needed).

## Review Focus (the five failure modes no single task's tests fully cover)

These are in the plan's "Review Focus" section. When you reach Task 17 (golden-profile
integration) and Task 23 (whole-branch verification), these are the acceptance bar:

1. Unknown `sheet:`/`registry:`/`state:` blocks and unknown sub-paths survive a load→save cycle
   **verbatim** (the core anti-regression property).
2. A corrupt/truncated settings file → backed up as `.bad-<timestamp>`, defaults load, process
   starts, next save is valid — never a startup failure or backup loop.
3. Self-ignoring the watcher (own writes don't fire as foreign changes).
4. Two instances editing different blocks → neither lost (per-block merge).
5. The parameter-sheet bridge does not loop.

## How to resume (the exact procedure)

1. `git checkout 1345-modern-settings-system && git pull` (or fetch + reset to the pushed tip).
2. Read this file, then the plan header + Global Constraints + Review Focus.
3. The next task is **Task 7** (`JIPipeSettingsServiceComponent`). It is the first integration
   task — it wires Tasks 1–6 together and adds the service component to `JIPipeService`.
4. Dispatch a fresh implementer subagent for Task 7 using the `implementer-prompt.md` template
   from the `subagent-driven-development` skill (or the manual dispatch below). Give it:
   - the task brief (extract Task 7's full text from the plan into a file and pass the path),
   - the carry-forward notes above (they are the decisions the brief cannot know),
   - the report-file path,
   - the global constraints verbatim.
5. On DONE: generate a review package over `BASE..HEAD` (BASE = the commit before you
   dispatched), dispatch a fresh task reviewer, and run the fix loop (up to 5 rounds) if needed.
6. Repeat for Tasks 8–23. Do not pause between tasks.
7. After Task 23, run the whole-branch final review (most capable model), then finish the branch.

### Manual dispatch shape (if the skill's scripts are unavailable)

- **Implementer prompt:** "You are implementing Task N: <name>. Read your task brief first:
  <brief-path>. <2–3 lines of context + the exact interface from earlier tasks>. Ask questions
  before starting. TDD: failing test → green. Commit. Self-review. Report to <report-path>.
  Do not dispatch subagents. Do not spawn a reviewer." Model: the session default is fine for
  integration tasks.
- **Reviewer prompt:** "Review Task N for spec compliance and code quality. Read the brief
  <path>, the implementer report <path>, and the diff <path>. Return: Spec ✅/❌, Strengths,
  Issues (Critical/Important/Minor), Task quality Approved/Needs fixes. Read-only; do not
  re-run the suite; do not dispatch subagents."
- **Fix loop:** resume the implementer with the open findings verbatim; re-review scoped to the
  fix diff.

### Ledger convention

Keep a running ledger at `docs/superpowers/plans/2026-10-27-modern-settings-system-progress.md`
(or re-create the `.superpowers/sdd/` workspace if the skill is available — that dir is
git-ignored). One line per task: `Task N: complete (commits <base7>..<head7>, review clean)` or
`Task N: fix round R/5 (...)`. Append a line for every ruling you make:
`Ruling: <what you decided> — <why> — <what it costs if wrong>`. **Trust the ledger and `git
log` over any conversation memory — this file is the recovery map.**

## Immediate next action

Dispatch Task 7. Do not re-dispatch Tasks 1–6. Do not touch `master`.
