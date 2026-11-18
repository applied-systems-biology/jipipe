# Spec: #1379 sub-project 3 - structured UI operations and synthetic input

Date: 2026-11-18
Status: DRAFT for user review
Work item: #1379 (Agentic interactions through MCP). Depends on: MCP v1 (#1369, MR !198), sub-projects 1 (project lifecycle) and 2 (viewer agent control) on branch `1379-agentic-interactions`.

## 1. Intent

AI agents driving JIPipe must be able to operate the user interface: trigger commands, open and close panels, configure parameters the generic `set_parameter` cannot express, and - for custom-painted surfaces such as the 3D viewers and interactive annotation tools planned for the Interactive JIPipe milestone - deliver real input events.

Work item #1369 listed this capability as "browse-style UI interaction (clicking controls, typing text, opening menus)". Exploration (see `.superpowers/sdd/2026-11-17-wi1379/explore-ui-toolkit.md`) found that literal mouse emulation is needed for almost nothing in the current UI: a fire-by-id primitive exists, docks and node UIs carry stable identifiers, and the "typing" gap is a value-conversion gap, not a UI gap. This spec therefore reshapes the capability into two layers, mirroring the proven design of the AI browser-control extensions (semantic element refs as the primary addressing, `elementsFromPoint`-style probes, coordinates only for canvas/custom-painted surfaces, probe-act-confirm loop, delivery-is-not-effect):

- **Layer 1 - structured UI operations** (no synthetic events): deterministic, reviewable operations on the three addressing axes that already exist (shortcut-action ids, dock panel ids, node UUIDs and parameter keys).
- **Layer 2 - synthetic input** (the escape hatch): coordinate-based input for custom-painted surfaces that have no element identity, restricted to JIPipe's own windows.

The user confirmed this reshaping satisfies the work item's intent (2026-11-18): structured operations cover most needs and the missing action ids are to be added; real input capability is required for the coming Interactive JIPipe milestone; there are no third-party plugins (JIPipe is monolithic user-side), so every component is held to the same standards.

## 2. Confirmed decisions

1. **Both layers** (user, Q1): element/structured-addressed operations as the primary API; screen-coordinate input as the escape hatch for custom-painted surfaces.
2. **JIPipe windows only** (user, Q2): coordinate input is accepted only when the target point falls inside a showing window of the JIPipe JVM. Clicks outside are refused. Native/OS dialogs are not JVM windows and are therefore unreachable by design.
3. **dispatchEvent default, Robot opt-in** (user, Q3): synthetic input is delivered as AWT events on the EDT by default (`input-mode: "dispatch"`); `input-mode: "system"` (java.awt.Robot, off the EDT, bounded) is available per call for surfaces that need physical input. The operation reports which mode ran.
4. **No committed CI tests exercise synthetic input** (user note): CI runs headless; per project policy such tests would not be in the finalized branch. Pure-logic helpers (bounds validation, resolution tables, conversion encodings) may have unit tests. The input layer is verified live.

## 3. Layer 1: structured UI operations

### 3.1 Action firing

- `fire_action` (Modify, Ask): fires a registered shortcut or context action by id, using the existing `JIPipeDesktopShortcutDispatcher.resolveAction(actionId, focusOwner)` (`desktop/app/shortcuts/JIPipeDesktopShortcutDispatcher.java:332-348`), which returns the exact `Runnable` the keyboard path fires, text-input guards included. Params: `action-id` (required), optional `project` (scope resolution: bound window > focused > first, the `get_ui_state` chain). The action is resolved in the TARGET EDITOR's dispatcher scope (the component hierarchy of the resolved project window's editor), never against the user's current keyboard focus, so the agent cannot accidentally fire an action aimed at whatever the user is doing. Unknown id or no resolving scope: a clear error; discovery is `list_ui_actions`' job.
- `list_ui_actions` (Query, read-only): returns the fireable action ids and human labels for the target editor (optional `project`). Built from the shortcut registry (`JIPipeShortcutRegistry.all()`) filtered by whether the target editor's dispatcher scope can resolve each id, so the list is exactly what `fire_action` can fire and the agent discovers instead of guessing. (Context actions are dispatcher-resolvable once they carry ids, so they appear in this list.)

### 3.2 Missing action ids

Fourteen context actions currently have no stable id (the exploration's estimate of nine was a subset; the implementer enumerated authoritatively via `getContextActions()`). The implementation assigns stable string-literal ids to every context action lacking one (14 in total, the `NodeContextActionWrapperUIContextAction` wrapper excluded), following the existing id conventions in `JIPipeShortcutActions` / the graph editor scope, via the existing `registerContextAction` path (`:635-643`): enable/disable save outputs, enable/disable virtual outputs, run and show intermediate results, invert selection, add edge control point, group, collapse IO interface, export compartment as JSON, open cache browser, clear cache, add bookmark, remove bookmark. Ids follow the existing kebab-case action-id style and are registered idempotently (skip-if-present). Settings-table categories follow the consolidation ruling (2026-11-18): io actions under `Input/output`, the cache family under one `Cache` bucket, `Running` for run actions, `Node` with an explicit `CATEGORY_ORDER` slot.

Main-menu items remain out of scope (section 8): they are not shortcut actions, and no current case needs them.

### 3.3 Dock panel control

- `set_dock_visible` (Query class - view state only, like `navigate_to_node`): params `dock-id` (required, the editor's existing string id such as `FIND`, `ADD_NODES`, `ERRORS`, `_PARAMETERS`), optional `project`, optional `visible` (default true). Uses `activatePanel` / `deactivatePanel` (`JIPipeDesktopPipelineGraphEditorUI.java:964-971`); the dock-id sets of the pipeline editor, compartments editor, and project overview are each addressable when that editor type is the resolved target.
- `list_docks` (Query, read-only): dock ids, titles, and visible state for the target editor.

### 3.4 Composite parameter conversion (no new operation)

`set_parameter`'s converter (`JIPipeAutomationAPI.convertParameterValue`, `:429-477`) handles String, primitives, Enum, and Path; every other leaf type silently receives a raw String. UI editing writes through the identical `access.set`, so widening the converter closes the "typing" gap without any UI machinery.

The implementation adds explicit conversions for exactly these composite leaf types (encodings verified against each parameter class during implementation; the classes are the authority):

| Type | Encoding (proposed) |
|---|---|
| Expression parameters (`JIPipeExpressionParameter` and subclasses) | the expression string |
| `Color` | `#RRGGBB` or `#RRGGBBAA` hex (alpha LAST - JIPipe's own `ColorUtils` convention; ruling 2026-11-18, the classes are the authority) |
| `Quantity`-style value+unit parameters | `"<value> <unit>"`, unit optional when the parameter has a default unit |
| Dynamic enum parameters (`JIPipeDynamicEnumParameter` family) | the registered item string |

Rules: conversions are additive; the raw-String fallthrough remains for anything unsupported so existing behavior never changes; a conversion failure produces a clear error naming the type and the accepted encoding, never a silent mis-set. List/set parameters whose items are primitives are already reachable through the dotted parameter-tree keys and are not touched. Matrix and image parameters stay out of scope (section 8).

## 4. Layer 2: synthetic input

### 4.1 Operations

Four input operations plus a probe, all Modify (Ask) except the probe (read-only), all `requiresEdt`-bridged like the existing desktop-touching ops:

- `ui_probe` (Query, read-only): the virtual cursor's peek, the direct equivalent of the browser extensions' `elementsFromPoint`. Params: `x`, `y` (screen coordinates, required). Returns, WITHOUT delivering any event: the window (title), and what is under the point, resolved with the same hit-testing the real mouse events use - `Component.getComponentAt(Point)` for Swing components (with the component chain's class names, and its accessible name/label where available), plus the graph-canvas-specific pickers when the point is over a canvas (`pickNodeUI` for the node UUID, `getSlotActiveArea` for the slot name).
  - **Cross-reference to Layer 1 (the core "aim right" rule).** When the probed point resolves to an element that HAS a Layer 1 identity - a node, a node slot/badge, a dock panel, a fireable action - the probe says so and names the better operation, e.g. "point is the run badge of node `<uuid>`; use `run_node` with the node id rather than `ui_click`" (a dock panel names `set_dock_visible` with its dock id). The agent is thereby steered to the deterministic semantic path; `ui_click` at raw coordinates remains only for surfaces with no identity (3D viewers, annotation canvases). This mirrors the extensions' rule "use the ref/semantic target; use coordinates only for canvas/custom-painted surfaces."
  - The probe is pure read: it delivers nothing and takes no input-mode.
- `ui_click`: params `x`, `y` (screen coordinates, required), optional `click-count` (default 1; 2 = double), optional `button` (default left), optional `input-mode`.
- `ui_drag`: params `from-x`, `from-y`, `to-x`, `to-y` (required), optional `button`, optional `input-mode`.
- `ui_scroll`: params `x`, `y`, `amount` (signed; required), optional `input-mode`.
- `ui_type_text`: params `text` (required), optional `input-mode`. Delivers the text as key events to the current keyboard-focus owner; the intended sequence is `ui_click` into a field (which transfers focus), then `ui_type_text`. Refuses when no keyboard-focus owner exists inside a showing JIPipe JVM window. No separate element-id API exists in this layer by design: everything with identity belongs to Layer 1, and custom-painted surfaces are addressed by coordinates.

Every input operation hit-tests its target FIRST (same resolution as `ui_probe`) and the response echoes the resolved element (window title, component class chain, node UUID/slot when over a canvas, the `input-mode` that ran) - so every click is self-describing and the agent never acts blind. For refusals the response lists the allowed rectangles.

### 4.2 Target validation (JIPipe windows only)

Before any event is delivered, the target must be inside the JIPipe JVM: for point-based operations (`ui_click`, `ui_drag`, `ui_scroll`), the point must fall inside the bounds of a showing `java.awt.Window` of the running JVM (project windows, data viewers, dialogs owned by them, the AI monitor); for `ui_type_text`, the keyboard-focus owner must be a component inside such a window. Implementation: iterate `Window.getWindows()`, filter `isShowing()`, test containment. Refusal response lists the allowed rectangles (or the focus-owner state) so the agent can correct itself. Native/OS dialogs (for example the GTK file chooser) are separate processes, are not JVM windows, and are unreachable - consistent with the standing rule that JIPipe never drives native dialogs.

### 4.3 Input modes

- `dispatch` (default): construct synthetic `MouseEvent`/`MouseWheelEvent`/`KeyEvent` and deliver them via the target component's `dispatchEvent` on the EDT. Deterministic, headless-compatible, unaffected by the Wayland portal issues observed earlier in this session. Limitation (documented in the op description): components that poll physical OS state may not react; use `system` mode for those.
- `system`: `java.awt.Robot` input (mouse move/press/release/wheel, key events), executed OFF the EDT on the bounded capture-style executor pattern established for screenshots, with a bounded wait for completion. Requires a non-headless environment; refuses with a clear error otherwise. This mode exists for the Interactive JIPipe milestone surfaces (3D viewers, annotation canvases) that read physical input state.

### 4.4 Auditability

Every Layer 2 call is recorded in the MCP operation log with the parameters, the resolved target, and the mode (the standard operation logging; no separate mechanism). The response echoes the same data.

### 4.5 The probe, act, confirm loop (agent guidance)

Layer 2 input is best-effort delivery, not proof of effect: a successful `ui_click` confirms the event reached the component, not that the component did what the agent expected (the same caveat the browser extensions state - an `ok` only confirms input delivery). The operations therefore follow the extension loop, and the operation descriptions say so:

1. **Probe** - `ui_probe` the point (or use `screenshot_canvas`/`screenshot_window` plus `ui_probe`) to learn exactly what is there and whether a Layer 1 operation applies.
2. **Act** - prefer the Layer 1 operation the probe named (`fire_action`, `set_dock_visible`, `set_parameter`); only if no identity exists, deliver the coordinate input.
3. **Confirm** - re-read after the action (`get_ui_state`, `ui_probe` the same point, `get_viewer_state`, or a screenshot) to verify the intended change happened. If not, re-probe and retry rather than assuming success.

This is the mechanism that lets an agent "know it targeted the right element": the probe resolves the element and its best operation before acting, and the post-action read confirms the effect. The optional user-facing virtual-cursor visual (a pointer that glides to the target, off by default) is a future cosmetic for the Interactive JIPipe milestone and is explicitly out of scope here - it is what the user watches, not how the agent aims.

## 5. Permissions

- `fire_action`: Modify, Ask (actions mutate; per-id classification is deliberately not attempted).
- `list_ui_actions`, `list_docks`, `set_dock_visible`: Query class, read-only (`set_dock_visible` follows the `navigate_to_node` "view state only" precedent).
- `ui_click`, `ui_drag`, `ui_scroll`, `ui_type_text`: Modify, Ask. `ui_probe`: Query, read-only.
- Composite conversion: no new operation; `set_parameter` keeps its existing permission (Ask for non-read-only targets per the existing classification).

## 6. Testing policy

- Unit tests: pure-logic helpers only - bounds validation against a window set, the format/sibling-style resolution tables for dock and action id resolution, and the new conversion encodings (each against the real parameter class, no Swing).
- No committed tests exercise synthetic input delivery (headless CI; user policy note in section 2.4). The input layer is verified live against the running application.
- The schema-description registry contract (`AutomationOperationsSchemaTest`) covers all new operations automatically; the presence gate gains the new ids.

## 7. Expected files

- `api/automation/operations/UiInteractionOperations.java` (new): the nine operations (fire_action, list_ui_actions, set_dock_visible, list_docks, ui_probe, ui_click, ui_drag, ui_scroll, ui_type_text) - or a split into two files (structured vs input) if the input layer's executor needs isolation; implementer judgment, coherence first.
- `api/automation/JIPipeAutomationAPI.java`: composite conversion additions to `convertParameterValue`.
- `api/shortcuts/JIPipeShortcutActions.java` + the graph editor scope: id assignments for the nine context actions.
- `AutomationPlugin.java`: registrations.
- Possibly small additive accessors on `JIPipeDesktopGraphEditorUI` / dock panel classes where a public method is missing (mirroring the `navigateToRow` precedent from sub-project 2).
- Test files for the pure helpers.

## 8. Out of scope

- Modal and native dialogs (EDT-blocking, unreachable by the window rule, untestable).
- OS-global input outside JIPipe JVM windows.
- Main-menu id assignment (deferred until a case needs it; the registry extension path exists).
- Matrix and image parameter conversion.
- CI tests for synthetic input.
- Per-action permission classification.

## 9. Sequencing

Implemented on branch `1379-agentic-interactions` after sub-project 2 (complete). Rebased onto master together with the rest of the branch once !198 merges. KDB integration (sub-project 4) and the eight agentic acceptance cases (sub-project 5) follow; the cases are the acceptance scenarios this spec's operations will be exercised with.
