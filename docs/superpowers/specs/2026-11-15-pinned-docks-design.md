# Pinned Docks - Design

**Work item:** [#1367](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1367)
**Related:** #1353 (context dock activity state is not persisted - the same persistence hygiene applies here), #1346 (dock toggle shortcuts - contract in section 8)
**Status:** approved design, 2026-11-15

## 1. Problem

Docks such as Parameters or Results are context-sensitive: they exist only while a
node is selected. When the user deselects the node and nothing else is displayed on
that side, the dock area collapses and the canvas grows. Selecting a node again
covers the freed-up space, which is disorienting (screenshot in the work item).

The Tools dock was designed to always fill the right side so this could not happen,
but the behavior was lost during the settings-system migration and the following
dock fixes. The current rebuild is too weak:

- `alwaysShowLeftPanel` / `alwaysShowRightPanel` are per-side booleans. The user
  cannot choose which dock stands in for the empty side.
- The stand-in dock is code-chosen: `JIPipeDesktopGraphEditorUI` hardcodes
  `setFallbackRight(DOCK_CONTEXT_PANEL)` / `setFallbackLeft(DOCK_CONTEXT_PANEL)`.
- The bottom side has no stand-in at all.
- The mechanism is not discoverable: it lives in the toolbar context menu
  ("Always show right panel"), not in the dock's own menu.
- The left fallback path is broken: `updateContent` adds the left fallback panel
  into the right content (`JIPipeDesktopDockPanel.java`, the
  `leftContent.isEmpty() && alwaysShowLeftPanel` branch).

## 2. Goals and non-goals

**Goals**

- The user can pin a dock to its side (LEFT, RIGHT, BOTTOM) through the dock
  toggle's right-click menu. One dock is pinned per side.
- A pinned dock automatically displays as a stand-in whenever its side would
  otherwise show nothing. The stand-in activation is dock-side (the dock panel
  owns it) and transient (never persisted).
- The Tools dock is pinned on the right by default; existing persisted layouts
  migrate so users keep the behavior they have today.
- The pin is visible: underlined dock name when toolbar labels are shown, a border
  around the icon when they are not.
- Moving a dock to another anchor clears its pin (it must be re-applied).

**Non-goals**

- Multiple pinned docks per side, pin priority or ordering.
- Drag-and-drop re-anchoring or floating panels (the move menu stays as it is).
- Persisting the stand-in activation itself (deliberately transient, section 5).
- New dock locations or changes to the context-dock lifecycle.

## 3. Concepts

| Concept | Meaning |
|---|---|
| Pin side | `Left` (`TopLeft`, `BottomLeft`), `Right` (`TopRight`, `BottomRight`), `Bottom` (`BottomBottom`) |
| Pin | At most one dock ID per pin side, part of the persisted `State` |
| Stand-in | Transient display of the side's pinned dock when no dock on that side is displayed; never flips the dock's `visible` flag |
| Stand-in pause | Runtime-only, per side: after the user explicitly closes the side's pinned dock while it is the only displayed dock on that side, the side stays collapsed until the dock set or activity on that side changes |

`PinSide` is a new enum in `JIPipeDesktopDockPanel`. The existing `PanelSide`
(Left/Right) keeps its role as the payload of `PanelSideVisibilityChangedEvent`
(layer visibility); the bottom area has no collapsible layer, so no `Bottom`
events are emitted. `PinSide` is only about pins.

## 4. Dock panel model and API

`JIPipeDesktopDockPanel.State` gains a persisted field:

- JSON key `"pins"`: a map from the lowercase pin side name (`"left"`, `"right"`,
  `"bottom"`) to a dock ID. An absent key means "no pin on that side". Unknown
  side names round-trip but are ignored on apply.

`JIPipeDesktopDockPanel` gains:

- `pinDock(String id)`: pins the dock at its current location's side. Pinning
  replaces that side's previous pin (the one-per-side invariant is enforced here
  and is unrepresentable in the data model, which stores one ID per side).
  Pinning a context dock (ID starts with `_`) is a no-op. Pinning an already
  pinned dock is idempotent.
- `unpinDock(String id)`, `isDockPinned(String id)`, `getPinnedDockId(PinSide)`.
- Runtime state: the pin map (`Map<PinSide, String>`) and the stand-in pause set
  (`Set<PinSide>`). Only the pin map is persisted.

Pins are applied defensively: a pin whose ID is not currently registered, or that
names a context dock, is inert (displays nothing, stays in `State`). This mirrors
the stale-panel-id tolerance the persisted state already has
(`DockStateStalePanelIdsTest`).

## 5. Display pipeline

`updateAll` computes a display plan first, then renders from it:

1. Per side: the docks displayed by the user (the `visible` flag), as today.
2. Otherwise, if `hideToolbars` is false and the side is not paused: the side's
   pinned dock (when registered) becomes the stand-in for that side.

- `updateToolbars` renders every toggle from the plan: selected means "on screen"
  (user-opened or stand-in). The pin indicator is applied here (section 7).
- `updateContent` composes the components from the plan. A stand-in instantiates
  a lazily-supplied component exactly like an activation does (a pinned dock with
  a component supplier gets its component created on first stand-in).
- The stand-in check sits inside the same guard that collects the regular
  content: `hideToolbars` collapses the whole dock area (the data viewer uses it
  when it has no content), so no stand-in is displayed while it is set. The old
  fallback block sat outside this guard; that block, including the
  left-into-right bug, is deleted together with `alwaysShowLeftPanel`,
  `alwaysShowRightPanel`, `fallbackLeft` and `fallbackRight`.
- `PanelSideVisibilityChangedEvent` semantics are unchanged: a side layer that
  becomes visible or hidden because of a stand-in fires the same events as one
  that becomes visible because of a user activation (the editor's scrollbar
  compensation keeps working).

**Stand-in pause lifecycle**

- Set when: `deactivatePanel` runs on the side's pinned dock while it is the only
  displayed dock on that side (toggle click or dock shortcut - both are explicit
  user closes). This covers both the stand-in-displayed case and the case where
  the user opened the pinned dock themselves; without the latter, closing a
  user-opened pinned dock would instantly re-show it as a stand-in and the close
  would appear to do nothing.
- Cleared when: the dock set or activity on that side changes - `addDockPanel`
  or panel removal touching the side, any `activatePanel` on the side, a pin
  change on the side, `movePanelToLocation` touching the side, `restoreState`.
- Not set when: the pinned dock is merely displaced by another activation
  (context docks appearing, another dock on the side opened). That is not a
  "the user wants this side empty" signal.
- Rationale: on node deselection the editors run
  `removeDockPanelsIf(id -> id.startsWith("_"))`, which touches every side and
  therefore clears the pause - the pinned dock stands in again, which is exactly
  the scenario the work item describes.

**Moving clears the pin**

`movePanelToLocation` unpins the moved dock. Any move clears the pin, including a
location change within the same side (the work item states the pin is lost and
must be re-applied; re-pinning is one right-click away from the move entries).

## 6. Persistence and migration

- `getCurrentState()` writes `pins`. It never writes stand-in or pause state and
  never writes the legacy `always-show-*` keys again.
- `withoutContextDockVisibilities()` keeps pins (a pinned dock is never a context
  dock, so pins are persistent-dock state by construction).
- The legacy `State` fields stay deserializable (their setters are kept) so old
  documents load, but they are no longer serialized.
- Migration is a pure, idempotent step on `State`, `State.migrated()`:

  | Stored state | Result |
  |---|---|
  | `always-show-right-panel-v2` is `true` or absent, no `right` pin | pin `right` to `CONTEXT_PANEL` (the stable literal ID of the Tools dock) |
  | `always-show-right-panel-v2` is explicitly `false` | no pin added |
  | `always-show-left-panel` is `true` | no pin added (the legacy left fallback was hardcoded to a right-side dock and never worked; there is no behavior to preserve) |
  | `pins` already present | untouched |

  The flag is deserialized into a nullable `Boolean`, so an absent key is
  distinguishable from an explicit `false`: users predating the flag, and
  states round-tripped through an older build (which drops the key and would
  otherwise lose the pin permanently), are pinned like everyone else; only a
  real uncheck of the old checkbox keeps the side unpinned.
- Migration is applied where persisted state enters an editor:
  `applyPipelineDockState` and `applyCompartmentsDockState` migrate before the
  JSON equality check, so the echo suppression compares like-for-like and does
  not re-apply in a loop, and the graph editors' `restoreDockStateTemplate`
  migrates before restoring a layout template. `restoreState()` itself stays
  generic: the run managers call it with in-memory snapshots that already
  carry pins.
- `SettingsKeys.defaultDockState()` pins `right` to
  `JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL` instead of setting the
  always-show flag. Fresh installs therefore show the Tools dock on first launch,
  before any selection.
- Users who had explicitly unchecked "Always show right panel" keep that
  preference: no pin, side collapses when empty.

## 7. UI: menu entry and pin indicator

- The right-click menu of a dock toggle gains a checkbox item "Pin dock"
  (tooltip: "Show this dock when its side would be empty"). Its state is
  `isDockPinned(id)`; selecting it pins (replacing that side's pin), deselecting
  unpins. The entry is hidden for context docks (they are not pinnable).
- Unpinning a dock that is displayed as a stand-in simply lets the side collapse;
  no special handling.
- Pin indicator, rendered while the dock is pinned (regardless of how it is
  displayed):
  - Toolbar labels shown: the toggle's label is underlined. The font is derived
    with `TextAttribute.UNDERLINE_ON`; nothing is measured or hardcoded.
  - Icon-only: a thin line border in the theme's icon base color replaces the
    empty border around the toggle.
- Removed UI: the "Always show right panel" / "Always show left panel" entries in
  the toolbar context menu. The menu keeps "Show Toolbar Labels".

## 8. Integration

- `JIPipeDesktopGraphEditorUI` drops its `setFallbackRight` / `setFallbackLeft`
  calls. The Tools dock registration is unchanged (code default `visible=false`;
  the default pin is what shows it).
- Both editor flavors: apply-path migration (section 6); the context-dock
  add/remove flow is untouched.
- All other dock hosts (data viewer, plot editor, project overview, welcome
  panel, legacy image viewer, run-single-algorithm IO editor) get pin support
  through the generic panel; none of them gets a default pin. Dock layout
  templates (a graph-editor feature) carry pins through their existing JSON
  round-trip; templates stored by older versions are migrated on restore.
- #1346 (keyboard shortcut overhaul, branch `1346-keyboard-shortcut-overhaul`,
  not yet merged): its `toggleDockPanel` currently decides on
  `panel.isVisible()`. With pins the contract is: toggle on **displayed** state
  (on screen, stand-in included), so a dock shortcut behaves exactly like the
  toggle button - closing the side's pinned dock while it is the only displayed
  dock on that side sets the pause. The side that lands second adapts; both
  branches touch `JIPipeDesktopDockPanel.java`, so expect a rebase either way.

## 9. Error handling and edge cases

- Pin references an unknown dock ID: inert, kept in `State` (self-heals if the
  dock is registered later).
- Pin names a context dock ID (only possible via a hand-edited document): inert.
- The pinned dock is removed while displayed as a stand-in (`removeAllPanels` and
  friends): the pin stays, is inert, and the side collapses.
- Two editors of the same kind share the setting through the existing settings
  binding; pins need no synchronization beyond the live re-apply that already
  exists.
- Absent or malformed `pins` in a document: treated as empty.
- The display plan is computed once per update, so there is no window in which a
  side shows both a user dock and a stand-in.

## 10. Testing

Headless unit tests, following `JIPipeDesktopDockPanelStateTest` (which
constructs real panels); per the project test policy, no tests for Swing
structure - the underline/border rendering is verified in the running app.

- Pin API: pinning replaces the side's previous pin; one pin per side; pinning a
  context dock is a no-op.
- Stand-in: fills an empty side; yields as soon as any dock on the side is
  user-opened; does not change `getCurrentState()`.
- Pause: closing the side's pinned dock while it is the only displayed dock on
  that side (stand-in or user-opened) collapses the side; a context-dock
  add/remove cycle re-arms it; a pin change re-arms it; closing a non-pinned dock
  does not pause the side.
- Move clears the pin.
- Migration: legacy flag `true` or absent pins the Tools dock; an explicit
  `false` adds nothing; existing pins are untouched; migration is idempotent.
- Persistence: `pins` JSON round-trip; `withoutContextDockVisibilities()` keeps
  pins; the legacy keys are not written anymore.
- Inert pins: unknown IDs and context dock IDs.
- `SettingsKeysTest`: the default dock state pins `CONTEXT_PANEL` on the right
  and sets no legacy flag (replaces the current always-show assertion).

## 11. Files

| File | Change |
|---|---|
| `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java` | Core of the feature: `PinSide`, pins in `State` (+ `migrated()`), pin API, stand-in and pause in the display plan, pin menu entry, pin indicator, removal of the always-show/fallback mechanism |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` | Remove the fallback wiring and the constructor's always-show default; migrate legacy layout templates on restore |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` | Apply-path migration before the state equality check; the flavor's `restoreDefaultDockState` pins the Tools dock |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` | Same as the pipeline flavor |
| `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java` | `defaultDockState()` pins the Tools dock on the right |
| `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelStateTest.java` | Extended with pin and migration tests |
| `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/SettingsKeysTest.java` | Updated default-state assertion |
| `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java` (new) | Pin behavior tests that do not belong into the state test |

Cleanup included because the name now actively misleads: `UI_ORDER_PINNED` (-100,
actually a "listed first" toolbar order) is renamed to `UI_ORDER_FIRST` with its
two usages (the Add-nodes dock registration in both editor flavors).

Branch: `1367-pinned-docks`, targeting `master`.
