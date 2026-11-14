# Keyboard shortcut overhaul - Design

**Work item:** [#1346](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1346)
**Related:** #1345 (modern settings system, merged: this feature hosts its settings there), #1341 (modern dialog API, merged: the key capture dialog is built on it)
**Status:** approved design, 2026-11-13 (rev. 2)
**Breaking-change window:** 6.0.0 is in development; no stored state exists for shortcuts today, so there is no data migration. Muscle memory is covered by the bundled "Legacy" preset (Section 9).

## 1. Problem

Keyboard shortcuts are hardcoded in four disconnected places:

1. **Graph editor tools** (Move F1, Connect F2, Rewire F3, Crop view F12, canvas notes F4/F6/F7/ESC) via `JIPipeDesktopGraphEditorTool.getKeyBinding()`, registered `WHEN_IN_FOCUSED_WINDOW` per editor in `JIPipeDesktopGraphEditorUI.initializeEditingToolbar()`.
2. **Node context actions** (Ctrl+D duplicate, Ctrl+C/X/V, Delete, arrows, ...) via `GraphInteractiveObjectUIContextAction.getKeyboardShortcut()`, fired by a per-canvas global `KeyEventDispatcher` in `JIPipeDesktopGraphCanvasUI.initializeHotkeys()`.
3. **Static editor strokes** (undo/redo/zoom/arrow-move) as hardcoded `KeyStroke` constants in `JIPipeDesktopGraphEditorUI`.
4. **Workbench menu accelerators** (Ctrl+N/O/S, Ctrl+Shift+S) via `JMenuItem.setAccelerator` in `JIPipeDesktopProjectWorkbench`.

Confirmed defects:

- **Unreliable triggering (the user-reported bug).** The context-action dispatcher only fires when the canvas itself is the focus owner. After clicking a toolbar toggle button, a dock panel, or a non-text child of a node UI, the canvas loses focus and Ctrl+D silently does nothing until the canvas is clicked again.
- **Unusual defaults.** Bare F-keys for tools, numpad-only zoom (hostile to laptops), `L` for lock vs `Ctrl+L` for unlock (weaker action holds the stronger chord), `Ctrl+P` for disable pass-through (universally read as "print").
- **No customization.** Users cannot reassign anything; there are no profiles; dock panel entries cannot be triggered by keyboard at all.

## 2. Goals and non-goals

**Goals**

- One open **shortcut registry**: stable string-literal action IDs (project rule for registered IDs), in-code default bindings, user overrides persisted through the settings system (#1345).
- **Chord support** ("Microsoft-style multi-keybinds", e.g. `Ctrl+T` then `D`): a binding is a list of variant chords (`KeyStroke[]` each; length 1 = plain binding, length 2 = chord), and actions may carry alternative variants (Section 4).
- **Reliable triggering**: graph editor actions fire whenever the focus is anywhere inside the editor (except text inputs), not only when the canvas is focused.
- **Customizable shortcuts + profiles**: presets now ("Default", "Legacy (pre-6.0)"); the storage is designed so user profiles are a thin layer on the same override map (Section 9.4).
- **Dock panel shortcuts**: every dock panel entry can be activated/deactivated from the keyboard.
- **Revised defaults** for the unusual bindings (Section 8), with the Legacy preset preserving the old ones.

**Non-goals (this work item)**

- No image viewer / data viewer key families in the first release (they can register into the same registry later; their current hardcoded keys keep working untouched).
- No user-profile management UI yet (presets only; the storage shape is final, Section 9.4).
- No "quick assign" overlay yet (WoW-style: highlight assignable buttons, click one to assign; Section 11 records the concept and the architectural readiness).

## 3. Decisions log (agreed with the user, 2026-11-13)

| Decision | Choice |
|---|---|
| Scope of customizable actions | Everything, via an open registry plugins can contribute to |
| Chords | In scope ("Microsoft-style"); prefix can never be a standalone action |
| Architecture | Central registry + document overrides + one app-level dispatcher (Section 4) |
| Profiles | Presets now; named user profiles later on the same storage |
| Chord timing | Timeout 2000 ms default, `0` = wait indefinitely; Escape cancels; any non-matching key cancels; pending prefix shown in the status bar |
| Default keymap | Revised (Section 8) + bundled Legacy preset |
| Binding variants | A binding may carry alternative chords; defaults use this for QWERTZ (numpad zoom as second variant), Section 8.3 |
| Notes editing | Keep `Ctrl+T` then `E` and the toolbar button; additionally hold-Ctrl transient note editing on the canvas (Section 8.1) |
| Quick assign | Future work (Section 11) |

## 4. Architecture

New core package `org.hkijena.jipipe.api.shortcuts` (no Swing beyond `KeyStroke`; headless-safe), desktop pieces in `org.hkijena.jipipe.desktop.app.shortcuts`.

```
JIPipeShortcutAction            catalog entry: stable literal id, name, description,
                                category, default variants (KeyStroke[][], may hold
                                more than one alternative chord), scope family,
                                textInputSafe flag
JIPipeShortcutRegistry          id -> action catalog; duplicate-id registration fails
                                fast; plugins contribute via the plugin system
JIPipeShortcutBinding           resolved state: action id + effective variants
                                (KeyStroke[][]) + source (DEFAULT / OVERRIDE / UNASSIGNED)
JIPipeShortcutProfile           named frozen override map {actionId -> chord[] or null};
                                null (or absent) = revert to the registry default
JIPipeChordStateMachine         pure, testable: pending-prefix state, matching,
                                timeout, escape, mismatch reset
KeymapSettings                  persisted POJO: activeProfileId, overrides
                                {actionId -> chord[] or null}, chordTimeoutMs
JIPipeDesktopShortcutDispatcher the single JVM-wide KeyEventDispatcher (desktop)
ShortcutScope                   installed by host components (graph editor,
                                workbench, dock host): component subtree + resolver
                                answering "is action X available right now?" and
                                providing the Runnable for it
```

**Binding model.** One effective binding per action: a list of **variants**, each variant a `KeyStroke[]` chord (length 1 = plain binding, length 2 = chord); an empty list means unassigned. Registry defaults may carry more than one variant - for example on a German (QWERTZ) keyboard `+`/`-` are behind Shift, so the zoom actions carry the numpad strokes as a second variant (Section 8.3). User overrides (captured bindings) replace the variant list with the captured chord. Dispatch matches any variant of the binding; a chord prefix can never also be a standalone action, so there is no "fire after timeout" ambiguity: the prefix key alone fires nothing.

Resolution order: user override (from `KeymapSettings.overrides`) > registry default (including its variants). Presets and (later) user profiles apply one-way into the override map (Section 9.3); they are not a separate live resolution source.

**Action ID scheme.** Stable string literals, prefix `org.hkijena.jipipe.` for core, plugin prefix for plugins. Families: `graph.tool.*`, `graph.node.*`, `graph.compartment.*`, `graph.edge.*`, `graph.*` (editor-level), `project.*` (menus), `dock:<panel-id>` (dock panels, dynamic).

**Flavor sharing.** Pipeline and compartment editors have their own copy/cut/paste implementations with the same semantics. They share one action ID (e.g. `graph.node.copy`); the *scope* resolves which implementation runs in the active editor. The settings table therefore shows one row per semantic action, not per editor flavor.

## 5. Storage in the settings document

One key in the **application** scope:

```java
JIPipeSettingKey.object("registry:keymap", "", KeymapSettings.class)
```

The whole `registry:keymap` block holds the `KeymapSettings` POJO (same pattern as the recent-projects block). Only overrides and configuration are stored; defaults live in code. This keeps the document minimal, honors the settings system's "the document contains user changes, not a snapshot" rule, and unknown action IDs inside `overrides` pass through untouched across versions (the document's pass-through semantics).

Changes go through the standard settings engine: live binding, debounced save, cross-process file watcher. The dispatcher and the menus subscribe to the binding, so edits apply live without a restart.

## 6. Dispatch (also fixes the focus bug)

One `JIPipeDesktopShortcutDispatcher` is installed once at desktop startup on the JVM-wide `KeyboardFocusManager`. The per-canvas dispatcher is deleted. For every `KEY_PRESSED` event:

1. **Normalize** to a `KeyStroke` (`KeyStroke.getKeyStrokeForEvent`).
2. **Resolve the scope**: the innermost installed `ShortcutScope` whose component subtree contains the focus owner, within the focused window. No scope, or a modal dialog owns the focus: pass through (dialog scopes silence underlying windows automatically).
3. **Chord state machine** (pure core class): if a prefix is pending, the event is consumed; a matching second stroke resolves the action, a mismatch / timeout / Escape cancels. A chord prefix can never also be a standalone action, so there is no "fire after timeout" ambiguity: the prefix key alone fires nothing. While a prefix is pending, the status bar shows a hint (e.g. `(Ctrl+T) was pressed, waiting for second key of chord`); a resolved or cancelled chord clears it.
4. **Availability**: ask the scope's resolver (e.g. graph editors evaluate `matches(currentSelection)` of the context action live; dock actions are always available; menu actions depend on window state such as "project is open"). Availability is never cached.
5. **Text-input guard**: if the focus owner is an editable text component (`JTextComponent`, spinner/combo editors) and the action is not flagged `textInputSafe`, the event passes through untouched. Graph-selection actions are `textInputSafe = false` (Ctrl+C in a node name field copies text, not nodes); window-level actions (menus, dock toggles) are `true`.
6. **Fire** on the EDT via `invokeLater`, consume the event, and send the existing "Executed: ..." status bar text.

Prefix keystrokes themselves are **not consumed** (plain `Ctrl` down does nothing anywhere, so passing it through is harmless). This allows the canvas to observe raw Ctrl press/release for the transient notes mode (Section 8.1) without fighting the dispatcher.

**Why this fixes the bug:** the trigger area for graph editor actions becomes "any non-text focus owner inside the editor window" instead of "the canvas is the focus owner". Ctrl+D works after clicking a tool toggle, a dock panel, or a node's non-text child.

## 7. Consumer migrations

| Consumer | Change |
|---|---|
| Context actions | `getKeyboardShortcut()` deleted; canvas hotkey dispatcher deleted; actions get `getShortcutActionId()` (stable literal); tooltips render via `JIPipeShortcutRegistry.formatChord()` |
| Editor tools | `getKeyBinding()` deleted; editor installs a scope with its tool actions; cycling on repeated press of a shared chord is kept |
| Static editor strokes | `KEY_STROKE_*` constants replaced by registry actions (`graph.undo`, `graph.zoom-in`, ...) |
| Menus | The four workbench accelerators become registry actions (`project.new`, `project.open`, `project.save`, `project.save-as`); `setAccelerator` is driven by the binding and updated live; plugin menu extensions are untouched (their accelerators keep working; they can register actions later) |
| Dock panels | `JIPipeDesktopDockPanel` registers one toggle action per panel, id `dock:<panel-id>`, name = panel name, default = unassigned; the action toggles `activatePanel`/`deactivatePanel`. Registrations happen at panel-add time; assignments persist by ID and re-apply when the panel appears in any future session |

## 8. Revised default keymap

### 8.1 Graph editor tools (the family the work item calls out)

| Action | Old | New default |
|---|---|---|
| Default tool / cancel | ESC | ESC (kept) |
| Move nodes | F1 | `Ctrl+T` then `M` |
| Connect | F2 | `Ctrl+T` then `C` |
| Rewire | F3 | `Ctrl+T` then `R` |
| Crop view | F12 | `Ctrl+T` then `V` |
| Notes: Text box | F4 | `Ctrl+T` then `T` |
| Notes: Arrow | F6 | `Ctrl+T` then `A` |
| Notes: Image box | F7 | `Ctrl+T` then `I` |
| Notes: Edit | ESC (cycled with default tool) | `Ctrl+T` then `E` |

**Hold-Ctrl transient note editing (verified feasible).** With the default tool active, holding `Ctrl` and pressing/dragging on a **canvas note** routes that interaction (press/drag/release triple) to the `EditNotesGraphNodeTool` handlers; releasing Ctrl restores normal behavior. Implementation: target-based delegation in the canvas mouse routing (hit-test: Ctrl held and topmost hit is an annotation UI, default tool active), no selection-mask changes.

- No conflict: `Ctrl` on the canvas is currently used only for wheel zoom (mouse wheel events, unaffected); `Ctrl+click` on nodes keeps multi-select because only annotation hits are delegated.
- The persistent tool (button, `Ctrl+T` then `E`) stays for discoverability; hold-Ctrl is the quick path.

### 8.2 Context actions

Kept as-is: Ctrl+C/X/V (copy/cut/paste; copy edge shares Ctrl+C via disjoint availability), Ctrl+D, Ctrl+Shift+D, Ctrl+A, Ctrl+G, Delete (delete node/edges; delete edge control point shares it), Shift+Delete (clear control points), Ctrl+Enter (run and show results), Ctrl+Space (update cache), Ctrl+E / Ctrl+Shift+E (enable/disable node), Ctrl+arrows (move node UI), PageUp / PageDown and Ctrl+variants (layers), Ctrl+Shift+I (isolate), Shift+A (add node / add compartment, per editor), Ctrl+Shift+A (add parameter set node).

Revised:

| Action | Old | New default |
|---|---|---|
| Lock node location/size | `L` | `Ctrl+L` |
| Unlock node location/size | `Ctrl+L` | `Ctrl+Shift+L` |
| Disable pass-through | `Ctrl+P` | `Ctrl+Shift+P` |
| Enable pass-through | `Ctrl+Shift+P` | `Ctrl+Alt+P` |

### 8.3 Editor-level and menus

Zoom in/out/reset get a **second variant** on the numpad: the main variants `Ctrl+=` / `Ctrl+-` / `Ctrl+0` fail on QWERTZ layouts because `+` and `-` sit behind Shift, so the numpad strokes are registered as additional variants of the same action (Section 4, binding model). Undo `Ctrl+Z`, redo `Ctrl+Shift+Z`, arrow-key selection movement: kept. Menus: Ctrl+N/O/S and Ctrl+Shift+S kept.

**Conflict audit of the default map:** no standalone action uses `Ctrl+T`; ESC is graph-scope only (dialogs own their ESC while modal); same-key pairs (copy node/copy edge, delete node/delete control point) stay disjoint by selection availability, as today. Same-key-different-editor pairs (`graph.node.add` vs `graph.compartment.add`) are disjoint by editor scope.

## 9. Keyboard settings section (fills the existing placeholder)

The settings tab already reserves the section: `JIPipeDesktopKeyboardApplicationSettings` (sheet id `org.hkijena.jipipe:desktop-keyboard`, seed-exempt) hosts the merged Keyboard page; this work replaces `KEYBOARD_EMPTY_STATE` with the real page.

### 9.1 Page

- **Header:** profile strip (preset picker, Section 9.3) + chord timeout setting (2000 ms default, 0 = wait indefinitely) + per-section Reset (removes all overrides).
- **Action table:** searchable, grouped by category ("Graph editor tools", "Selection", "Creation", "Input/output", "Layers", "Running", "Navigation", "Editor", "Menus", "Dock panels"). Columns: Action, Binding (`formatChord`: `Ctrl+T then M`; actions with variants show all of them, `Ctrl+= or Ctrl+Num+`), state badge (Modified / Default / Unassigned), Edit, Reset-to-default.

### 9.2 Capture dialog and conflicts

- **Capture dialog:** built on `JIPipeDesktopDialogs.Builder` (#1341). Shows the current binding (all variants); the recorder displays live chord progression (`Ctrl+T` pending, `... then M` completed), Clear, OK/Cancel. Modifiers-only presses never complete a chord. Accepting a capture **replaces** the binding's variants with the single captured chord; Reset-to-default restores the registry's multi-variant default.
- **Conflicts on OK:** a variant is conflicting if it equals any variant of another action's binding in the same scope family; the warning names the conflicting action(s) with *Assign anyway* (selection availability usually disambiguates) or Cancel. The bundled default keymap is conflict-free by construction.
- Graph editor tooltips switch to `formatChord()` so tooltips, menus, and the settings table can never disagree.

### 9.3 Presets

Presets apply **one-way** into the user's override map; afterwards everything is editable, and per-action Reset returns to the registry default.

- **Default**: clears all overrides (the registry default keymap of Section 8; shown for clarity and as the reset target).
- **Legacy (pre-6.0)**: an override map restoring every pre-revision binding (F1/F2/F3/F4/F6/F7/F12 tools, ESC-cycled notes edit, numpad zoom, `L` lock, `Ctrl+L` unlock, `Ctrl+P`/`Ctrl+Shift+P` pass-through).

Applying either preset is idempotent.

### 9.4 Profiles (storage final, UI later)

User profiles are `JIPipeShortcutProfile`s persisted in a future `state:keymap-profiles` block (name -> override map) plus `activeProfileId` in `KeymapSettings`. Phase 1 ships only the two bundled presets; the profile management UI (create/rename/switch/import/export) is a later work item that needs no further model changes.

## 10. Testing (project test policy: non-trivial logic only, no Swing interaction tests)

- **Chord state machine** (pure): prefix pending, second-stroke match, mismatch reset, Escape, timeout expiry, `0` = no timeout.
- **Registry:** stable-literal IDs, duplicate-ID rejection, conflict detection across variants, `formatChord` rendering (including multi-variant display).
- **Variant matching:** dispatch resolves to the action for any variant of its binding (QWERTZ numpad zoom case), and does not match when the variant list is empty (unassigned).
- **Persistence:** `KeymapSettings` JSON round-trip; overrides for unknown/removed action IDs preserved verbatim; `null` override means revert-to-default; profile block round-trip.
- **Presets:** Default/Legacy produce the exact expected effective bindings; apply/reset idempotent.
- **Text-input guard:** the suppression predicate as a pure unit test.
- UI behavior (capture dialog ergonomics, live table updates) is verified by the user in the running app.

## 11. Future work (recorded, not built)

- **User profiles UI** (Section 9.4): create/rename/switch/import/export named profiles.
- **Quick assign overlay** (WoW-style, requested 2026-11-13): a mode that highlights every on-screen element bound to a registry action; clicking one opens the capture dialog prefilled with that action; Escape exits. Architectural readiness kept in Phase 1: every action-bound UI element (toolbar buttons, menu items) is created through the registry-bound helpers so it carries its action ID, which is exactly the enumeration the overlay needs. Image viewer / data viewer key families can join the registry the same way.

## 12. Rollout phases (input for the implementation plan)

1. Core model + registry + settings key (+ tests)
2. Dispatcher + scopes + graph editor migration (context actions, tools, static strokes, transient notes mode) + focus fix + tooltip formatting
3. Menus + dock panel actions
4. Keyboard settings section + capture dialog + presets
5. Dead-code removal (per-canvas dispatcher, old shortcut plumbing) + branch `1346-keyboard-shortcut-overhaul`, MR referencing the work item
