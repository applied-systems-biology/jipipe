# Verification Record — Modern Settings System (#1345)

Branch: `1345-modern-settings-system` (57 commits over `master`), verified 2026-10-31 on
Java 21 (`/usr/lib/jvm/java-21-openjdk-amd64`).

This record maps each success criterion of the approved spec
(`docs/superpowers/specs/2026-10-27-modern-settings-system-design.md` §11) to the test(s) or
manual check that prove it, with the exact command and the observed result.

## Gates

| Gate | Command | Result |
| --- | --- | --- |
| Full build (all modules, all tests) | `mvn clean install -q` | **BUILD SUCCESS** (exit 0; the only test-relevant log noise is the pre-existing CLIJ2 fixture "Unable to find node type … Skipping" lines, unrelated to settings) |
| Settings-focused, core | `mvn test -pl jipipe-core -Dtest='*Settings*,Settings*' -q` | **PASS — 207 run, 0 failures, 0 errors, 0 skipped** (includes `PaletteStorageLifecycleTest`; the desktop model tests are additionally listed below per criterion) |
| Settings-focused, launcher | `mvn test -pl jipipe-launcher -Dtest='*Settings*' -q` | **PASS — 20 run, 0 failures** (`SettingsMigrationIntegrationTest` 3, `SettingsNoModalUiTest` 1, `SettingsBindingLiveUpdateTest` 2, `SettingsPaletteKeysTest` 2, `SettingsDelegateTest` 10, `JIPipeProjectSettingsPersistenceTest` 2) |
| No legacy file write path | `git grep -n "settings-db-" -- jipipe-core/src/main` ; `git grep -n "recent-projects.txt" -- jipipe-core/src/main` | **PASS** — only the migration code references the legacy names (see below) |

**Legacy file references (Step 4) — every hit is migration or documented read-only:**

- `settings-db-<key>.json` — `SettingsMigration` (`REGISTRY_FILE_PREFIX` :118, the migration
  reader :284, `registryFiles()` :626, `registryFileKey()` :645) and one Javadoc sentence in
  `JIPipeApplicationSettingsServiceComponent` (:250) explaining the delegate behavior.
  **No runtime code writes these files.**
- `recent-projects.txt` — `SettingsMigration` (:117, :297 — the one-time converter) and
  `JIPipeRecentProjectsRegistry.getPropertyFile()` (:147), whose Javadoc states "It is no
  longer written; it is read only to migrate a profile whose settings were not migrated by
  the startup profile migration." The registry's live read/write path is the
  `state:recent-projects` document block (`BLOCK_PATH` :61).

## Success criteria (spec §11)

### Criterion 1 — A 5.x profile opens with all settings present, old files moved to backup, nothing lost; a 6.x profile's unknown content survives a save cycle

- **Tests:**
  - `SettingsMigrationIntegrationTest.legacyProfileMigratesWithoutLoss` (launcher) — a
    committed 5.x-shaped fixture profile (flat `settings.json` + two `settings-db-*.json` +
    `recent-projects.txt`) is run through the production `SettingsMigration.migrateLegacyProfile`
    with the production `SettingsScopes.scopeByBlock()` routing map and the real
    `SettingsFileStore`; asserts every block lands in the new-shape documents (runtime blocks
    routed to `runtime-settings.json`), the old files are moved to
    `settings-migration-backup/`, and `needsMigration` is false afterwards.
  - `SettingsMigrationIntegrationTest.unknownBlocksSurviveASaveCycle` (launcher) — unknown
    kind prefix, unknown sheet block, and an unknown sub-path inside a known block all
    survive a load→save byte-for-byte (the core anti-regression property).
  - `SettingsMigrationTest` (core, 20 tests) — the migration statics in isolation, incl. the
    deterministic temp-dir migration of the same legacy shape.
  - `SettingsCarryOverTest` (core, 12) — unknown blocks are recovered from a sibling profile
    version on first open.
  - `SettingsFileStoreTest` (core, 8) — atomic write, orphan `.tmp` cleanup, `.bad-` recovery.
- **Command:** `mvn test -pl jipipe-launcher -Dtest=SettingsMigrationIntegrationTest -q`
- **Observed result:** PASS (3/3; part of the 20/20 launcher gate above).

### Criterion 2 — Two JIPipe processes: a change in one is reflected in the other within ~2 s; no corruption under simultaneous saves

- **Tests:**
  - `SettingsFileWatcherTest` (core, 6) — an external file change is detected by the watcher
    and merged into the live document with `ChangeOrigin.EXTERNAL_FILE`.
  - `JIPipeSettingsServiceComponentSyncTest` (core, 7) —
    `foreignFileChangeIsAdoptedWithExternalFileOrigin`,
    `interleavedForeignWriteIsNotSilentlyLost`, `sameBlockCollisionIsReportedAndForeignWins`,
    `saveDoesNotClobberAForeignWriteThatLandedFirst`,
    `localEditDuringMergeIsNotDiscardedByTheSecondPass`,
    `corruptForeignFileDoesNotAbortOrWipeTheLocalSave`,
    `mergeRunsOnTheExecutorThreadNotInTheWatcherCallback`.
  - `SettingsDocumentConcurrencyTest` (core, 1) — the document's lock discipline under
    concurrent mutation.
  - `SettingsAutoPersistTest` (core, 4) — the prerequisite that the other process only sees
    what is on disk: a local edit is persisted by the engine's debounced auto-save (250 ms),
    so the second process's watcher sees it within ~2 s; the origin gate excludes
    foreign-merge, quiescent and the save path's own writes (no loop, no starvation).
- **UI side (manual, per test policy — no Swing interaction tests):** the settings tab
  subscribes to `JIPipeSettingsServiceComponent.getChangedEventEmitter()` and shows the
  live-update banner + the changes strip for foreign-origin events
  (`JIPipeDesktopSettingsTab.onSettingsChanged`); open editors update live via their
  `SettingsBinding` subscriptions (criterion 3). Verify in the app: run two JIPipe instances
  on the same profile, toggle a setting in one, watch the other's settings tab banner and
  the affected editor within ~2 s.
- **Command:** `mvn test -pl jipipe-core -Dtest='SettingsFileWatcherTest,JIPipeSettingsServiceComponentSyncTest,SettingsDocumentConcurrencyTest,SettingsAutoPersistTest' -q`
- **Observed result:** PASS (18/18; part of the 207/207 core gate above).

### Criterion 3 — Toggle "show toolbar labels" in one open graph editor → every other open graph editor updates without reopening

- **Tests:**
  - `SettingsBindingLiveUpdateTest` (launcher, 2) — two live bindings on
    `SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS` against the real service: a write through one
    fires the other's subscriber with the new value (the test restores the developer profile
    value in a `finally`).
  - `SettingsBindingLifecycleTest` (core, 3) — the mechanism: a bound key registers exactly
    one document path listener; `close()` removes exactly its own listener; re-open after
    close re-registers. (This is what makes the subscription the editors hold safe to
    create/destroy per editor.)
- **UI side (manual):** `JIPipeDesktopGraphEditorUI.initializeShowDockToolbarLabels` binds
  the key per editor, re-applies on change, and writes the panel's own toggle through the
  binding. Verify in the app: open two graph editors, toggle the toolbar-labels switch in
  one, the other updates immediately.
- **Command:** `mvn test -pl jipipe-launcher -Dtest=SettingsBindingLiveUpdateTest -q`
- **Observed result:** PASS (2/2; part of the 20/20 launcher gate above).

### Criterion 4 — All 35 sheets and the 4 Customize-JIPipe controls are reachable in the new tab; search finds any setting by name or key path; the raw editor lists and edits every stored key including pass-through ones

- **Tests:**
  - `SettingsTreeModelTest` (core, 3) — search matches name, description, AND block path
    (the key-path search: `search("GENERAL-UI")` resolves via `org.hkijena.jipipe:general-ui`),
    case-insensitively, across the cross-scope-visible sections; `isEditable` is the scope
    rule.
  - `AppearanceSectionModelTest` (core, 11) — the merged Customize-JIPipe content: theme,
    theme style, file-chooser type + fallback, reduced motion are composed entries; the
    per-key default-aware reset removes exactly the displayed keys (non-displayed keys stay
    intact — the clobber regression pin).
  - `RawSettingsTableModelTest` (core, 24) — documented keys AND pass-through blocks are both
    listed (`rows("")`), filtered by path substring, editable (`setValue` with JSON
    validation; JSON `null` is a storable value), resettable (`resetKey` with the
    scope-checked documented-vs-pass-through semantics — a key registered in another scope
    does NOT document this scope's location); previews are bounded but `rawJson` is not.
  - `SettingsDelegateTest` (launcher, 10) — includes
    `everyRegisteredSheetHasADocumentBlock`: every registered sheet (the registry the tab
    enumerates from `JIPipe.getSettings().getRegisteredSheets()`) has a document block, i.e.
    is reachable through the engine; plus the exact `destructive`-flag semantics of the
    migrated registry API.
  - `JIPipeProjectSettingsDocumentTest` (core, 6) — the project-scope document shape
    (`$meta` = `written-by-version` only) for the tab's PROJECT scope.
- **Manual (per test policy):** open the settings tab, confirm all categories render (the
  tree is built from the live registry — the 35 sheets plus the merged/placeholder
  sections), search "GENERAL-UI" (block path) and "theme" (name/description), open the raw
  section and confirm pass-through blocks are listed and editable.
- **Command:** `mvn test -pl jipipe-core -Dtest='SettingsTreeModelTest,AppearanceSectionModelTest,RawSettingsTableModelTest,SettingsChangeLogTest' -q`
- **Observed result:** PASS (44/44 for those four classes; part of the 207/207 core gate).

### Criterion 5 — No feature depends on save-on-shutdown for correctness; killing the process mid-session loses ≤ the last debounced window of edits

- **Test:** `SettingsAutoPersistTest` (core, 4; RED-first pin of the Task 18 engine fix) —
  a local edit through the real write path (binding/bridge) is scheduled by the engine and
  reaches the file after the debounce; quiescent and foreign-origin writes do NOT trigger
  the auto-save. Before the engine fix this test failed (the edit was never persisted) —
  the old modal's Save button had masked exactly this gap.
- **Manual:** edit a setting, kill the process within the 250 ms debounce window, re-open:
  only that last edit is lost. Edit again, wait ~1 s, kill: nothing is lost.
- **Command:** `mvn test -pl jipipe-core -Dtest=SettingsAutoPersistTest -q`
- **Observed result:** PASS (4/4; part of the 207/207 core gate).

### Criterion 6 — All three scopes exist, are loaded and saved by the same engine; every current setting is assigned to exactly one of them; a runtime document can be created, written, observed and access-checked using only the public API

- **Tests:**
  - `JIPipeSettingsServiceComponentTest` (core, 10) — `documentsAreScopeSeparated`,
    `availableScopesAlwaysContainAllThree`, `fileForMapsScopesToProfileFiles` (three
    scope-separated documents on the one engine; `settings.json` /
    `runtime-settings.json` file mapping; PROJECT has no file), and
    `changeEventsAreEmittedWithOldAndNewValues` (a key created/written/observed via the
    public API `bind`/`set`/`subscribe` fires the change event).
  - `SettingsScopesTest` (core, 6) — `runtimeExecutionBlocksAreRuntimeScoped` +
    `uiBlocksAreNotRuntimeScoped` (the assignment: exactly the 6 runtime sheets; everything
    else APPLICATION — presence-based, per the plugin-aware test policy),
    `migrationRoutesRuntimeBlocksViaScopeMap` (the assignment exercised by a real
    migration of a temp legacy profile), `runtimeScopedKeyContributesItsBlockToTheMergedView`
    (the keys-first layer), and `writeGuardBlocksRejectedKey` +
    `allKeysAreWritableWhenNoGuardIsInstalled` (the access check, public API,
    `SettingsAccessDeniedException` with the document left untouched).
- **Command:** `mvn test -pl jipipe-core -Dtest='JIPipeSettingsServiceComponentTest,SettingsScopesTest' -q`
- **Observed result:** PASS (16/16; part of the 207/207 core gate).

### Criterion 7 — The runtime document can be exported and re-imported into a fresh service instance without loss (the prerequisite for the #1102 process split)

- **Test:** `JIPipeSettingsServiceComponentTest.runtimeDocumentTransfersIntoAFreshInstanceWithoutLoss`
  (core; **added by this verification task** — it was the one §11 criterion without a
  dedicated pin when the verification sweep was started). A RUNTIME-scoped key is bound, set
  and saved by one `forTesting` instance; a FRESH instance reading the same directory
  re-imports the runtime document and reads back the transferred value; the runtime document
  lives in its own `runtime-settings.json` file (the transferable artifact — the split
  moves where that file lives, not how settings are defined).
- **Command:** `mvn test -pl jipipe-core -Dtest='JIPipeSettingsServiceComponentTest#runtimeDocumentTransfersIntoAFreshInstanceWithoutLoss' -q`
- **Observed result:** PASS (1/1; part of the 10/10 class run).

## Notes

- The Swing surfaces (tab tree, merged sections with live theme preview, the changes strip,
  the raw editor dialogs, the banner) are verified by the user in the running app, per the
  project test policy (no Swing structure/interaction tests). The behavioral contracts
  behind them (auto-persist, binding live updates, reset semantics, row enumeration, change
  log) are the automated pins above.
- The settings-tab deep links use stable registered sheet IDs
  (`openSettingsSection(SettingsScope, String)`), not display names — a sheet renamed or
  re-categorized does not rot them; a removed sheet falls back to the tab's default
  selection and is logged (`openSettingsSection` captures `selectSheet(scope, id)`'s boolean
  and warns `Settings deep link to unknown sheet id '<id>' (scope <scope>)`).
- Follow-up (recorded, out of scope for #1345): consolidate the project-settings modal's
  three legacy wrapper sheets (metadata/parameters/directories) into the tab's PROJECT
  scope, then delete `JIPipeDesktopProjectSettingsUI` — the last remaining JDialog-based
  settings UI.
