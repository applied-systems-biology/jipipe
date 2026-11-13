# Keyboard Shortcut Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace JIPipe's four hardcoded keyboard shortcut systems with one open shortcut registry (stable IDs, in-code defaults, user overrides in the settings document), chord support ("Microsoft-style" two-key sequences), reliable focus-independent triggering, dock panel toggle shortcuts, and a Keyboard settings page with presets.

**Architecture:** Pure core model in `org.hkijena.jipipe.api.shortcuts` (actions, registry, bindings, chord state machine, persisted `KeymapSettings`); a single JVM-wide `KeyEventDispatcher` on the desktop side resolves strokes through per-component `ShortcutScope`s (availability is asked live, never cached); graph editors, menus, and dock panels migrate onto the registry; the Keyboard settings page (already a reserved placeholder section) renders the table, capture dialog (#1341 API), and presets.

**Tech Stack:** Java 21, Swing, Jackson (settings document), JUnit 5 (jipipe-core test setup with `ImageJLegacyPatcherLauncherSessionListener` already in place).

**Spec:** `docs/superpowers/specs/2026-11-13-keyboard-shortcut-overhaul-design.md` (rev. 2, commit `3e67688b22`)

## Global Constraints

- Branch: `1346-keyboard-shortcut-overhaul`, created in Task 1 from `master`.
- Java 21, Maven multi-module; build with `mvn clean compile`, tests with `mvn -pl jipipe-core test -Dtest=<TestClass>`.
- No em dashes (U+2014) or en dashes (U+2013) anywhere in code, Javadoc, comments, or strings. Use `-`.
- Registration IDs (action IDs, sheet IDs) are stable string literals, never derived from code (`AGENTS.md` rule).
- No Swing interaction tests; the user verifies UI behavior in the running app. Tests cover non-trivial logic only.
- Never hardcode text-dependent pixel sizes in new UI; measure or derive from preferred sizes.
- Do not touch `ImageJLegacyPatcherLauncherSessionListener` or its `META-INF/services` registration.
- Settings rules: defaults are never materialized into the document; unknown keys pass through; bindings are live-observable (`SettingsBinding`).
- Chord semantics (locked): prefix can never be a standalone action; timeout default 2000 ms, `0` = wait indefinitely; Escape cancels a pending chord; non-matching second key is swallowed and cancels; pending prefix shows a status bar hint.
- Every task ends with a green build and a commit.

## Review Focus

- **QWERTZ `+`/`-` behind Shift** (user's report): zoom must fire via the numpad variant when the main variant is unreachable; pinned by the variant-matching test in Task 4.
- **The reported focus bug** (click toolbar toggle, press Ctrl+D): pure logic is tested (scope order, text guard), but the end-to-end behavior needs the user's manual check after Task 6.
- **Ctrl+C in a node name field must copy text, not nodes**: pinned by the text-guard predicate test in Task 5.
- **Overrides for removed/unknown action IDs** (plugin uninstalled) must survive saves verbatim: pinned in Task 3.
- **Overlapping scopes** (two dock panels, editor inside editor): the first non-null resolver in innermost-first order must win; pinned by the scope-order test in Task 5.

---

### Task 1: Core shortcut model and registry

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutAction.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutBinding.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutRegistry.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutRegistryTest.java`

**Interfaces (produces, used by all later tasks):**
- `JIPipeShortcutAction(String id, String name, String description, String category, String scopeFamily, boolean textInputSafe, List<List<KeyStroke>> defaultVariants)` plus `static Builder builder(String id, String name, String category)` with fluent `description(String)`, `scopeFamily(String)`, `textInputSafe(boolean)`, `defaultVariants(List<List<KeyStroke>>)`, `defaultChord(KeyStroke... stroke)` (wraps one single-stroke variant), `build()`. Getters `id()`, `name()`, `description()`, `category()`, `scopeFamily()`, `isTextInputSafe()`, `defaultVariants()`. `equals`/`hashCode` on `id` only. Family constants: `public static final String FAMILY_GRAPH = "graph"`, `FAMILY_PROJECT = "project"`, `FAMILY_DOCK = "dock"`.
- `record JIPipeShortcutBinding(String actionId, List<List<KeyStroke>> variants, Source source)` with `enum Source { DEFAULT, OVERRIDE, UNASSIGNED }` and `boolean isUnassigned()`.
- `JIPipeShortcutRegistry.getInstance()`; `void register(JIPipeShortcutAction)` throws `IllegalStateException` on duplicate id; `JIPipeShortcutAction byId(String)` (null when unknown); `boolean isRegistered(String)`; `List<JIPipeShortcutAction> all()` (sorted by category, then name); `List<JIPipeShortcutAction> byCategory(String)`; `void addChangeListener(Runnable)` / `removeChangeListener(Runnable)` (fired on register, so the dispatcher can rebuild stroke maps when plugins add dock actions).
- `static String formatChord(List<KeyStroke> chord)`: joins per-stroke `UIUtils.keyStrokeToString(stroke)` with `" then "`; empty list renders `"-"`.
- `static String formatVariants(List<List<KeyStroke>> variants)`: joins `formatChord` of each with `" or "`.

- [ ] **Step 1: Write the failing test** `JIPipeShortcutRegistryTest`

```java
@Test void registerAndLookup() {
    JIPipeShortcutRegistry r = new JIPipeShortcutRegistry();   // package-private ctor for tests; getInstance() wraps a singleton
    JIPipeShortcutAction a = JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.duplicate", "Duplicate", "Selection")
            .scopeFamily(JIPipeShortcutAction.FAMILY_GRAPH).defaultChord(KeyStroke.getKeyStroke("control pressed D")).build();
    r.register(a);
    assertSame(a, r.byId("org.hkijena.jipipe.graph.node.duplicate"));
    assertTrue(r.isRegistered("org.hkijena.jipipe.graph.node.duplicate"));
    assertFalse(r.isRegistered("org.hkijena.jipipe.graph.node.nope"));
}
@Test(expected = IllegalStateException.class) void duplicateIdFailsFast() { /* register same id twice */ }
@Test void allIsSortedByCategoryThenName() { /* register "B"/"cat2", "A"/"cat1", "C"/"cat1" -> [A, C, B] */ }
@Test void changeListenerFiresOnRegister() { /* count == 1 after one register */ }
@Test void formatChordRendersChordAndEmpty() {
    assertEquals("Ctrl+T then M", JIPipeShortcutRegistry.formatChord(List.of(
            KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))));
    assertEquals("-", JIPipeShortcutRegistry.formatChord(List.of()));
    assertEquals("Ctrl+= or Ctrl+Num+", JIPipeShortcutRegistry.formatVariants(List.of(
            List.of(KeyStroke.getKeyStroke("control pressed EQUALS")),
            List.of(KeyStroke.getKeyStroke("control pressed ADD")))));
}
```

Note: pin `formatChord`'s exact spacing to `UIUtils.keyStrokeToString` output ("Ctrl+T"); if `keyStrokeToString` renders differently, adapt the expected strings to its real output, not the other way round.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -Dtest=JIPipeShortcutRegistryTest`
Expected: FAIL (classes missing)

- [ ] **Step 3: Implement the three classes** exactly as specified in Interfaces.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-core test -Dtest=JIPipeShortcutRegistryTest`
Expected: PASS

- [ ] **Step 5: Commit** `feat(#1346): shortcut action model and registry`

---

### Task 2: Chord state machine

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeChordStateMachine.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/JIPipeChordStateMachineTest.java`

**Interfaces (produces):**
- `public enum EventResult { PASSTHROUGH, PREFIX_STARTED, CHORD_COMPLETED, CANCELLED }`
- `public record KeyEventResult(EventResult result, List<KeyStroke> completedChord)` (`completedChord` null unless `CHORD_COMPLETED`)
- `void configure(long timeoutNanos, Set<List<KeyStroke>> prefixes)` (prefixes = the *first* strokes of all length-2 bindings, each wrapped as a one-element list; resets to idle)
- `KeyEventResult keyPressed(KeyStroke stroke, long nanoTime)`
- `void cancel()`, `boolean isPending()`, `List<KeyStroke> pendingPrefix()`

Semantics (spec Section 6): idle + stroke in prefixes -> `PREFIX_STARTED`; idle + other -> `PASSTHROUGH`; pending + Escape (`VK_ESCAPE`, no modifiers) -> `CANCELLED`; pending + any other stroke -> `CHORD_COMPLETED([prefix, stroke])`; pending + timeout expired -> behave as idle for this stroke; any result except `PASSTHROUGH` means the caller must consume the event. `configure` cancels a pending chord.

- [ ] **Step 1: Write the failing test**

```java
private static final KeyStroke CTRL_T = KeyStroke.getKeyStroke("control pressed T");
private static final KeyStroke D = KeyStroke.getKeyStroke("pressed D");
private static final KeyStroke ESC = KeyStroke.getKeyStroke("pressed ESCAPE");

@Test void prefixStartsAndCompletes() {
    JIPipeChordStateMachine m = new JIPipeChordStateMachine();
    m.configure(2_000_000_000L, Set.of(List.of(CTRL_T)));
    assertEquals(EventResult.PREFIX_STARTED, m.keyPressed(CTRL_T, 0).result());
    assertTrue(m.isPending());
    KeyEventResult r = m.keyPressed(D, 100);
    assertEquals(EventResult.CHORD_COMPLETED, r.result());
    assertEquals(List.of(CTRL_T, D), r.completedChord());
}
@Test void nonMatchingSecondKeyStillCompletesForCallerToCancel() {
    // configure prefix Ctrl+T; press Ctrl+T, then X -> CHORD_COMPLETED([Ctrl+T, X]) (swallowed; dispatcher cancels)
}
@Test void escapeCancelsPending() { /* PREFIX_STARTED then ESC -> CANCELLED, isPending() == false */ }
@Test void timeoutExpiresThenStrokeIsEvaluatedAsIdle() {
    // configure timeout 100ns; press Ctrl+T at 0 (PREFIX_STARTED); press D at 1_000_000 -> PASSTHROUGH (D not a prefix)
}
@Test void zeroTimeoutWaitsIndefinitely() { /* timeout 0; press Ctrl+T at 0, D at 10_000_000_000L -> CHORD_COMPLETED */ }
@Test void prefixAloneIsNeverAnAction() { /* Ctrl+T pressed twice: first PREFIX_STARTED, second CHORD_COMPLETED([Ctrl+T, Ctrl+T]) - the dispatcher finds no action and cancels */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -Dtest=JIPipeChordStateMachineTest`
Expected: FAIL (class missing)

- [ ] **Step 3: Implement `JIPipeChordStateMachine`** (pure class, no Swing beyond `KeyStroke`).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-core test -Dtest=JIPipeChordStateMachineTest`
Expected: PASS

- [ ] **Step 5: Commit** `feat(#1346): chord state machine`

---

### Task 3: KeymapSettings persistence

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/KeymapSettings.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutSettings.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/KeymapSettingsJsonTest.java`

**Interfaces (produces):**
- `KeymapSettings`: Jackson bean with `String activeProfileId` (nullable), `Map<String, List<List<String>>> overrides` (default empty `LinkedHashMap`), `long chordTimeoutMs` (default `2000L`). Typed helpers: `List<List<KeyStroke>> overrideVariants(String actionId)` (parses each string with `KeyStroke.getKeyStroke(String)`, silently drops malformed entries with a `logger.warn`), `void setOverride(String actionId, List<List<KeyStroke>> variants)` (null/empty removes the key), `void removeOverride(String actionId)`, `boolean hasOverride(String actionId)`.
- `JIPipeShortcutSettings`: `public static final String BLOCK_PATH = "registry:keymap"`; `public static final JIPipeSettingKey<KeymapSettings> KEYMAP = JIPipeSettingKey.object(BLOCK_PATH, "", KeymapSettings.class)`; `static SettingsBinding<KeymapSettings> binding()` (lazy static binding over `JIPipe.getSettings().bind(KEYMAP)`, registering the key first the way `JIPipeRecentProjectsRegistry.postprocess()` does, `JIPipeRecentProjectsRegistry.java:83` is the pattern); `static KeymapSettings current()` = `binding().get()`.

- [ ] **Step 1: Write the failing test**

```java
@Test void jsonRoundTrip() {
    KeymapSettings s = new KeymapSettings();
    s.setOverride("org.hkijena.jipipe.graph.tool.move", List.of(List.of(
            KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))));
    s.setOverride("org.hkijena.jipipe.graph.tool.connect", List.of());   // removed, must not appear
    String json = JsonUtils.writeObjectToJson(s);
    KeymapSettings back = JsonUtils.readJson(json, KeymapSettings.class);
    assertEquals(2000L, back.getChordTimeoutMs());
    assertEquals(1, back.getOverrides().size());
    // KeyStroke.toString() canonical form ("ctrl", not "control"); parse accepts it again
    assertEquals(List.of(List.of("ctrl pressed T", "pressed M")),
            back.getOverrides().get("org.hkijena.jipipe.graph.tool.move"));
    assertEquals(1, back.overrideVariants("org.hkijena.jipipe.graph.tool.move").size());
}
@Test void unknownActionIdsSurvive() {
    // overrides containing "org.hkijena.jipipe.plugin.that.went.away.x" survive write+read verbatim
}
@Test void malformedChordDroppedNotThrown() {
    // overrides {"x": [["not a keystroke"]]} -> overrideVariants("x") is empty, no exception
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -Dtest=KeymapSettingsJsonTest`
Expected: FAIL (classes missing)

- [ ] **Step 3: Implement both classes.** `JsonUtils` is the project's Jackson facade (`org.hkijena.jipipe.utils.json.JsonUtils`).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-core test -Dtest=KeymapSettingsJsonTest`
Expected: PASS

- [ ] **Step 5: Commit** `feat(#1346): keymap settings persistence`

---

### Task 4: Binding resolution, conflicts, and presets

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutRegistry.java` (add `bindingFor`, `findConflicts`, `chordPrefixes`)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutProfiles.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutActions.java` (the core catalog constants; registration helper)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutResolveTest.java`

**Interfaces (produces):**
- `JIPipeShortcutRegistry.bindingFor(String actionId, KeymapSettings settings) -> JIPipeShortcutBinding`: override present -> `OVERRIDE` with the override variants; else default variants non-empty -> `DEFAULT`; else `UNASSIGNED`.
- `JIPipeShortcutRegistry.findConflicts(String actionId, List<List<KeyStroke>> variants, KeymapSettings settings) -> List<String>`: ids of *other registered actions* in the **same scope family** where any candidate variant equals any of the other action's effective variants (order-insensitive chord equality). `null`/empty variants conflict with nothing.
- `JIPipeShortcutRegistry.chordPrefixes(KeymapSettings settings) -> Set<List<KeyStroke>>`: first strokes of every length-2 variant across all bindings.
- `JIPipeShortcutProfiles`: `public static final JIPipeShortcutProfile DEFAULT` (id `"default"`, empty overrides), `public static final JIPipeShortcutProfile LEGACY` (id `"legacy"`), `static List<JIPipeShortcutProfile> bundled()`, `static void apply(JIPipeShortcutProfile profile, KeymapSettings settings)` (DEFAULT -> `settings.getOverrides().clear()`; else put all overrides).
- `JIPipeShortcutActions`: `public static void registerCore()` registers every core action constant (IDs below). Constants are `public static final JIPipeShortcutAction` so later tasks reference them by name. Registration is idempotent (skip when `isRegistered`).

Action ID list for `registerCore()` (category in parentheses; all `FAMILY_GRAPH` unless noted; `textInputSafe` false unless noted):

| Constant | ID | Default |
|---|---|---|
| TOOL_DEFAULT | `org.hkijena.jipipe.graph.tool.default` | ESC |
| TOOL_MOVE | `org.hkijena.jipipe.graph.tool.move` | `Ctrl+T` then `M` |
| TOOL_CONNECT | `org.hkijena.jipipe.graph.tool.connect` | `Ctrl+T` then `C` |
| TOOL_REWIRE | `org.hkijena.jipipe.graph.tool.rewire` | `Ctrl+T` then `R` |
| TOOL_CROP_VIEW | `org.hkijena.jipipe.graph.tool.crop-view` | `Ctrl+T` then `V` |
| EDITOR_UNDO | `org.hkijena.jipipe.graph.undo` | `Ctrl+Z` |
| EDITOR_REDO | `org.hkijena.jipipe.graph.redo` | `Ctrl+Shift+Z` |
| EDITOR_ZOOM_IN | `org.hkijena.jipipe.graph.zoom-in` | `Ctrl+=` **and** `Ctrl+Num+` (two variants) |
| EDITOR_ZOOM_OUT | `org.hkijena.jipipe.graph.zoom-out` | `Ctrl+-` **and** `Ctrl+Num-` |
| EDITOR_ZOOM_RESET | `org.hkijena.jipipe.graph.zoom-reset` | `Ctrl+0` **and** `Ctrl+Num0` |
| EDITOR_MOVE_SELECTION_* | `org.hkijena.jipipe.graph.move-selection-left/right/up/down` | bare arrows (Editor category) |
| PROJECT_NEW/OPEN/SAVE/SAVE_AS | `org.hkijena.jipipe.project.new/open/save/save-as` | `Ctrl+N` / `Ctrl+O` / `Ctrl+S` / `Ctrl+Shift+S` (category "Menus", `FAMILY_PROJECT`, `textInputSafe` true) |

Context-action IDs are declared on the action classes in Task 6 and registered by the same `registerCore()` plus a `registerContextAction(JIPipeShortcutAction)` group in `JIPipeShortcutActions` covering (IDs literal, names = the action's `getName()`):

- Selection: `graph.node.select-all` `Ctrl+A`, `graph.node.select-and-move-here` `Ctrl+G`, `graph.node.add` `Shift+A`, `graph.compartment.add` `Shift+A`, `graph.node.add-parameter-set` `Ctrl+Shift+A`, `graph.node.isolate` `Ctrl+Shift+I` (category "Selection"/"Creation" for the three add actions)
- Clipboard: `graph.node.copy` `Ctrl+C`, `graph.node.cut` `Ctrl+X`, `graph.node.paste` `Ctrl+V`, `graph.node.duplicate` `Ctrl+D`, `graph.node.duplicate-with-inputs` `Ctrl+Shift+D`, `graph.edge.copy` `Ctrl+C` (category "Selection")
- Deletion: `graph.node.delete` `Delete`, `graph.edge.delete-control-point` `Delete`, `graph.edge.clear-control-points` `Shift+Delete` (category "Selection")
- Input/output: `graph.node.enable` `Ctrl+E`, `graph.node.disable` `Ctrl+Shift+E`, `graph.node.pass-through-enable` `Ctrl+Alt+P`, `graph.node.pass-through-disable` `Ctrl+Shift+P` (category "Input/output")
- Locking (revised): `graph.node.lock` `Ctrl+L`, `graph.node.unlock` `Ctrl+Shift+L` (category "Input/output")
- Layers: `graph.node.raise` `Shift+PageUp`, `graph.node.lower` `Shift+PageDown`, `graph.node.send-to-foreground` `Ctrl+Shift+PageUp`, `graph.node.send-to-background` `Ctrl+Shift+PageDown` (category "Layers")
- Running: `graph.node.run-show-results` `Ctrl+Enter`, `graph.node.update-cache` `Ctrl+Space` (category "Running")
- Navigation: `graph.node.move-ui-left/right/up/down` `Ctrl+Left/Right/Up/Down` (category "Navigation")

- [ ] **Step 1: Write the failing test**

```java
@Test void overrideBeatsDefault() { /* move: default Ctrl+T,M; settings.setOverride -> OVERRIDE with override variants */ }
@Test void unassignedWhenNoDefaultAndNoOverride() { /* dock-style action with empty defaultVariants -> UNASSIGNED */ }
@Test void zoomHasTwoDefaultVariants() {
    JIPipeShortcutActions.registerCore();
    JIPipeShortcutBinding b = JIPipeShortcutRegistry.getInstance()
            .bindingFor(JIPipeShortcutActions.EDITOR_ZOOM_IN.id(), new KeymapSettings());
    assertEquals(2, b.variants().size());
}
@Test void chordPrefixesContainToolPrefix() { /* chordPrefixes contains [Ctrl+T] */ }
@Test void conflictSameFamilyOnly() {
    // graph.node.copy (Ctrl+C) vs graph.edge.copy (Ctrl+C): findConflicts(copy.id, [Ctrl+C]) contains edge.copy
    // but findConflicts(project.new, [Ctrl+C]) is empty (different family)
}
@Test void legacyPresetRestoresOldBindings() {
    JIPipeShortcutActions.registerCore();
    KeymapSettings s = new KeymapSettings();
    JIPipeShortcutProfiles.apply(JIPipeShortcutProfiles.LEGACY, s);
    assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed F1"))),
            JIPipeShortcutRegistry.getInstance().bindingFor(JIPipeShortcutActions.TOOL_MOVE.id(), s).variants());
    assertEquals(List.of(List.of(KeyStroke.getKeyStroke("pressed L"))),
            ...bindingFor(...LOCK...).variants());
    assertEquals(List.of(List.of(KeyStroke.getKeyStroke("control pressed ADD"))),
            ...bindingFor(EDITOR_ZOOM_IN...).variants());
}
@Test void applyIsIdempotent() { /* apply LEGACY twice -> same map */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -Dtest=JIPipeShortcutResolveTest`
Expected: FAIL

- [ ] **Step 3: Implement** `bindingFor`/`findConflicts`/`chordPrefixes`, `JIPipeShortcutProfiles` (LEGACY overrides: F1/F2/F3/F12/F4/F6/F7 tools, notes edit ESC, numpad-only zoom, `L` lock, `Ctrl+L` unlock, `Ctrl+P`/`Ctrl+Shift+P` pass-through), and `JIPipeShortcutActions` with every constant from the tables above.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-core test -Dtest=JIPipeShortcutResolveTest`
Expected: PASS

- [ ] **Step 5: Commit** `feat(#1346): binding resolution, conflict detection, presets`

---

### Task 5: Desktop dispatcher, scopes, text guard

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutTextGuard.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/shortcuts/ShortcutScope.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/shortcuts/JIPipeDesktopShortcutDispatcher.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutTextGuardTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/shortcuts/ShortcutScopeOrderTest.java`

**Interfaces (produces):**
- `JIPipeShortcutTextGuard.isTextInput(Component c) -> boolean`: true when `c` is an editable `JTextComponent` (covers spinner and combo editors, which host one), false otherwise; null-safe.
- `ShortcutScope(final JComponent root, final Resolver resolver)` with `public interface Resolver { Runnable resolve(String actionId); }` (null = unavailable), `JComponent root()`, `Resolver resolver()`, `boolean contains(Component focusOwner)` (`SwingUtilities.isDescendingFrom`).
- `JIPipeDesktopShortcutDispatcher implements KeyEventDispatcher`:
  - `public static synchronized void install()` idempotent (`KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(...)`), `public static JIPipeDesktopShortcutDispatcher getInstance()`
  - `public void registerScope(ShortcutScope)` / `public void unregisterScope(ShortcutScope)`
  - `public void rebuildStrokeMaps()` (also called by a change listener on `JIPipeShortcutSettings.binding()` and on registry changes): builds `Map<KeyStroke, String> singleStrokeMap` (length-1 variants) and `Map<List<KeyStroke>, String> chordMap` (length-2 variants) and reconfigures the `JIPipeChordStateMachine` with `settings.getChordTimeoutMs() * 1_000_000L` and `registry.chordPrefixes(settings)`
  - `dispatchKeyEvent(KeyEvent e)`: ignore non-`KEY_PRESSED`; normalize with `KeyStroke.getKeyStrokeForEvent(e)`; run the chord machine; then resolve:
    1. `PASSTHROUGH` -> look up `singleStrokeMap`; no hit -> return false
    2. `CHORD_COMPLETED` -> look up `chordMap`; no hit -> machine reset, status "Chord cancelled", return true
    3. `PREFIX_STARTED` -> status bar hint `(Ctrl+T) was pressed, waiting for second key of chord`, return true
    4. `CANCELLED` -> clear status hint, return true
    5. action hit: order all scopes containing the focus owner innermost-first (`ShortcutScopeOrderTest` pins this); first `Resolver.resolve(actionId)` returning non-null wins; text guard: if `JIPipeShortcutTextGuard.isTextInput(focusOwner)` and `!action.isTextInputSafe()` -> not resolvable (fall through: `PASSTHROUGH` returns false; chord results return true because the second key was already swallowed); no resolver fired -> as before
    6. fire `SwingUtilities.invokeLater(runnable)`, status bar `Executed: <name>` (via the workbench of the scope's window when reachable, else skip), return true
  - On `WindowClosedEvent`/dispose of hosts nothing special: scopes unregister themselves (Task 6/8 call `unregisterScope` in their dispose paths).

- [ ] **Step 1: Write the failing tests**

```java
// JIPipeShortcutTextGuardTest (headless-safe: creating components is fine)
@Test void textFieldIsTextInput() { JTextField f = new JTextField(); f.setEditable(true); assertTrue(JIPipeShortcutTextGuard.isTextInput(f)); }
@Test void disabledTextFieldIsNot() { JTextField f = new JTextField(); f.setEditable(false); assertFalse(...); }
@Test void labelIsNot() { assertFalse(JIPipeShortcutTextGuard.isTextInput(new JLabel("x"))); }
@Test void nullIsNot() { assertFalse(JIPipeShortcutTextGuard.isTextInput(null)); }

// ShortcutScopeOrderTest
@Test void innermostScopeWins() {
    // outer panel P with resolver A, child panel C (in P) with resolver B; scope list [P-scope, C-scope]
    // orderScopes(containing focus in C) -> [C-scope, P-scope]; first non-null resolve wins
}
@Test void scopeNotContainingFocusIsIgnored() { /* sibling panel scope is excluded */ }
```

Extract the ordering as a static helper on the dispatcher (`static List<ShortcutScope> orderedContainingScopes(List<ShortcutScope> scopes, Component focusOwner)`) so it is testable without installing the global dispatcher.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl jipipe-core test -Dtest='JIPipeShortcutTextGuardTest,ShortcutScopeOrderTest'`
Expected: FAIL (classes missing)

- [ ] **Step 3: Implement** the guard, scope, and dispatcher as specified. The dispatcher class lives under `desktop/app/shortcuts` inside jipipe-core (that is where all desktop UI code lives in this project).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl jipipe-core test -Dtest='JIPipeShortcutTextGuardTest,ShortcutScopeOrderTest'`
Expected: PASS

- [ ] **Step 5: Commit** `feat(#1346): desktop shortcut dispatcher and scopes`

---

### Task 6: Graph editor migration (context actions + focus fix)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/GraphInteractiveObjectUIContextAction.java` (replace `getKeyboardShortcut()` with `default String getShortcutActionId() { return null; }`)
- Modify: all ~36 implementing classes (list: every file matching `**/contextmenu/**/*ContextAction.java` under `jipipe-core/src/main/java`): add `public static final String SHORTCUT_ACTION_ID = "<id from Task 4 tables>"`, override `getShortcutActionId()`, delete `getKeyboardShortcut()` and now-unused `KeyStroke`/`KeyEvent`/`InputEvent` imports
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphCanvasUI.java` (delete `initializeHotkeys()` and its call)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (install scope, registry-driven tooltips)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/StandardSettingsPlugin.java` (call `JIPipeShortcutActions.registerCore()` in `register()`)
- Test: none new (availability/run logic is the unchanged `matches`/`run` of the actions; dispatch wiring is Task 5's tested logic; the user verifies the focus fix in the running app)

**Interfaces (consumes/produces):**
- Consumes Task 4/5: `JIPipeShortcutRegistry.getInstance()`, `bindingFor`, `formatVariants`, `JIPipeShortcutActions.*` constants, `ShortcutScope`, `JIPipeDesktopShortcutDispatcher.getInstance().registerScope/unregisterScope`.
- Produces: each context action class exposes `SHORTCUT_ACTION_ID` and `getShortcutActionId()`.

- [ ] **Step 1: Add the interface method** and delete `getKeyboardShortcut()` from `GraphInteractiveObjectUIContextAction`.

- [ ] **Step 2: Migrate all context action classes** to `SHORTCUT_ACTION_ID` + `getShortcutActionId()`; remove `getKeyboardShortcut()` overrides. Flavor sharing: pipeline `AlgorithmGraphCopyNodeUIContextAction` and compartments `JIPipeDesktopCompartmentsCopyNodeUIContextAction` both use `org.hkijena.jipipe.graph.node.copy` (same for cut/paste); pipeline-only duplicate actions use `graph.node.duplicate` / `graph.node.duplicate-with-inputs`; the two delete actions share `graph.node.delete`; `AddNewNodeUIContextAction` -> `graph.node.add`, `AddNewCompartmentUIContextAction` -> `graph.compartment.add`.

- [ ] **Step 3: Delete the canvas hotkey dispatcher** (`initializeHotkeys()` and its call site in the `JIPipeDesktopGraphCanvasUI` constructor).

- [ ] **Step 4: Install the editor scope in `JIPipeDesktopGraphEditorUI`.** In the editor's initialize (after canvas/context actions exist): create a `ShortcutScope` with `root = this` and a resolver that, for the given id: (a) looks up the context action by `getShortcutActionId()` among `canvasUI.getContextActions()`, returns null unless `action.matches(canvasUI.getSelectionManager().getSelection())`, else a runnable calling `action.run(canvasUI, selection)`; (b) tool ids (`graph.tool.*`) return a runnable calling the editor's existing `selectTool(tool)` for the tool previously identified by `getKeyBinding()`; (c) `graph.undo`/`graph.redo`/`graph.zoom-in`/`graph.zoom-out`/`graph.zoom-reset` return runnables for the existing `undo()`/`redo()`/`canvasUI.zoomIn()`/`zoomOut()`/`resetZoom()` calls; (d) `graph.move-selection-*` return the existing `canvasUI.moveSelection(dx, dy, false)` runnables; (e) otherwise null. Register with the dispatcher; `unregisterScope` in the editor's dispose path. Register the dispatcher itself (`install()`) at the top of the same initialize.

- [ ] **Step 5: Registry-driven shortcut display.** Replace `contextAction.getKeyboardShortcut()` at `JIPipeDesktopGraphEditorUI.java:1045` and the tool tooltip construction (`:867`, `:882`) with lookups of `bindingFor(...).variants()` rendered via `JIPipeShortcutRegistry.formatVariants(...)` passed into `TooltipUtils.createTooltipWithShortcut(name, description, null)`'s replacement: add `TooltipUtils.createTooltipWithShortcutText(String name, String description, String shortcutText)` and use it (the old `KeyStroke` overload is deleted in Task 10 once unused).

- [ ] **Step 6: Wire registration** `JIPipeShortcutActions.registerCore()` into `StandardSettingsPlugin.register()`.

- [ ] **Step 7: Build** `mvn -pl jipipe-core compile` (expect unused-import warnings only if left; fix them).

- [ ] **Step 8: Commit** `feat(#1346): graph editor actions onto the shortcut registry (focus fix)`

---

### Task 7: Tools, static strokes, hold-Ctrl transient notes

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/grapheditortool/JIPipeDesktopGraphEditorTool.java` (delete `getKeyBinding()`)
- Modify: the 9 tool classes (`tools/*.java`, `plugins/canvasnotes/.../tools/*.java`): delete `getKeyBinding()` overrides and unused imports
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (re-key tool cycling on chords; replace `KEY_STROKE_*` constants with registry lookups)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphCanvasUI.java` (hold-Ctrl transient note delegation)
- Modify: `plugins/jipipe-plugin-canvasnotes/...` plugin class (register the 4 notes tool actions: `org.hkijena.jipipe.canvasnotes.tool.notes-edit/textbox/arrow/imagebox` with the Task 4 spec table defaults, category "Graph editor tools")

**Interfaces (consumes):** Task 4 constants; the editor scope from Task 6 (tool ids already resolve via `selectTool`).

- [ ] **Step 1: Delete `getKeyBinding()`** from the tool interface and all 9 implementations.

- [ ] **Step 2: Re-key the editor's tool bookkeeping on chords.** In `JIPipeDesktopGraphEditorUI`: replace `toolToKeyBinding`/`keyBindingToToolsMap` and `initializeEditingToolbar`'s `registerKeyboardAction` blocks with maps keyed on the action's effective variants from the registry; tool cycle (`handleToolCycle`) now takes the variant (a `List<KeyStroke>`); repeated press of the same completed chord cycles tools sharing it, preserving the existing cycling semantics; toolbar toggle tooltips use `formatVariants`.

- [ ] **Step 3: Replace static editor strokes.** Delete `KEY_STROKE_UNDO/REDO/ZOOM_IN/ZOOM_OUT/ZOOM_RESET/MOVE_SELECTION_*` (`JIPipeDesktopGraphEditorUI.java:93-101`); the scope resolver from Task 6 serves these ids, and the editor's `registerKeyboardAction` calls are deleted (the dispatcher now owns all of these keys).

- [ ] **Step 4: Hold-Ctrl transient note editing** in `JIPipeDesktopGraphCanvasUI` mouse routing: on `mousePressed` with `e.isControlDown()`, the current tool is the default (tool manager `hasDefaultTool()`), and a hit test finds an annotation UI (`JIPipeDesktopGraphInteractiveObjectUI` on the annotation layer) as topmost target at the mouse position, delegate this press and the following drag/release triple to the `EditNotesGraphNodeTool` instance obtained from the editor's tool map (do not change the tool manager state, no toggle-button changes; a `mouseReleased` or Ctrl release ends the delegation). Node/background interactions are untouched (they are not annotation hits), preserving Ctrl+click multi-select.

- [ ] **Step 5: Register notes tool actions** in the canvasnotes plugin's `register()`.

- [ ] **Step 6: Build and full core test run** `mvn -pl jipipe-core test` (all existing tests must stay green).

- [ ] **Step 7: Commit** `feat(#1346): tools and editor strokes on the registry, hold-Ctrl note editing`

---

### Task 8: Menus and dock panel actions

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java` (4 menu items onto the registry; accelerator updates on settings change)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java` (register toggle action per panel; window-wide scope)
- Test: none new (registration/toggling is thin glue over Task 4/5 logic; user verifies in the app)

**Interfaces (consumes):** Task 4 `PROJECT_*` constants; Task 5 dispatcher/scope.

- [ ] **Step 1: Menu migration.** In `initializeProjectMenu` (and the file/save menu items), replace the four `setAccelerator(...)` calls: a small private helper `JMenuItem createMenuItem(String actionId, String text, Icon icon, String tooltip, Runnable action)` that creates the item, sets the accelerator from `bindingFor(actionId, current()).variants()` **only when the first variant has length 1** (JMenuItem accelerators cannot express chords; chord-assigned menu actions simply have no visible accelerator and fire through the dispatcher), and subscribes to `JIPipeShortcutSettings.binding()` to update accelerators live. Store the subscription and dispose it in the workbench's `dispose()`. Keep the items' `textInputSafe` semantics (already true from Task 4).

- [ ] **Step 2: Dock panel actions.** In `JIPipeDesktopDockPanel.addDockPanel(...)`: if `!JIPipeShortcutRegistry.getInstance().isRegistered("dock:" + id)`, register `new JIPipeShortcutAction("dock:" + id, name, "", "Dock panels", FAMILY_DOCK, true, List.of())` (no default binding). In `addNotify()` (first call): install a `ShortcutScope` with `root` = the enclosing window's root pane (`SwingUtilities.getRoot(this)`) whose resolver maps `"dock:<id>"` -> a runnable toggling the panel: `activatePanel(id, true)` when not currently visible on its side, else `deactivatePanel(id, true)`; register with the dispatcher. In `removeNotify()`: unregister the scope. Scopes are per dock-panel instance but resolvers only know their own panels; the dispatcher's innermost-first multi-scope resolution (Task 5) handles windows with several dock panels.

- [ ] **Step 3: Build** `mvn -pl jipipe-core compile`.

- [ ] **Step 4: Commit** `feat(#1346): menu and dock panel shortcuts on the registry`

---

### Task 9: Keyboard settings page, capture dialog, presets UI

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopKeyboardSection.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopKeyboardCaptureDialog.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopAppearanceSection.java` (`registerCompositions()`: host the `SECTION_KEYBOARD` page with the new section class instead of the empty state)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/AppearanceSectionModel.java` (drop/replace `KEYBOARD_EMPTY_STATE` usage for the real page)
- Test: none new (UI; user verifies in the running app). Conflict warning logic reuses Task 4's tested `findConflicts`.

**Interfaces (produces):**
- `JIPipeDesktopKeyboardCaptureDialog.capture(Component parent, String actionName, List<List<KeyStroke>> currentVariants) -> Optional<List<KeyStroke>>`: modal dialog built on `JIPipeDesktopDialog` (the #1341 Builder escape hatch); records pressed strokes on the dialog root (`KeyEventDispatcher` local to the dialog): a non-modifier first press displays as primary (`formatChord`), a subsequent non-modifier press while a primary is displayed forms the chord display `first then second`; Clear resets; OK commits the displayed chord (`Optional.of`), Cancel/dismiss returns empty. Escape dismisses the dialog (chrome behavior).
- `JIPipeDesktopKeyboardSection(JIPipeDesktopWorkbench workbench, JIPipeDesktopSettingsTab settingsTab)` mirroring `JIPipeDesktopAppearanceSection`'s structure (workbench access, `Disposable`, subscribes the keymap binding, re-created by the tab after reset):
  - Header: preset picker (`JIPipeDesktopSegmentedButtonGroup` or combo with `JIPipeShortcutProfiles.bundled()` labels; applying calls `JIPipeShortcutProfiles.apply` + writes through the binding), chord timeout spinner (2000 default, 0 allowed; writes `KeymapSettings.chordTimeoutMs`), "Reset section" (clears all overrides + restores timeout default)
  - Table: one row per registered action from `JIPipeShortcutRegistry.all()`, grouped by `category()` in spec order ("Graph editor tools", "Selection", "Creation", "Input/output", "Layers", "Running", "Navigation", "Editor", "Menus", "Dock panels"); columns Action / Binding / State / Edit / Reset; searchable `JIPipeDesktopSearchTextField` filtering by name; Binding cell renders `formatVariants(bindingFor(...).variants())`; State badge: `OVERRIDE` -> "Modified", `DEFAULT` -> "Default", `UNASSIGNED` -> "Unassigned"
  - Edit: opens the capture dialog; on result: `findConflicts` non-empty -> warning dialog naming the conflicting actions with Assign anyway / Cancel (`JIPipeDesktopDialogs`); then `settings.setOverride(id, List.of(captured))` through the binding
  - Per-row Reset: `settings.removeOverride(id)`
  - Table sizing derives from preferred sizes (no hardcoded text widths)

- [ ] **Step 1: Implement the capture dialog.**
- [ ] **Step 2: Implement the section and register the page** (replace the empty state).
- [ ] **Step 3: Build** `mvn -pl jipipe-core compile`.
- [ ] **Step 4: Commit** `feat(#1346): keyboard settings page with capture dialog and presets`

---

### Task 10: Cleanup, docs, full verification

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/TooltipUtils.java` (delete the old `createTooltipWithShortcut(String, String, KeyStroke)` overload if now unused)
- Modify: `AGENTS.md` (add a short "Keyboard shortcuts" subsection under Development Patterns: register actions with stable literal IDs via `JIPipeShortcutRegistry`, defaults in `JIPipeShortcutActions`/plugin classes, do not read `KeyStroke`s from actions)
- Remove: any now-unused `KeyStroke`/`InputEvent`/`KeyEvent` imports across touched files (compiler warnings guide this)

- [ ] **Step 1: Remove dead code** (old overload, leftover constants).
- [ ] **Step 2: Full build and test** `mvn clean compile` then `mvn -pl jipipe-core test` (all green).
- [ ] **Step 3: Update AGENTS.md** subsection (5-8 lines).
- [ ] **Step 4: Commit** `chore(#1346): cleanup and developer docs`
- [ ] **Step 5: Push and open MR** `git push -u origin 1346-keyboard-shortcut-overhaul`, MR title `Keyboard shortcut overhaul`, description referencing work item #1346 and summarizing: registry + chords, focus fix, dock shortcuts, revised defaults + Legacy preset, settings page. **Do not merge; the user verifies the UI in the running app first.**
