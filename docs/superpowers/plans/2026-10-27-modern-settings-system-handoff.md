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
API, and a new non-modal settings tab UI. 23 tasks in 3 phases. **Tasks 1–8 are complete and
reviewed; Task 9 is implemented and green but NOT yet reviewed — review it first.**

## Current state

- **Branch:** `1345-modern-settings-system` (off `master`), pushed to `origin`
- **Test baseline:** `jipipe-core` suite = **644 run, 0 failures, 0 errors, 1 pre-existing skip** at the current head (`db24d2aee2`). (The skip is the property-gated `TaxonomyConversionReportTest`, unrelated.)
- **Java:** the box default is Java 25; the project needs Java 21. Point `JAVA_HOME` at Java 21 before running Maven.
- **Build gate:** `mvn test -pl jipipe-core -q` must stay green at every commit.

## Task status (do NOT re-implement tasks already committed)

| Task | Component | Commit(s) | Status |
|---|---|---|---|
| 1 | Scopes, kinds, block-path model | `5257729e96` | complete, review clean |
| 2 | In-memory `SettingsDocument` (pass-through) | `0869ef5f6f` | complete, review clean |
| 3 | `SettingsDocumentIO` serialization + isolation | `e52778cbd9`, `ff82e25e20` | complete (1 fix round) |
| 4 | `SettingsFileStore` atomic write + corrupt recovery | `3d67bd97bc`, `9f93eefa44` | complete, review clean |
| 5 | `JIPipeSettingKey` + `SettingsBinding` + document path listeners | `2c0ce9ddef`, `74b542d22d` | complete (1 fix round) |
| 6 | `SettingsSheetBridge` (loop-safe sheet ⇄ doc) | `a47a67bd12`, `cd7fb6f4b5` | complete, review clean |
| 7 | `JIPipeSettingsServiceComponent` (scoped docs, bind, save/reload, bridges) | `062e7a05a2`, `328d876333` | complete, review clean (618 tests) |
| 8 | `SettingsFileWatcher` (poll-floor + watch, self-write suppression, debounce) | `66917ca4b8`, `e3f635d501` | complete (1 fix round), review clean (624 tests) |
| 9 | `SettingsDocumentMerger` (per-block merge) + watcher→merger wiring + self-healing read-back | `4052a14359` | **IMPLEMENTED, REVIEW PENDING** (638 tests) |

New types all live in `org.hkijena.jipipe.api.settings` (main + test) and
`org.hkijena.jipipe.api.service.components` (the component). Read those classes
before you start the next step — do not guess their signatures.

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
6. **Task 9 merge needs a 4th input: the last-synced baseline.** The brief's own tests
   `keepsBlockChangedOnlyLocally` and `foreignWinsOnCollisionAndIsReported` have identical
   3-arg inputs modulo the foreign value yet require opposite outcomes — no pure function of
   `(local, foreign, locallyChanged)` can distinguish them. The spec §5 resolving input is the
   block's value **at the last sync point** ("the file still shows the old block → we are newer
   → keep"). So `SettingsDocumentMerger` has the brief's 3-arg `merge(local, foreign,
   locallyChangedBlocks)` **plus** a 4-arg overload `merge(local, foreign,
   locallyChangedBlocks, Map<String, JsonNode> lastSynced)`; the 3-arg form delegates with an
   empty baseline (conservative: locally-changed + present-foreign → conflict, foreign wins;
   present-only-locally → kept). The component tracks the baseline via `recordSyncPoint`
   (after load, after each save, after each merge) and passes it to the 4-arg form.
   **Do not simplify this away** — dropping the baseline re-creates an ambiguity that silently
   chooses one of two wrong answers.
7. **The watcher's self-write API is NOT widened; the merge is self-healing instead.**
   Task 8's reviewer found a false negative (owner saves S → foreign writes F → owner's
   own-fingerprint read sees F, records it as its own, watcher suppresses F → F silently lost).
   A proposed `setOwnWrite(fingerprint, content)` watcher API was flagged and **rejected by
   ruling**: the plan mandates the `setOwnFingerprint(FileFingerprint)` signature, and the
   watcher reading settings-file content would couple it to the document's byte representation.
   Instead, `JIPipeSettingsServiceComponent.verifyOwnWrite` (added in Task 9) reads the file
   back **after each save** and compares it to what this process actually serialized; a mismatch
   is fed through the same `handleForeignChange` merge path. Order matters: read the
   fingerprint first, then the content, and decide on the content — a foreign write landing
   between the two reads fails the content compare and is merged. Residual blind spot (a
   byte-identical foreign write) is documented and is a no-op for values.
8. **`SettingsFileWatcher` is package-private** (`org.hkijena.jipipe.api.settings`); the
   service component reaches it through the `SettingsFileWatching` public façade. If you need
   the watcher from `api.service.components`, use the façade.

## Carry-forward notes (things later tasks must honor)

- **DONE (Task 7):** service component tracks and closes its bindings; headless debounce uses
  `ScheduledExecutorService`; bridges re-attach on `reload()`.
- **DONE (Task 8/9):** watcher is poll-floor + WatchService, self-write-suppressed, debounced;
  the merge is wired with a cheap callback hand-off (single daemon
  `jipipe-settings-foreign-merge` thread) and a post-save `verifyOwnWrite`.
- **Task 10** (migration): the component's `document(scope)` lazily loads from disk today;
  migration must run BEFORE the first materialization (the first `document(APPLICATION)`/
  `document(RUNTIME)` call), or it will race the lazy load. The component exposes
  `fileFor(scope)` — use those paths.
- **Task 13** delegates: `putIntoRegistry`/`getFromRegistry`/`getListFromRegistry` keep their
  exact signatures and the `destructive` flag semantics (true = write default when absent;
  false = leave absent). Route them through the document, not through new bindings.
  **Task 13 is also where `JIPipe.getSettingsService()` gets added** and where the old
  `JIPipeApplicationSettingsServiceComponent` becomes a delegate. After Task 13, the component's
  `bind()` results created before a `reload()` are orphaned (the document is replaced) —
  one-shot registry calls must not retain a binding across a reload.
- **Task 14** adds the access-policy seam (`canWrite`, `setWriteGuard`, `SettingsAccessDeniedException`)
  onto the Task 7 component.
- **Sheet key caveat** (Task 6 Javadoc): nested (child-collection) sheet parameters are skipped in
  document→sheet sync, and unknown nested sub-keys inside a known group are replaced by
  `flushToDocument`. The example sheets are flat, so this is acceptable; if a real sheet uses a
  nested collection, decide whether per-segment handling is needed.
- **Deferred minor findings** (cosmetic, not blocking; fix opportunistically or at final review):
  - Task 4: `fingerprint()` returns null on any IOException (docs promise null only for absence)
    — the watcher reads null as "no known fingerprint" and treats the next change as foreign
    (conservative, one spurious reload max).
  - Task 4: `System.err` used instead of SLF4J in the new `api/settings` package (consistent
    within the package; invisible once wired into the app).
  - Task 5: `set()` Javadoc misstates the `set(null)` mechanism for null-default keys; dead null
    branch in `set()`.
  - Task 6: `applying` flag is not volatile (single-threaded EDT usage is safe; document if needed).
  - Task 7: double `attachSheet` for the same sheet leaks the first bridge (defensive detach);
    `forTesting` NPEs on `getContext()`; debounce coalescing has no deterministic unit test.
  - Task 8: `SettingsFileWatcher` constructor and `POLL_INTERVAL_MILLIS` are `public` on a
    package-private class (redundant modifiers).
  - Task 9 (unreviewed): the EDT local-edit vs background-merge race on the same block is
    mitigated (concurrent maps + conservative keep rule = no data loss, at worst a missing
    conflict banner) but not test-pinned; the Task 9 review should scrutinize this.

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
   Confirm the tip is `4052a14359` (`feat(settings): per-block merge for multi-instance sync`).
2. Read this file, then the plan header + Global Constraints + Review Focus.
3. **FIRST ACTION — finish Task 9, which is implemented but NOT reviewed.** Task 9's
   implementer reported DONE and the suite is green (638 tests), but no task review has run.
   It is the highest-risk task so far: it introduces the merge rules and the threading between
   the watcher callback, the save path and the EDT.
   - Generate the review package: `bash <skill>/scripts/review-package docs/superpowers/plans/2026-10-27-modern-settings-system.md e3f635d501 4052a14359`
     (or, without the script: `git log --oneline e3f635d501..4052a14359`, `git diff --stat
     e3f635d501..4052a14359`, `git diff -U10 e3f635d501..4052a14359 > <file>`).
   - Dispatch a fresh task reviewer with: the Task 9 brief, the implementer report
     (`task-9-report.md` — an **untracked** file; see "Where the working files live"), the
     diff file, and the global constraints.
   - **Scrutinize these specific things** (the implementer self-flagged some):
     - the **4-arg baseline overload** — is `recordSyncPoint` called at every place that can
       change the sync baseline (after load, after save, after merge), and can the baseline go
       stale such that a "keep" is chosen when the file actually shows a newer conflicting value?
     - the **self-healing read-back ordering** (fingerprint first, then content, decide on
       content) — is the mismatch path really routed through the merge, and can it loop?
     - **thread-safety**: the merge runs on `jipipe-settings-foreign-merge` while local edits run
       on the EDT; `locallyChangedBlocks` and the document were touched by both. Verify the
       claim "concurrent maps + conservative keep rule = no data loss".
     - the **double-delivery guard** (recording the handled foreign fingerprint so the same
       on-disk state is not merged twice) — the report claims a second delivery could adopt local
       blocks as foreign deletions (data loss). Verify that is actually prevented and tested.
   - Run the fix loop (resume the implementer, scoped re-review) if findings appear.
   - Then ledger `Task 9: complete (commits …)` and continue.
4. Next after Task 9: **Task 10** (5.x → 6.0 migration with scope routing), then Task 11
   (carry-over), 12 (project scope), 13 (delegates + `JIPipe.getSettingsService()`), 14–17
   (consumers + integration), 18–22 (UI), 23 (verification). Follow the plan.
5. For every remaining task: extract the brief (the `task-brief` script, or paste the task's
   section from the plan into a file), dispatch a fresh implementer with the brief + the
   carry-forward notes above + global constraints + a report-file path, then a fresh reviewer
   with brief + report + diff, then the fix loop if needed.
6. Do not pause between tasks. Rulings you make go in the ledger.
7. After Task 23, run the whole-branch final review (most capable model), then finish the branch.

### Where the working files live

- **Tracked (in git, you get them from a clone):** this file, the spec, the plan.
- **NOT tracked** (`.superpowers/` is git-ignored, so a fresh clone does NOT have them):
  the ledger, the per-task briefs, the implementer reports, and the review-package diffs —
  including `task-7-report.md`, `task-8-report.md` and `task-9-report.md`, which the review
  instructions above refer to. On a new machine, recreate the workspace and the briefs from the
  plan (`bash <skill>/scripts/sdd-workspace <plan>` and `task-brief <plan> <N>`), and note that
  **the Task 9 report will be missing** — if you need it, re-derive the evidence from the commit
  `4052a14359` and its diff, or ask the previous machine for the file. Everything the review
  needs is in the commit itself.

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

Review Task 9 (implemented, unreviewed — see "How to resume", step 3). Do not re-dispatch
Tasks 1–8. Do not touch `master`.
