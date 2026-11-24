# Spec: #1383 sub-project B - graph rendering and editor view state

Date: 2026-11-20
Status: DRAFT for user review
Work item: #1388 (child of #1383, MCP enhancements). Depends on: sub-projects A (!204), F (!205), E (!206, open) - independent file surface except `AutomationPlugin` registration blocks and the shared screenshot contract.
Source: user feedback items 1, 2, 9, 10 of the #1383 charter. Design approved in chat (user, 2026-11-20): approach A, the thin surface - "yes A should be well enough".

## 1. Intent

An agent that edits a pipeline must be able to look at it the way a user does: obtain a full-fidelity picture of a compartment's graph (not the viewport crop, not the simplified minimap painter), put the editor back into a sane view after programmatic edits, and see the compartment overview without a screenshot framed as a document tab. All three are view-only: no operation in this sub-project changes project data.

## 2. Confirmed decisions

1. **Approach A (thin surface) approved** (user, 2026-11-20). Rejected for now: rendering a sub-selection of nodes (needs canvas visibility machinery that does not exist), and `set_view`/`restore_view` (a view-state model nothing owns; revisit later - approach C is addable without rework).
2. **Natural-scale rendering** (my call, stated in the approved design): the full-graph export renders at zoom 1.0 regardless of the user's current zoom, so before/after comparisons across calls are deterministic. The user's zoom is restored afterwards; the normalization happens inside one EDT run so nothing is painted in between.
3. **No per-node fit target**: `fit_to_nodes` fits the whole canvas (all nodes). Focusing a single node already exists (`navigate_to_node` selects and scrolls to it); a subset fit without hiding other nodes adds machinery for a cosmetic gain.
4. **Sub-project order** (charter): B fourth of six (after A, F, E). New charter additions ledgered 2026-11-20 and OUT OF SCOPE here: sub-project G (node movement + canvas note creation, user-confirmed scope), KDB revival decision (pending).

## 3. Verified ground (master bcec5fbc30)

- **Screenshot seams**: `ScreenshotSource` (`api/automation/ScreenshotSource.java:31`) exposes `renderCanvas` (viewport view via `renderComponentScaled(canvas)`) and `renderMinimap` (full-graph bounds through `canvas.getPaintManager().paintMiniMap`, a simplified painter). Both use the editor-on-demand machinery `findOrOpenCanvas` / `forceLayoutAndPaintCycle` / `closeAutoOpenedCanvas` (`JIPipeDesktopMCPIntegration.java:946-1000`): when the compartment has no open editor tab, one is opened without switching tabs, its layout/paint cycle is forced, and it is closed afterwards unless it was already open (E2E ruling 2026-11-16).
- **GUI export** (the render this sub-project exposes): `JIPipeDesktopGraphCanvasUI.createScreenshotPNG()` = `ScreenImage.createImage(this)` (whole canvas component), `createScreenshotSVG()` = `ScreenImageSVG.createImage(this)` returning an `SVGGraphics2D` whose `getSVGElement()` string is written with `SVGUtils.writeToSVG` (`JIPipeDesktopGraphEditorUI.java:1254-1283`). These are full-fidelity node renderings at the CURRENT zoom.
- **Zoom**: `setZoom(double)` is public (`JIPipeDesktopGraphCanvasUI.java:1740`); `zoomIn`/`zoomOut` clamp to [0.1, 3]; `resetZoom()` = 1.0. No zoom-to-fit exists anywhere (`grep zoomToFit|fitToNodes` = 0 hits).
- **Selection**: `JIPipeDesktopGraphCanvasSelectionManager.clearSelection()` (`canvas/managers/JIPipeDesktopGraphCanvasSelectionManager.java:125`); editors expose it via `getSelectionManager()`.
- **Window/tab resolution precedent**: `UiStateOperations.NavigateToNode` resolves the project window (`resolveTargetWindow`), uses the selected tab when it is a pipeline editor, otherwise `workbench.getOrOpenPipelineEditorTab(compartment, true)` and defers the view change into `SwingUtilities.invokeLater` (`UiStateOperations.java:232-340`). Category Query, `isReadOnly() = true` ("Changes view state only, never project data"), `requiresEdt() = true`.
- **Compartments-editor surface bug**: `findUiSurfaceOnEdt("compartments-editor")` returns the whole `JIPipeDesktopCompartmentsGraphEditorUI` tab content, so `screenshot_ui` captures the editor including its dock sidebar (`JIPipeDesktopMCPIntegration.java:1130-1136`). The compartments canvas (the editor's `getCanvasUI()`) renders the compartment graph itself.
- **Image envelope**: operations return inline images as JSON fields `image-base64` + `mime`; the broker converts them to MCP image content (`MCPBrokerServer.java:91`). `save-to` targets are rooted by `AutomationOperationUtils.resolveWriteTarget` (the `jipipe-mcp-export` temp directory or the project work directory) and answer `{savedTo, sizeBytes}`.
- **Verify-first item (implementer, before coding B1)**: `screenshot_canvas`'s description says "visible viewport at the current zoom/pan", but its implementation prints the whole canvas component via `renderComponentScaled`. Determine empirically (headless harness or a live check by the user) whether the canvas component equals the viewport or the full graph area. Either way `export_graph_image` is specified and built below; the finding only decides whether `screenshot_canvas`'s description must be corrected in the same commit (it must tell the truth).

## 4. B1: `export_graph_image` (new Render operation)

New operation in `ScreenshotOperations` following the house seam: production constructor uses the globally registered `ScreenshotSource`, a package-private test constructor takes a fixed source.

`ScreenshotSource` gains two methods (documented with the same editor-on-demand contract as the existing pair):

- `BufferedImage renderFullCanvas(String compartmentKey, double scale)`: full-fidelity render of the compartment's ENTIRE graph at zoom 1.0 - implementation: remember `getZoom()`, `setZoom(1.0)` (skip when already 1.0), force the layout/paint cycle for auto-opened tabs, `renderComponentScaled(canvas, scale)`, restore the old zoom in `finally`, `closeAutoOpenedCanvas` in `finally`.
- `String renderFullCanvasSvg(String compartmentKey)`: same normalization, then `ScreenImageSVG.createImage(canvas).getSVGElement()`.

Operation params: `compartment` (required, UUID/alias/name), `format` (optional enum `png|svg`, default `png`), `scale` (optional number, default 1, PNG only), `save-to` (optional rooted path).

- PNG without `save-to`: inline `{image-base64, mime: "image/png"}` (the `AutomationResults.choose` envelope, corrected 2026-11-20: the screenshot size contract returns ONLY `{"saved-to": path}`, no size field).
- PNG with `save-to`: exactly the screenshot envelope `{"saved-to": path}` via the same `finish` helper as the sibling operations.
- SVG: file format only - without `save-to` the response is an error naming the restriction (SVG cannot travel the inline image envelope); with `save-to` the element is written UTF-8 and the response is `{"saved-to": path, "sizeBytes": n}` (the byte length; useful because there is no inline fallback to eyeball).
- Unknown compartment, no source, or no project: the existing `error(...)` envelope style of the sibling operations.

Description names the distinction explicitly: full graph at natural zoom with full-fidelity node rendering; use `screenshot_canvas` for the user's current view and `screenshot_minimap` for the minimap painter. Category Render, `isReadOnly() = true`, `requiresEdt() = true`.

## 5. B2: `fit_to_nodes` and `clear_selection` (new view-state operations)

Both live in `UiStateOperations`, mirror `NavigateToNode`'s resolution and classification (Query, read-only true, EDT true), and change view state only.

- **`fit_to_nodes`**: params `compartment` (optional). Resolution: with `compartment`, open-or-focus that compartment's editor tab (`getOrOpenPipelineEditorTab(compartment, true)`) and defer the fit into `invokeLater` (the NavigateToNode deferred branch); without, the currently selected tab must be a graph editor (pipeline or compartments flavor - both extend `JIPipeDesktopGraphEditorUI`), otherwise the error `no graph editor tab is active; pass compartment to open one`. Fit math is a pure static helper (testable headless, e.g. `ViewFit.compute`): union the node UI bounds (in canvas coordinates), pick `zoom = min(viewportW/bboxW, viewportH/bboxH)` clamped to [0.1, 3] with a small margin, and derive scrollbar values that center the bbox; the EDT glue applies `setZoom(zoom)` and the scrollbar positions. Empty canvas (no node UIs): `ok: true, zoom: unchanged, nodes: 0` - a no-op, not an error. Response: `{ok, compartment, zoom, tabOpened?}`.
- **`clear_selection`**: params `compartment` (optional, same resolution and active-tab rule). Calls `getSelectionManager().clearSelection()` on the resolved editor. Response: `{ok, cleared: <selection size before clearing>, compartment}`. Clearing an empty selection is `ok: true, cleared: 0`.

## 6. B3: compartments-editor surface fix

`findUiSurfaceOnEdt("compartments-editor")` returns `editor.getCanvasUI()` (the compartment graph canvas) instead of the whole editor component, so `screenshot_ui` with surface `compartments-editor` captures the compartment overview without the dock sidebar. The `screenshot_ui` schema description of the surface is updated to say it captures the compartments canvas. Nothing else references the surface id (verified by grep); no schema surface set changes.

## 7. Permissions and broker

`export_graph_image`: Render category (the sibling screenshots' category), read-only, Allow by category default. `fit_to_nodes`, `clear_selection`: Query, read-only, Allow by category default, EDT true. All three are project-bound; no `GLOBAL_TOOL_IDS` changes; no broker changes.

## 8. Testing policy (house rules: headless-safe, no Swing interaction tests, no trivial tests)

- `export_graph_image` over the fake-source seam: PNG inline envelope shape (base64 + mime), PNG `save-to` rooting and `{savedTo, sizeBytes}`, SVG without `save-to` refused with the named restriction, SVG `save-to` byte length, unknown compartment error, `scale` default. The zoom-normalization/restore path is desktop EDT code - verified by the user in the app (no Swing test).
- `ViewFit` pure math: clamping at both bounds, centering arithmetic, aspect handling (wide and tall graphs), empty-bounds no-op. This is the non-trivial logic of B2 and earns its tests.
- `fit_to_nodes` / `clear_selection` headless error paths (no project, unknown compartment via the fake seam where applicable) without touching Swing; the EDT glue is user-verified.
- Registry/schema validation tests pick up the three operations automatically; the compartments surface fix has no test (house ruling: the user checks the running app by eye).

## 9. Out of scope (ledgered)

- `set_view` / `restore_view` (approach C) - revisit if agents roam long pipelines often.
- Selected-compartment rendering (approach B).
- Node movement and canvas note creation - sub-project G (user-confirmed scope, 2026-11-20), designed separately.
- KDB revival - charter decision pending.
