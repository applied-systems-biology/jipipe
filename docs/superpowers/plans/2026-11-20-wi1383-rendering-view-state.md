# #1383 Sub-project B - Graph Rendering and Editor View State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give agents the full-fidelity graph picture (`export_graph_image`, natural zoom, PNG inline/save-to + SVG save-to), view hygiene ops (`fit_to_nodes`, `clear_selection`), and a truthful `compartments-editor` screenshot surface.

**Architecture:** All work in the existing operation layer. `export_graph_image` extends the `ScreenshotSource` seam with two render methods implemented in the desktop wiring (editor-on-demand machinery reused); `fit_to_nodes`/`clear_selection` follow `NavigateToNode`'s window/tab resolution with the fit math extracted into a pure, unit-tested helper. `AutomationPlugin` registers three new tools.

**Tech Stack:** Java 21, Jackson databind, JUnit 5 in jipipe-core, Maven.

**Spec:** `docs/superpowers/specs/2026-11-20-wi1383-rendering-view-state-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- kebab-case operation param names; `additionalProperties: false` on every schema.
- Branch `1383-rendering-view-state` (from master `bcec5fbc30`; spec commit `d969a6c6cd` + envelope-key correction).
- Classification (spec section 7): `export_graph_image` = Render, read-only, `requiresEdt() = true`; `fit_to_nodes`, `clear_selection` = Query, read-only, `requiresEdt() = true` (the `NavigateToNode` classification: view state only, never project data).
- Image envelopes: inline = `AutomationResults.image` (`image-base64` + `mime`); save-to = `AutomationResults.savedTo` (key `saved-to`); PNG save-to goes through the existing `ScreenshotOperations.finish` helper unchanged.
- House test policy: no Swing interaction tests; no tests that restate the implementation; pure-logic helpers get real math tests.
- Test command per task: `mvn -q test -pl jipipe-core -Dtest=ScreenshotOperationsTest,ViewFitTest,UiStateOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` (skip classes that do not exist yet). Builds are sequential - one slot; wait out concurrent mvn, never kill.

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **Zoom restoration on the early-return paths of the full-canvas render** - if `renderComponentScaled` or the SVG painter throws, the user's zoom must still be restored (restore in `finally`, before `closeAutoOpenedCanvas`). The seam has no headless test (desktop EDT code, user-verified by eye: zoom in the app, run `export_graph_image`, zoom must be unchanged). Owned by Task 1 (implementation shape), listed here so the reviewer traces the `finally` order explicitly.
2. **`ViewFit` division-by-zero on degenerate bounds** - a single node or a zero-area union must not produce `Infinity`/`NaN` zoom or scroll. Pinned in Task 2 (`degenerateBoundsYieldFiniteUnitFit`).
3. **`scale` default differs from the siblings** - `screenshot_canvas`/`screenshot_minimap` default `scale` to 2; `export_graph_image` defaults to 1 (natural scale is the point). A copy-paste of `scaleParam` would silently import the wrong default. Pinned in Task 1 (`exportGraphImageDefaultsScaleToNaturalOne`).
4. **SVG without `save-to` must be refused, not silently PNG'd** - the error must name both the restriction and the fix. Pinned in Task 1 (`svgWithoutSaveToIsRefused`).
5. **`fit_to_nodes` on a canvas with no node UIs is a no-op success, not an error** - an empty compartment is a legitimate state after programmatic edits. Pinned in Task 3 via the `ViewFit` empty-bounds contract (glue returns `ok: true, nodes: 0`); the glue itself is user-verified in the app.

---

### Task 1: `export_graph_image` (seam + desktop render + operation)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/ScreenshotSource.java` (add two methods after `renderMinimap`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java` (implement the two methods next to `renderMinimap`, ~line 968)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperations.java` (new `ExportGraphImage` inner class; optional description truth-fix for `ScreenshotCanvas`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java:149` (register after `screenshot_window`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperationsTest.java` (extend `FakeSource`, add tests)

**Interfaces:**
- Consumes: `ScreenshotOperations` internals `error(String)`, `finish(AutomationContext, BufferedImage, JsonNode)`, `textOrNull`, `NO_SOURCE_ERROR`; `AutomationOperationUtils.findCompartment(project, ref)` (null = unknown); `AutomationOperationUtils.resolveWriteTarget(project, paramName, target)` returning `TargetPath(Path path, String errorMessage)`; `AutomationResults.choose/savedTo`.
- Produces: `ScreenshotSource.renderFullCanvas(String compartmentKey, double scale)` -> `BufferedImage` (null = no canvas); `ScreenshotSource.renderFullCanvasSvg(String compartmentKey)` -> `String` (the SVG element, null = no canvas); operation id `export_graph_image` with params `compartment` (required), `format` (`png|svg`, default `png`), `scale` (number, default 1, PNG only), `save-to` (optional). Response envelopes: PNG inline `{image-base64, mime}`; PNG save-to `{saved-to}`; SVG without save-to = error; SVG save-to `{saved-to, sizeBytes}`.

- [ ] **Step 1: Write the failing tests** (in `ScreenshotOperationsTest`)

Extend `FakeSource` with: `BufferedImage fullCanvas` (init like `canvas`), `String svg = "<svg/>"`, `Double fullCanvasScale` (recorded), and the two new `@Override` methods that record their arguments and return the fields. Then:

```java
@Test
void exportGraphImageDefaultsScaleToNaturalOne() // compartment "Screenshot Compartment"; no scale param
    // -> result has "image-base64" + "mime"="image/png"; source.fullCanvasScale == 1.0 (1e-9)

@Test
void exportGraphImagePassesScaleThrough() // scale 2.5 -> source.fullCanvasScale == 2.5; inline envelope

@Test
void exportGraphImageWithSaveToWritesPngAndReturnsOnlyPath() // save-to inside the project work directory
    // -> result has "saved-to", no "image-base64"; file exists and is a decodable PNG (ImageIO.read != null)

@Test
void svgWithoutSaveToIsRefused() // format=svg, no save-to
    // -> error mentions both "svg" and "save-to"; no "image-base64"

@Test
void svgWithSaveToWritesElementAndReportsSizeBytes() // format=svg, save-to inside project work dir
    // -> result "saved-to" set; result "sizeBytes" == byte length of the written file; file content == source.svg

@Test
void exportGraphImageWithUnknownCompartmentReturnsError() // -> "Compartment not found: does not exist"

@Test
void exportGraphImageWithoutSourceReturnsDocumentedError() // -> ScreenshotOperations.NO_SOURCE_ERROR
```

Model each on the existing `canvas*` tests (same `createProject`/`createContext` helpers; save-to paths use the same allowed-root trick as `canvasWithSaveToWritesPngAndReturnsOnlyPath`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest=ScreenshotOperationsTest`
Expected: COMPILATION ERROR - `ScreenshotOperationsTest.FakeSource` does not implement `renderFullCanvas`/`renderFullCanvasSvg` (the interface methods do not exist yet). This is the expected first failure.

- [ ] **Step 3: Add the two methods to `ScreenshotSource`**

Javadoc for both: full-fidelity render of the compartment's ENTIRE graph at zoom 1.0 (same painter as the on-screen canvas); the implementation remembers the current zoom, sets 1.0 (skipped when already 1.0), forces the layout/paint cycle for auto-opened tabs, renders, and restores the old zoom in `finally` before `closeAutoOpenedCanvas` - all within one EDT run so nothing is painted in between; same editor-on-demand contract as `renderCanvas` (E2E ruling 2026-11-16). `renderFullCanvasSvg` returns the SVG element string (`SVGGraphics2D.getSVGElement()`).

- [ ] **Step 4: Implement both methods in `JIPipeDesktopMCPIntegration`** next to `renderMinimap`

Same skeleton as `renderCanvas` (`callOnEdt`, `findOrOpenCanvas`, null check, `try/finally { closeAutoOpenedCanvas }`), plus the zoom normalize/restore bracket. PNG path: `renderComponentScaled(opened.canvas(), scale)`; order inside `try`: zoom normalize -> `forceLayoutAndPaintCycle` when `opened.tab() != null` -> render -> zoom restore in `finally`. SVG path: `ScreenImageSVG.createImage(opened.canvas()).getSVGElement()` (import `org.hkijena.jipipe.utils.ui.ScreenImageSVG`; `SVGUtils` is NOT needed here - the operation writes the string).

- [ ] **Step 5: Implement `ExportGraphImage` in `ScreenshotOperations`**

Inner class modeled 1:1 on `ScreenshotCanvas` (production constructor `ScreenshotOperations::getSource`, package-private test constructor, `getId() = "export_graph_image"`, `requiresEdt() = true`, Render, read-only). `execute`: the standard null-source/project/compartment gates, then:
- `format` via `params.path("format").asText("png")`; anything but `png`/`svg` -> `error("Unknown format: <f> (supported: png, svg)")`.
- SVG: `save-to` blank/absent -> `error("format svg requires save-to: SVG cannot be returned inline (use format png for the inline image, or pass save-to)")`; resolve target, `Files.writeString(target, svgElement, StandardCharsets.UTF_8)`, return `AutomationResults.savedTo(target)` with `.put("sizeBytes", <file byte length>)` added to the returned node.
- PNG: `screenshotSource.renderFullCanvas(key, params.path("scale").asDouble(1.0))` (do NOT reuse `scaleParam` - its default is 2); null -> `error("No canvas available for compartment: " + compartment)`; otherwise `finish(ctx, image, params)`.
- Schema: `compartment` (required, "Compartment to render (UUID, alias, or name)"), `format` (enum, default png, "png renders a PNG image (inline or save-to); svg writes an SVG file (save-to only)"), `scale` ("Render scale factor (default 1 = natural size); PNG only"), `save-to` (same wording as the siblings).
- Description: "Export the FULL graph of a compartment as an image at natural zoom (1.0), full-fidelity node rendering, independent of the user's current zoom and scroll. The user's zoom is restored afterwards. Use screenshot_canvas for the user's current view and screenshot_minimap for the minimap painter. PNG is returned inline or saved via save-to; SVG is written via save-to only." + `SAVE_TO_DESCRIPTION`.

- [ ] **Step 6: Register in `AutomationPlugin`** after `screenshot_window`: `registerAutomationOperation("export_graph_image", new ScreenshotOperations.ExportGraphImage());`

- [ ] **Step 7: `ScreenshotCanvas` description truth-check** (doc-only)

Read how `renderComponentScaled` sizes the image (whole canvas component) and how the canvas sizes itself (`minDimensions` growth in `JIPipeDesktopGraphCanvasUI.crop()`, `getParent().revalidate()`). If the evidence shows `screenshot_canvas` actually captures more than the visible viewport (whole graph at current zoom), correct its description to say so in this task's commit and note the finding in the commit body; if it stays ambiguous, leave the description unchanged and record the question in the SDD ledger for the user's by-eye verification. Do not change behavior either way.

- [ ] **Step 8: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest=ScreenshotOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS (all existing tests plus the seven new ones; registry/schema gates pick up the new operation).

- [ ] **Step 9: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/ScreenshotSource.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperationsTest.java
git commit -m "feat(#1383): add export_graph_image operation (full-graph render at natural zoom)"
```

### Task 2: `ViewFit` pure math helper

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ViewFit.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ViewFitTest.java`

**Interfaces:**
- Produces (Task 3 consumes): `ViewFit.compute(Rectangle2D boundsAtCurrentZoom, double currentZoom, int viewportWidth, int viewportHeight, double minZoom, double maxZoom)` -> `FitResult(double zoom, int scrollX, int scrollY)`. Contract: `zoom = clamp(currentZoom * min(viewportWidth / max(boundsWidth, 1), viewportHeight / max(boundsHeight, 1)), minZoom, maxZoom)`; the scroll positions center the bounds center converted to new-zoom pixels (`center / currentZoom * zoom`) minus half the viewport; degenerate/empty bounds (width or height <= 0) yield `zoom = currentZoom` (clamped) and scroll 0/0. All values finite.

- [ ] **Step 1: Write the failing tests** (`ViewFitTest`, plain JUnit, no fixtures)

```java
@Test void wideGraphFitsByWidth()          // bounds 1000x100 at zoom 1, viewport 500x500 -> zoom 0.5, scroll centers
@Test void tallGraphFitsByHeight()         // bounds 100x1000 at zoom 1, viewport 500x500 -> zoom 0.5
@Test void smallGraphZoomsInAndClampsAtMax() // bounds 10x10, viewport 500x500, maxZoom 3 -> zoom 3
@Test void hugeGraphClampsAtMinZoom()      // bounds 100000x100000, viewport 500x500, minZoom 0.1 -> zoom 0.1
@Test void scrollCentersTheBounds()        // bounds center 400,300 at zoom 1->2, viewport 400x400 -> scroll = center*2 - 200 per axis
@Test void degenerateBoundsYieldFiniteUnitFit() // 0-width bounds -> no NaN/Infinity, zoom == clamped current, scroll 0
@Test void fractionalCurrentZoomRescalesBounds() // bounds measured at zoom 0.5 scale correctly to the new zoom
```

Assertions assert exact values where the math is exact, and `Double.isFinite` plus sign/range where the contract is only bounded.

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=ViewFitTest` -> COMPILATION ERROR (class missing).

- [ ] **Step 3: Implement `ViewFit`** - package-private final class, private constructor, static `compute`, nested `record FitResult(double zoom, int scrollX, int scrollY)`; the math exactly as in the interface contract; no Swing imports.

- [ ] **Step 4: Run to verify pass** - same command -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ViewFit.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ViewFitTest.java
git commit -m "feat(#1383): add the graph view fit math"
```

### Task 3: `fit_to_nodes` and `clear_selection` operations

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UiStateOperations.java` (two new inner classes after `NavigateToNode`; reuse `resolveTargetWindow`, `error`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java:123` (register after `navigate_to_node`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/UiStateOperationsTest.java` (create; headless guard tests only)

**Interfaces:**
- Consumes: Task 2's `ViewFit.compute(...)`/`FitResult`; `NavigateToNode`'s private helpers pattern (`resolveTargetWindow(ctx, params)` - widen from `private` to `private static` is already the case; reuse as-is); `JIPipeDesktopProjectWorkbench.getOrOpenPipelineEditorTab(JIPipeProjectCompartment, boolean)`; `findOpenPipelineEditorTabs(compartment)`; `JIPipeDesktopGraphEditorUI.getSelectionManager()`, `getCanvasUI()`; `JIPipeDesktopGraphCanvasUI.getNodeUIs()`, `getZoom()`, `setZoom(double)`, `getScrollPane()`.
- Produces: operation ids `fit_to_nodes` (params: `compartment` optional, `project` optional) and `clear_selection` (same params). Responses: `fit_to_nodes` -> `{ok: true, compartment: <uuid>, zoom: <applied zoom>, nodes: <count>, tab_opened?: <bool>}` (deferred compartment branch reports the pre-fit zoom application like `NavigateToNode` reports deferred scroll; `nodes: 0` = no-op success with the zoom unchanged); `clear_selection` -> `{ok: true, compartment: <uuid>, cleared: <selection size before clearing>}`.

- [ ] **Step 1: Write the failing guard tests** (`UiStateOperationsTest`)

Create the class modeled on `ScreenshotOperationsTest`'s `ensureJIPipe` + `createProject`/`createContext` helpers:

```java
@Test void fitToNodesWithoutProjectReturnsError()    // -> error "No project loaded"
@Test void clearSelectionWithoutProjectReturnsError() // -> error "No project loaded"
@Test void fitToNodesWithUnknownCompartmentReturnsError() // compartment "missing" -> error "Compartment not found: missing"
```

(The window-resolution and fit application paths need a live desktop and are deliberately untested per the house policy; the user verifies them in the app.)

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=UiStateOperationsTest` -> COMPILATION ERROR (classes missing).

- [ ] **Step 3: Implement `FitToNodes` in `UiStateOperations`**

Classification exactly like `NavigateToNode` (Query, read-only true, `requiresEdt() = true`). Schema: `compartment` ("Compartment whose editor tab is fitted (UUID, alias, or name). Omit to fit the active graph editor tab"), `project` (same wording as `NavigateToNode`), `additionalProperties: false`. Description: "Fit the graph editor view to all nodes: zoom the active or given compartment's editor so every node is visible, and center the content. Changes view state only, never project data".

`execute`:
1. Project null -> `error("No project loaded")`.
2. With `compartment`: resolve via `AutomationOperationUtils.findCompartment` (null -> `error("Compartment not found: " + ref)`); resolve window (null -> `error("No matching project window is open")`; wrong project -> the `NavigateToNode` wording); `tabOpened = workbench.findOpenPipelineEditorTabs(compartment).isEmpty()`; `getOrOpenPipelineEditorTab(compartment, true)`; defer into `SwingUtilities.invokeLater`: if the tab content is a `JIPipeDesktopGraphEditorUI`, apply the fit (step 3's private helper); return `{ok, compartment: uuid, tab_opened}` WITHOUT `zoom`/`nodes` (the fit runs deferred, exactly like `NavigateToNode` reports "the response reports the target compartment and whether a tab had to be opened rather than the final scroll state" - say the same in `getAgentDocumentation()`).
3. Without `compartment`: the selected tab (`workbench.getDocumentTabPane().getSelectedTab()`) content must be a `JIPipeDesktopGraphEditorUI` (pipeline OR compartments flavor), else `error("no graph editor tab is active; pass compartment to open one")`; apply the fit synchronously and return `{ok, compartment: <canvas compartment uuid or null>, zoom, nodes}`.
4. Private `applyFit(JIPipeDesktopGraphEditorUI editor)`: `canvas = editor.getCanvasUI()`; union `canvas.getNodeUIs().values()` bounds into a `Rectangle2D`; empty union -> no-op; `scrollPane = canvas.getScrollPane()` (null -> no-op); `FitResult fit = ViewFit.compute(bounds, canvas.getZoom(), scrollPane.getViewport().getExtentSize().width, ...height, 0.1, 3.0)`; `canvas.setZoom(fit.zoom())`; then `SwingUtilities.invokeLater` the scrollbar `setValue(fit.scrollX())/fit.scrollY()` (the zoom's re-layout must land before the scroll clamps).

- [ ] **Step 4: Implement `ClearSelection` in `UiStateOperations`**

Same classification and resolution skeleton (with-compartment deferred branch, without-compartment active-tab branch with the same error). Selection size read BEFORE clearing via `editor.getSelectionManager().getSelection().size()`; then `clearSelection()`. Description: "Clear the node selection of the active or given compartment's graph editor. Changes view state only, never project data". Response `{ok, compartment, cleared}` (deferred branch omits `cleared`, agent documentation says so).

- [ ] **Step 5: Register in `AutomationPlugin`** after `navigate_to_node`: `fit_to_nodes` -> `new UiStateOperations.FitToNodes()`, `clear_selection` -> `new UiStateOperations.ClearSelection()`.

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest=UiStateOperationsTest,ViewFitTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UiStateOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/UiStateOperationsTest.java
git commit -m "feat(#1383): add fit_to_nodes and clear_selection view operations"
```

### Task 4: `compartments-editor` surface fix

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java:1130-1136` (the `"compartments-editor"` case)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperations.java` (the `surface` schema description of `screenshot_ui`)

**Interfaces:**
- Consumes: `JIPipeDesktopCompartmentsGraphEditorUI.getCanvasUI()` (inherited).
- Produces: surface `compartments-editor` now yields the compartments CANVAS (the compartment overview) instead of the whole editor tab including the dock sidebar. No schema surface-set change.

- [ ] **Step 1: Change the case body** to `return editor.getCanvasUI();` (keep the tab lookup loop and the null return; the variable type stays `JIPipeDesktopCompartmentsGraphEditorUI`). Update the method Javadoc's catalog sentence: the compartments surface captures the compartments canvas.

- [ ] **Step 2: Update the `screenshot_ui` schema description**: extend the surface list sentence with "(compartments-editor captures the compartments canvas, i.e. the compartment overview without the editor panels)".

- [ ] **Step 3: Verify nothing else depends on the old surface semantics** - `grep -rn "compartments-editor" jipipe-core/src/main/java` must show only the integration and the schema description. No test (house ruling: the user checks the running app by eye).

- [ ] **Step 4: Compile** - `mvn -q compile -pl jipipe-core` -> EXIT 0.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperations.java
git commit -m "fix(#1383): capture the compartments canvas for the compartments-editor surface"
```

### Task 5: Full-suite verification

- [ ] **Step 1: Run the whole jipipe-core suite** - `mvn -q test -pl jipipe-core` (long; wait it out).
Expected: 0 failures, 0 errors; skipped count at the current master baseline (2) plus any pre-existing environment-dependent skips; zero failures in new or touched test classes.

- [ ] **Step 2: Record the numbers** in the SDD workspace ledger (`.superpowers/sdd/2026-11-19-wi1383/progress.md`): suite counts, the `screenshot_canvas` description truth-check finding (Task 1 Step 7), and any deferred minors.

No commit (ledger path is git-ignored).
