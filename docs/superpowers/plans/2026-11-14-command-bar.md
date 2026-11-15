# Command Bar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a VS-Code-style command bar to JIPipe windows that fires registry shortcut commands, opens in a chord-aid mode on chord prefixes, offers per-row Configure-Keybinding, switches windows/tabs, and revises the default keymap (specialty operations onto the Ctrl+T chord layer, `Ctrl+P`/`Ctrl+Shift+P` trigger).

**Architecture:** Pure command-bar model in `org.hkijena.jipipe.desktop.app.commandbar` (entries, availability via the dispatcher's scope chain, list modes, chord filter, search, MRU); a `JIPipeDesktopCommandBar` overlay per window on the root pane layered pane that registers a trigger `ShortcutScope` (dock-panel pattern) and takes over keys via the dispatcher's existing capture-suspend; the dispatcher hands chord prefixes to the focused window's bar through a static hook (status-sink pattern).

**Tech Stack:** Java 21, Swing (layered pane, JList, InputMap), JUnit 5, commons-text `StringMetrics.jaroWinkler()` (already a jipipe-core dependency).

**Spec:** `docs/superpowers/specs/2026-11-14-command-bar-design.md` (commit `2a936d5a46`)

## Global Constraints

- Branch: `1346-keyboard-shortcut-overhaul` (already checked out; do not create or switch branches).
- Java 21, Maven multi-module; tests with `mvn -pl jipipe-core test -Dtest=<TestClass>`; full gate is `mvn -pl jipipe-core test` (baseline 1119 tests, 0 failures, 2 known pre-existing skips, known scijava INFO line - nothing else may fail).
- No em dashes (U+2014) or en dashes (U+2013) anywhere in code, Javadoc, comments, or strings; use `-`.
- Registration IDs are stable string literals, never code-derived (AGENTS.md rule).
- No Swing interaction tests (user verifies UI in-app). Pure logic tests only; use the existing patterns in `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/` and `.../desktop/app/shortcuts/`.
- Never hardcode text-dependent pixel sizes; derive from preferred sizes / window fractions; fixed gaps are fine.
- Every task ends with a green build and a commit.
- Tooling (user instruction): for symbol-level navigation use the native JetBrains MCP (`tools.jetbrains.*`), not `serena_jet_brains_*`; all paths relative to the repository root.

## Review Focus

- **Chord prefix opens the bar, not the hint, in every window kind and from text fields** (spec 6.2/6.3): the chord-filter logic is test-pinned in Task 2; the handoff wiring in Task 5 is user-verified in-app.
- **Bar close must always restore dispatch** (`setCaptureActive(false)` on every close path: Escape, focus loss, fire, configure) or all shortcuts die until restart: Task 4 funnels every close through `close()`; user-verified in-app.
- **Grayed rows must not fire** via Enter or click in the default list mode: covered by the Task 2 model test (unavailable entries carry no command) and user-verified.
- **Hand-edited settings documents must not break the bar**: unknown list-mode value falls back to `grayed`, out-of-domain MRU ids are skipped: pinned in Tasks 6 and 8.
- **LEGACY interplay**: under the LEGACY preset `Ctrl+P`/`Ctrl+Shift+P` hit pass-through inside graph editors (innermost scope, existing pinned behavior) and open the bar elsewhere: user-verified; the LEGACY growth is test-pinned in Task 1.

---

### Task 1: Keymap revision and new catalog entries

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutActions.java` (constants ~253-544, `CORE_ACTIONS` ~521-544, legacy map ~594-618)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/running/UpdateCacheShowIntermediateNodeUIContextAction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/running/UpdateCacheOnlyPredecessorsNodeUIContextAction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/add/AddTunnelEntranceUIContextAction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/add/AddTunnelExitUIContextAction.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutResolveTest.java` (extend)

**Interfaces (produces, used by Tasks 4-6):**
- New constants (names, IDs, categories, families, defaults exactly):
  - `COMMAND_BAR_OPEN` = `org.hkijena.jipipe.command-bar.open`, "Open command bar", category "Menus", family `FAMILY_PROJECT`, `textInputSafe(true)`, default variants `Ctrl+P` and `Ctrl+Shift+P` (`KeyStroke.getKeyStroke("control pressed P")`, `KeyStroke.getKeyStroke("control shift pressed P")`).
  - `NODE_CACHE_INTERMEDIATE_RESULTS` = `org.hkijena.jipipe.graph.node.cache-intermediate-results`, "Cache intermediate results", "Running", default `Ctrl+T` then `Shift+U` (`"control pressed T"`, `"shift pressed U"`).
  - `NODE_UPDATE_PREDECESSOR_CACHE` = `org.hkijena.jipipe.graph.node.update-predecessor-cache`, "Update predecessor cache", "Running", default `Ctrl+T` then `B`.
  - `NODE_ADD_TUNNEL_ENTRANCE` = `org.hkijena.jipipe.graph.node.add-tunnel-entrance`, "Add tunnel entrance here ...", "Creation", default `Ctrl+T` then `G`.
  - `NODE_ADD_TUNNEL_EXIT` = `org.hkijena.jipipe.graph.node.add-tunnel-exit`, "Add tunnel exit here ...", "Creation", default `Ctrl+T` then `X`.
- Changed default variants (same constants, new `defaultVariants`):
  - `PASS_THROUGH_ENABLE`: `Ctrl+T` then `P`; `PASS_THROUGH_DISABLE`: `Ctrl+T` then `Shift+P`
  - `NODE_LOCK`: `Ctrl+T` then `L`; `NODE_UNLOCK`: `Ctrl+T` then `Shift+L`
  - `NODE_ENABLE`: `Ctrl+T` then `Shift+D`; `NODE_DISABLE`: `Ctrl+T` then `D`
  - `NODE_ADD_PARAMETER_SET`: `Ctrl+T` then `S`; `NODE_SELECT_AND_MOVE_HERE`: `Ctrl+T` then `H`
  - `NODE_UPDATE_CACHE`: `Ctrl+T` then `U`
  - `NODE_ADD`: two variants, `Shift+A` (`"shift pressed A"`) and `Ctrl+T` then `N`
- LEGACY map additions (pre-6.0 restorations): `NODE_ENABLE` -> `Ctrl+E`, `NODE_DISABLE` -> `Ctrl+Shift+E`, `NODE_ADD_PARAMETER_SET` -> `Ctrl+Shift+A`, `NODE_SELECT_AND_MOVE_HERE` -> `Ctrl+G`, `NODE_UPDATE_CACHE` -> `Ctrl+Space` (`"control pressed SPACE"`). Existing LEGACY entries unchanged.
- All five action classes above get `public static final String SHORTCUT_ACTION_ID = <matching constant>.id();` + `getShortcutActionId()` override (same pattern as the Task 6 migration of the shortcut overhaul; `NodeUIContextAction` extends `GraphInteractiveObjectUIContextAction`, so the default method exists).
- New constants are added to `CORE_ACTIONS` so `registerCore()` registers them.

- [ ] **Step 1: Write the failing tests** (extend `JIPipeShortcutResolveTest`)

```java
@Test void commandBarActionDefaults() {
    // COMMAND_BAR_OPEN: 2 variants (ctrl P, ctrl shift P), family project, textInputSafe true, category "Menus"
}
@Test void revisedChordDefaults() {
    // with a fresh KeymapSettings: pass-through enable/disable, lock/unlock, enable/disable,
    // add-parameter-set, move-here, update-cache each resolve to exactly the spec chord;
    // NODE_ADD resolves to [shift A] and [ctrl T, N] (order: single stroke first)
}
@Test void freedDirectKeysAreUnboundByDefault() {
    // for strokes ctrl G, ctrl E, ctrl shift E, ctrl L, ctrl shift L, ctrl alt P,
    // ctrl SPACE, ctrl shift A: no registered action's DEFAULT binding contains them
}
@Test void newContextActionCatalogEntries() {
    // the four new constants resolve with their chords; registerCore is idempotent (second call no-throw)
}
@Test void legacyPresetRestoresMovedActions() {
    // apply(LEGACY): enable->ctrl E, disable->ctrl shift E, parameter set->ctrl shift A,
    // move-here->ctrl G, update-cache->ctrl SPACE; lock/unlock/pass-through entries unchanged
}
```

- [ ] **Step 2: Run to verify failure** `mvn -pl jipipe-core test -Dtest=JIPipeShortcutResolveTest` (compile error: constants missing)
- [ ] **Step 3: Implement** the catalog changes exactly as specified.
- [ ] **Step 4: Run to verify pass**, then the full `mvn -pl jipipe-core test` (other tests may pin old defaults? Only `JIPipeShortcutResolveTest` and the LEGACY test pin values - fix those expectations here if any other test breaks, and note it in the report).
- [ ] **Step 5: Commit** `feat(#1346): command bar trigger and chord-layer keymap revision`

---

### Task 2: Command bar model, availability, list modes, chord filter

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBarModel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/shortcuts/JIPipeDesktopShortcutDispatcher.java` (add two public helpers)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBarModelTest.java`

**Interfaces (produces, consumed by Tasks 3/4/7/8):**
- `JIPipeDesktopCommandBarModel` (static utility, no instances):
  - `enum ListMode { GRAYED, AVAILABLE_ONLY, EVERYTHING }` with `static ListMode parse(String)` (unknown/null -> `GRAYED`) and `String id()` (`"grayed"`, `"available-only"`, `"everything"`).
  - `interface Entry`: `String name()`, `String category()`, `Icon icon()` (nullable), `String bindingText()` (nullable, ephemeral), `List<List<KeyStroke>> variants()` (empty for ephemeral), `boolean isAvailable()`, `boolean isConfigurable()` (true for registry commands), `String actionId()` (null for ephemeral), `Runnable command()` (non-null only when available), `boolean isCategoryHeader()`; static factories `Entry categoryHeader(String category)` and `Entry ephemeral(String name, String category, Icon icon, Runnable command)`.
  - `static List<Entry> collect(List<JIPipeShortcutAction> actions, KeymapSettings settings, ListMode mode, Function<String, Runnable> resolver, List<Entry> ephemeral)`: builds category headers in registry order (`registry.all()` is already category-then-name; do not re-sort), appends ephemeral entries (their categories, e.g. "Windows"/"Tabs", sort after all registry categories), applies the mode: `GRAYED` keeps unavailable entries with `command()` null; `AVAILABLE_ONLY` drops them; `EVERYTHING` ignores availability differences only in that unavailable entries are still shown grayed and the caller is expected to pass all actions (the window-kind filter is the caller's scope list - document this in the Javadoc).
  - `static List<Entry> chordEntries(List<Entry> registryEntries, KeyStroke prefix)`: registry commands having a length-2 variant whose first stroke equals `prefix`; order preserved; available ones first, then unavailable (stable).
- Dispatcher additions (used by the bar to resolve/evaluate like `fireResolvedAction` does):
  - `public List<ShortcutScope> scopesContaining(Component focusOwner)` (wraps `orderedContainingScopes(scopes, focusOwner)`).
  - `public Runnable resolveAction(String actionId, Component focusOwner)`: innermost-first walk, text guard per the action (`JIPipeShortcutTextGuard` + `JIPipeShortcutRegistry.getInstance().byId(actionId)`), returns the first resolvable runnable or null. Refactor `fireResolvedAction` to delegate its per-id work to this method so there is one walk (behavior must not change; the existing dispatcher tests must stay green).

- [ ] **Step 1: Write the failing tests** (stub resolver via a `Map<String, Runnable>`, stub actions built with `JIPipeShortcutAction.builder`, no dispatcher install)

```java
@Test void collectOrdersCategoriesAndAppendsEphemeral() { /* headers in registry order; ephemeral categories last */ }
@Test void grayedModeKeepsUnavailableWithoutCommand() { /* unavailable entry: isAvailable false, command() null, still listed */ }
@Test void availableOnlyDropsUnavailable() { /* ... */ }
@Test void chordEntriesFilterByPrefixAndAvailability() {
    // two actions sharing prefix Ctrl+T, one available one not -> available first; action with only
    // a single-stroke variant is excluded; entries for a different prefix excluded
}
@Test void parseToleratesUnknownAndNull() { /* parse(null)==GRAYED, parse("nonsense")==GRAYED, round-trip id() */ }
```

- [ ] **Step 2: Run to verify failure** `mvn -pl jipipe-core test -Dtest=JIPipeDesktopCommandBarModelTest`
- [ ] **Step 3: Implement** the model and the two dispatcher helpers.
- [ ] **Step 4: Run** the new test class plus `mvn -pl jipipe-core test -Dtest='ShortcutScopeOrderTest,JIPipeDesktopShortcutDispatcherMapTest'` (dispatcher behavior unchanged), then the full suite.
- [ ] **Step 5: Commit** `feat(#1346): command bar model, availability, chord filter`

---

### Task 3: Window and tab switching provider

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBarSwitching.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/api/dataviewer/JIPipeDesktopDataViewerWindow.java` (static open-window set)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBarSwitchingTest.java`

**Interfaces (produces):**
- `JIPipeDesktopCommandBarSwitching` (static utility):
  - `public static List<JIPipeDesktopCommandBarModel.Entry> windowEntries(Window focused)`: one entry per open JIPipe window except `focused`, built with `JIPipeDesktopCommandBarModel.ephemeral(...)`, name "Switch to window: <title>", category "Windows", icon `JIPipe.RESOURCES.getIcon16("apps/jipipe.png")` (verify an existing suitable icon; any stable app icon is fine), command = `window.toFront(); window.requestFocus()`. Sources: `JIPipeDesktopProjectWindow.getOpenWindows()` and `JIPipeDesktopDataViewerWindow.getOpenWindows()` (new, below). Entries where the target is disposed/hidden are skipped.
  - `public static List<JIPipeDesktopCommandBarModel.Entry> tabEntries(Window focused)`: walk `focused`'s descendant components for `JIPipeDesktopTabPane` instances (a shallow recursive walk over `Container.getComponents()`); for each pane, one ephemeral entry per `tab` of `pane.getTabs()`, name "Switch to tab: <title>", category "Tabs", command = `pane.switchToTab(tab)`. Panes whose window is not an ancestor are skipped by construction.
  - `public static List<JIPipeDesktopCommandBarModel.Entry> all(Window focused)`: windows then tabs.
- `JIPipeDesktopDataViewerWindow`: add `private static final Set<JIPipeDesktopDataViewerWindow> OPEN_WINDOWS = new HashSet<>()` and `public static Set<JIPipeDesktopDataViewerWindow> getOpenWindows()`, mirroring `JIPipeDesktopProjectWindow` exactly (same add/remove points: construction and its dispose path).

- [ ] **Step 1: Write the failing tests** (pure: entry construction/ordering; the Swing walks themselves are thin and user-verified)

```java
@Test void windowEntryShapeAndExclusion() {
    // given two fake entries via the pure builder used internally: name prefix "Switch to window: ",
    // category "Windows", isConfigurable false, actionId null
}
@Test void allOrdersWindowsBeforeTabs() { /* all() preserves window-then-tab order */ }
```

(Structure the walks as one-liners over `JIPipeDesktopCommandBarModel.ephemeral(...)`; the tests call that factory directly for shape/ordering, so no Swing is needed.)

- [ ] **Step 2: Run to verify failure** `mvn -pl jipipe-core test -Dtest=JIPipeDesktopCommandBarSwitchingTest`
- [ ] **Step 3: Implement** the provider and the data-viewer static set.
- [ ] **Step 4: Run** the new test + full suite.
- [ ] **Step 5: Commit** `feat(#1346): window and tab switching command source`

---

### Task 4: The bar component (overlay, modes, configure, trigger scope)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBar.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/shortcuts/JIPipeDesktopShortcutDispatcher.java` (chord-prefix hook, Task 5 wires the branch - add only the hook field/accessors here)
- Test: none new (UI; user verifies in-app)

**Interfaces (produces, consumed by Tasks 5/6):**
- `JIPipeDesktopCommandBar extends JPanel`:
  - `public void install(JFrame window)`: adds itself to `window.getRootPane().getLayeredPane()` (invisible), registers with the dispatcher: (a) a `ShortcutScope` with root = the window's root pane whose resolver answers only `JIPipeShortcutActions.COMMAND_BAR_OPEN.id()` with a runnable calling `openNormalMode()`, registered on first `install`, unregistered in `uninstall()`; (b) the chord handler hook (Task 5) if not already installed (identity-checked, sink pattern).
  - `public void openNormalMode()`: shows the overlay top-center (width = window width fraction 0.5, height from content preferred size; reposition on root-pane resize via a `ComponentListener`), focuses the search field, sets `JIPipeDesktopShortcutDispatcher.setCaptureActive(true)` (dispatch suspended while open), builds the entry list from `JIPipeDesktopCommandBarModel.collect(...)`.
  - `public boolean openChordMode(KeyStroke prefix)`: called by the dispatcher's hook; if the bar's window owns the keyboard focus (`KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == window`), shows the overlay in chord mode (list from `chordEntries`), suspends dispatch, installs a local `KeyEventDispatcher` (capture-dialog pattern) that fires a listed second key (`entry.command()`), closes on Escape, closes+cancels on an unlisted key (consume everything); returns true. Otherwise false.
  - `public void close()`: hides the overlay, removes the local dispatcher, `setCaptureActive(false)`, restores focus to the previous focus owner. Every exit path (Escape, focus loss via a `FocusListener` on the search field, fire, configure) funnels through `close()`.
  - Normal-mode key handling: `InputMap`/`ActionMap` on the search field - Up/Down move the selection, Enter fires the selected available entry (`resolveAction` again at fire time via the dispatcher, then `close()`), Escape closes, either trigger variant closes (toggle). Typed text filters via a `DocumentListener` re-running `collect`.
  - Configure-Keybinding: a small affordance on the selected/hovered row (registry commands only): **first calls `close()`** (the capture dialog owns the suspend switch), then `JIPipeDesktopKeyboardCaptureDialog.capture(window, name, variants)` and the Keyboard page's commit path (skip conflicts when clearing; `JIPipeShortcutRegistry.findConflicts`; `JIPipeDesktopDialogs.choose` with an identity-checked "Assign anyway"; write via `JIPipeShortcutSettings.binding()`). The bar does not reopen afterwards.
  - Rendering: category header rows (registry order) + entry rows (name left, binding text right in gray, grayed foreground when unavailable, Configure affordance on the selected row). List re-builds on mode change; the mode is read from `CommandBarSettings.listMode()` (Task 6) at each `collect` via a small accessor that defaults to `GRAYED` until Task 6 exists (read through a package-private indirection `currentListMode()` so Task 6 only changes its body).
- Dispatcher hook (field + accessors only; the `PREFIX_STARTED` branch change is Task 5):
  - `public static void setChordPrefixHandler(Predicate<KeyStroke> handler)` / `public static Predicate<KeyStroke> currentChordPrefixHandler()` (mirror the status-sink pattern including the "remove only when owned" contract).

- [ ] **Step 1: Implement** the component and hook accessors per above. Follow `JIPipeDesktopKeyboardSection`/`JIPipeDesktopKeyboardCaptureDialog` for style; no hardcoded text sizes (row heights from renderer preferred sizes; overlay width from the window).
- [ ] **Step 2: Build** `mvn -pl jipipe-core compile`, then the full `mvn -pl jipipe-core test`.
- [ ] **Step 3: Commit** `feat(#1346): command bar overlay component`

---

### Task 5: Dispatcher chord handoff

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/shortcuts/JIPipeDesktopShortcutDispatcher.java` (`PREFIX_STARTED` case, ~line 379)
- Test: none new (the handoff is wiring; the chord filter and hint behavior are test-pinned elsewhere; user verifies in-app)

**Interfaces (consumes):** Task 4's `setChordPrefixHandler`/`currentChordPrefixHandler`.

- [ ] **Step 1: Implement.** In `dispatchKeyEvent`'s `PREFIX_STARTED` case, before the status hint: `Predicate<KeyStroke> handler = currentChordPrefixHandler(); KeyStroke prefix = chordMachine.pendingPrefix().isEmpty() ? stroke : chordMachine.pendingPrefix().get(0);` If `handler != null && handler.test(prefix)`: `chordMachine.cancel(); setCaptureActive(true); return true;`. Otherwise the existing status-hint behavior unchanged. Note: `captureActive` is already true when the handler is the bar (it sets it in `openChordMode`); setting it again here is harmless and covers a handler that forgot.
- [ ] **Step 2: Run** `mvn -pl jipipe-core test -Dtest='ShortcutScopeOrderTest,JIPipeDesktopShortcutDispatcherMapTest'` and then the full suite (consume semantics for the no-handler path must be byte-identical).
- [ ] **Step 3: Commit** `feat(#1346): dispatcher hands chord prefixes to the command bar`

---

### Task 6: List-mode setting, Keyboard page combo, window installation

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/CommandBarSettings.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopKeyboardSection.java` (`createControls`, ~204-246)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWindow.java` (both constructors, ~147 and ~169)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/api/dataviewer/JIPipeDesktopDataViewerWindow.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/commandbar/CommandBarSettingsTest.java`

**Interfaces (produces):**
- `CommandBarSettings`: `public static final String BLOCK_PATH = "registry:command-bar"`; `public static final JIPipeSettingKey<String> LIST_MODE = JIPipeSettingKey.string(BLOCK_PATH, "list-mode", String.class).defaultValue("grayed").name("Command bar list mode").description("Which commands the command bar lists: grayed-out unavailable commands, only available ones, or everything")`; lazy idempotent key registration + `static SettingsBinding<String> binding()` + `static JIPipeDesktopCommandBarModel.ListMode currentMode()` (`ListMode.parse(binding().get())` with a null-safe read exactly like `JIPipeShortcutSettings`).
- Keyboard page: a third header control in `createControls` - label "List mode:" + `JComboBox<String>` with the three human labels ("Gray out unavailable", "Available only", "Show everything"), initialized from `CommandBarSettings.currentMode()`, writing `"grayed"/"available-only"/"everything"` through `CommandBarSettings.binding()` on selection; no listener storm (guard with an updating flag like the spinner's).
- `JIPipeDesktopCommandBar.currentListMode()` (Task 4's indirection) now delegates to `CommandBarSettings.currentMode()`.
- Installation: both windows create a `JIPipeDesktopCommandBar` field, `install(this)` in their initialization (after the root pane exists), `uninstall()` in their dispose paths (project window: both constructors already call `JIPipeDesktopShortcutDispatcher.install()` - add the bar right there; data viewer: same pattern at its construction/dispose).

- [ ] **Step 1: Write the failing test** `CommandBarSettingsTest`: `currentMode()` parses stored ids and falls back to `GRAYED` on null/unknown (boot the live settings service headlessly like `KeymapSettingsJsonTest.currentIsNonNullWithDefaultsWhenNothingStored` does; if the string binding is awkward to pin headlessly, pin `ListMode.parse` + the key constant and say so in the report).
- [ ] **Step 2: Run to verify failure** `mvn -pl jipipe-core test -Dtest=CommandBarSettingsTest`
- [ ] **Step 3: Implement** the settings holder, the combo, the indirection, and the window installations.
- [ ] **Step 4: Run** the new test + full suite.
- [ ] **Step 5: Commit** `feat(#1346): command bar list mode setting and window installation`

---

### Task 7: Fuzzy search (in-branch follow-up)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBarSearch.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBar.java` (filter path)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBarSearchTest.java`

**Interfaces (produces):**
- `JIPipeDesktopCommandBarSearch` (static utility): the same primitives `JIPipeEnhancedNodeDatabaseSearch` uses (commons-text `StringMetrics.jaroWinkler()`, word splitting on `[\\s_\\-/\\.]+` and camel-case boundaries) applied to command names:
  - `public static double score(String query, String name)`: 0.0 when no match; ladder per query token - exact word 1.0, word prefix 0.9, substring 0.8, jaroWinkler similarity >= 0.85 -> `0.65 + (sim - 0.85)`; score = mean of per-token scores, 0.0 if any token unmatched. Camel-case/acronym matching falls out of the word splitting ("cb" matches "Command Bar" via prefixes).
  - `public static boolean matches(String query, String name)`: `score > 0`.
- Bar filter change: with a non-empty query, entries are kept when `matches(query, entry.name())` OR case-insensitive `contains` on the category (category match keeps today's behavior); ordering stays registry order in the base bar (MRU re-ranking arrives in Task 8); with an empty query everything is listed. The plain-substring path remains for category matching only.

- [ ] **Step 1: Write the failing tests**

```java
@Test void ladderRanksExactAbovePrefixAboveSubstringAboveFuzzy() {
    // score("copy","Copy") > score("cop","Copy") > score("cpy","Copy") > 0; score("xyz","Copy")==0
}
@Test void acronymViaWordPrefixes() { /* matches("cb","Command Bar") is true */ }
@Test void everyTokenMustMatch() { /* score("copy delete","Copy")==0 (delete unmatched) */ }
@Test void caseInsensitive() { /* score("COPY","copy")==1.0 */ }
```

- [ ] **Step 2: Run to verify failure** `mvn -pl jipipe-core test -Dtest=JIPipeDesktopCommandBarSearchTest`
- [ ] **Step 3: Implement** the scorer and the bar filter change.
- [ ] **Step 4: Run** the new test + full suite.
- [ ] **Step 5: Commit** `feat(#1346): fuzzy matching for the command bar`

---

### Task 8: MRU (in-branch follow-up)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/CommandBarState.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/CommandBarSettings.java` (state key)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/commandbar/JIPipeDesktopCommandBar.java` (record on fire, Recently-used section, tie-break)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/commandbar/CommandBarStateTest.java`

**Interfaces (produces):**
- `CommandBarState`: Jackson bean with `List<String> recentCommandIds` (default empty list, getter returns a copy, `@JsonGetter`/`@JsonSetter` explicit per the mapper-visibility convention of `KeymapSettings`); `void addRecent(String actionId)` (insert at front, dedupe, cap at 20); `static List<String> knownRecent(List<String> recent, Predicate<String> isKnown)` (filters unknown ids, order preserved) - the bar passes `registry::isRegistered`; settings key `CommandBarSettings.STATE = JIPipeSettingKey.object(BLOCK_PATH, "state", CommandBarState.class)` registered lazily like `LIST_MODE`; `static void recordFired(String actionId)` reads the state through the binding, calls `addRecent`, writes back only when the list is non-empty (defaults never materialized).
- Bar: after firing a registry command, `CommandBarState.recordFired(actionId)`. On open with an empty query, a "Recently used" category header is prepended listing up to 5 known registry commands (id resolvable in the registry, most recent first, each still subject to the list mode). On a queried search, equal fuzzy scores are broken by recency (index in the recent list; unknown ids rank last).

- [ ] **Step 1: Write the failing tests**

```java
@Test void addRecentDedupesAndCaps() { /* 25 adds -> size 20, most recent first, middle re-add moves to front */ }
@Test void jsonRoundTrip() { /* toJsonString/readFromString preserves order; empty list serializes as [] */ }
@Test void knownRecentFiltersUnknownIds() { /* unknown ids dropped, order preserved, known kept */ }
@Test void recordFiredSkipsEmptyWrite() { /* a state that stays empty is never written (binding value remains absent) - pin via the state's write guard helper if the live binding is awkward headlessly, and say so */ }
```

- [ ] **Step 2: Run to verify failure** `mvn -pl jipipe-core test -Dtest=CommandBarStateTest`
- [ ] **Step 3: Implement** the bean, key, and bar integration.
- [ ] **Step 4: Run** the new test + full suite.
- [ ] **Step 5: Commit** `feat(#1346): recently-used ranking for the command bar`

---

### Task 9: Docs and full verification

**Files:**
- Modify: `AGENTS.md` (extend the "Keyboard Shortcuts" subsection by 3-5 lines)
- Modify: `docs/superpowers/specs/2026-11-14-command-bar-design.md` only if implementation revealed a spec deviation (record it, do not silently diverge)

- [ ] **Step 1: AGENTS.md.** Append to the existing Keyboard Shortcuts subsection: command bar entries are registry actions (including unassigned ones); the trigger is `org.hkijena.jipipe.command-bar.open`; window/tab switching are ephemeral bar entries, not registry actions; the chord prefix opens the bar in chord mode via the dispatcher handoff.
- [ ] **Step 2: Full verification** `mvn clean compile` then `mvn -pl jipipe-core test` (all green, only the 2 known skips).
- [ ] **Step 3: Commit** `docs(#1346): command bar developer docs`

User verification checklist (in-app, after implementation): trigger `Ctrl+P`/`Ctrl+Shift+P` opens and toggles the bar in project and data viewer windows; chord prefix (e.g. `Ctrl+T`) opens the bar in chord mode and the second key fires; unlisted second key cancels and closes; Escape and focus loss close and restore shortcuts; grayed rows do not fire; Configure-Keybinding round-trips through the capture dialog with conflict warning; window/tab switching entries fire; LEGACY preset still restores old bindings and shadows `Ctrl+P`/`Ctrl+Shift+P` inside graph editors.
