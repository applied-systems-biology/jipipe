# Parameter Editor & Form Panel UX Overhaul - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Overhaul the node parameter panel UX: rule-by-complexity layout, enum/dynamic-set editors on the new panel
API, modernized list editors, a dedicated `FontParameter` with node migration, and a modern multi-space (RGB/HSB/LAB)
color dialog.

**Architecture:** The form panel (`JIPipeDesktopFormPanel`) becomes the single source of parameter names; editors
declare only *shape* (inline vs. stacked). Enum and dynamic-set editors adopt the existing generic
`JIPipeDesktopRichSelectionBox` panel API. Two new library pieces - `ColorSpaces`/`JIPipeDesktopColorChooserDialog` and
`JIPipeFontParameter`/its editor - replace `JColorChooser` and the split font params. #1124 (code editor) is out of
scope; expression editors keep their widget.

**Tech Stack:** Java 21, Swing, Jackson, JUnit 5 (jipipe-core test module only), no new external dependencies.

**Spec:** `docs/superpowers/specs/2026-10-25-parameter-ux-overhaul-design.md` (read it; decisions D1–D9 are normative)

## Global Constraints

- Java 21, Maven. Build check: `mvn -q -pl jipipe-core compile`. Tests: `mvn -q -pl jipipe-core test -Dtest=<Class>`.
- **No new external dependencies.** Swing only.
- Registration IDs are **stable string literals** - never code-derived (AGENTS.md).
- **No Swing UI-structure tests** (tab titles, component visibility, insets, renderer details, dialog layout). UI
  changes are verified by the user in the running app. Tests only for non-trivial logic (spec §12): color math, font
  param serialization + migration, enum threshold decision, dynamic-set leniency.
- ImageJ legacy patcher constraint (AGENTS.md): do not call `LegacyInjector.preinit()` from tests, do not add
  `--add-opens` workarounds. Tests that call `JIPipe.ensureInstance()` are fine - the `LauncherSessionListener` handles
  ordering.
- Commit per task, message suffix `(#1339)`.
- Do NOT edit `dist/zip/build*.sh` (generated) or remove `ImageJLegacyPatcherLauncherSessionListener`.

## Review Focus

These are the input classes / failure modes the spec implies but no unit test fully exercises - each gets a check in its
owning task:

1. **LAB out-of-gamut input** (e.g. L=100, a=+100, b=+100) - user expects a *clamped sRGB color*, never an exception;
   hex readout shows the clamped value. → Task 6 (clamp round-trip test) + Task 7 (VLM: LAB-tab screenshot shows
   in-range hex, no error dialog).
2. **8-digit hex with alpha** (`#RRGGBBAA`) on an *alpha-capable* parameter - must round-trip the alpha; on a non-alpha
   parameter the α UI is hidden and 8-digit input is normalized to 6-digit. → Task 7 (manual checklist + VLM:
   alpha-capable screenshot shows α rail + checkerboard; plain screenshot shows no α rail).
3. **Editable dynamic enum** (`JIPipeDynamicEnumParameter.isEditable() == true`, e.g. TrackMate feature params,
   `PluginCategoriesEnumParameter`) - must keep the editable combo with free text input even for >5 values. → Task 4
   (decision unit test) + VLM screenshot of the editable-combo case if reachable via the showcase set (else manual).
4. **Dynamic enum whose option count crosses the 5-value threshold at runtime** (values loaded asynchronously / from
   data) - editor must rebuild its control without losing the current value or crashing on a stale combo. → Task 4
   (rebuild test) + manual.
5. **Font migration on a node with *partial* legacy keys** (e.g. size set, no family key in an old project) - must
   compose with defaults for the missing keys, never NPE; idempotent on the second load. → Task 10 (migration test with
   one key omitted) + manual: open a pre-migration project.
6. **Old project containing the removed `collapsed` key** on a dynamic set - loads without error; key gone after save. →
   Task 5 (leniency test).
7. **`comboBoxStyle=true` booleans** (trueLabel/falseLabel, e.g. "By dilation"/"By erosion") - must still render as the
   2-value combo, not a switch. → Task 3 (VLM: comboStyle node screenshot shows the two-value combo with its labels).
8. **Settings sheets / run dialogs using `JIPipeDesktopFormPanel` directly** (non-parameter rows) - unchanged behavior
   (they call `addToForm`/`addWideToForm`; only parameter rows change). → Task 2 (VLM: settings-sheet screenshot
   unchanged vs. master baseline) + manual: open a run dialog.

**VLM verification** (Task 0 harness): every screenshot task produces a *before/after pair* - the `before` rendered from
the merge-base (`master`), the `after` from the feature branch - and the VLM (via `da_describe_image`) verifies the
expected difference *is present* and no obvious regression (empty panel, overlapping/truncated controls, error text) *is
visible*. The VLM verdict is a *signal*, not a gate: ambiguous findings are reported to the user, who remains the final
judge of UI quality (standing ruling).

---

### Task 0: VLM screenshot harness - launcher `screenshot-ui` command

**Files:**

- Create: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java`
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/JIPipeLauncher.java` (dispatch: `screenshot-ui` →
  `ScreenshotUiCommand.doScreenshotUi(argsList)`, `keepRunning = true` - the GUI needs the AWT thread alive during
  rendering; the command exits via `JIPipe.exitLater(0)` when done)
- Create: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/screenshotui/ScreenshotRenderer.java`
  (panel/dialog → PNG logic, reusable)

**Interfaces:**

- Consumes: `GuiCommand.startGui`-style boot (`new ImageJ()` + `ij.command().run(JIPipeGUICommand.class, true)` - see
  `JIPipeGUICommand.main`), `JIPipe.createNode(String id)`, `new JIPipeProject()`/`JIPipeProject.addNode`,
  `JIPipeDesktopParameterFormPanel` (construct directly: flags `WITH_SCROLLING | WITH_DOCUMENTATION`, no project window
  needed - it takes the workbench; use `JIPipeDesktopWorkbench`/`JIPipeDesktopDummyWorkbench` as the parameter panels'
  context - verify which is used by `JIPipeDesktopPipelineParametersPanel`), `java.awt.Robot` or
  `component.paint(Graphics2D)` (headless-safe offscreen: `new BufferedImage(...)`, `component.printAll(g)`).
- Produces:
    - CLI: `java … JIPipeLauncher screenshot-ui --out <dir> [--only <id>[,<id>…]]`
    - PNG outputs (deterministic set, stable filenames - the VLM prompts key off these names):
        - `panel-stack-to-montage.png` - `JIPipeDesktopParameterFormPanel` over a `StackToMontage2Algorithm`
          (`ij1-dimensions-stack2montage-v2`) instance: expressions, booleans, enums (incl. 9-value `Anchor` → box),
          optional colors/numbers
        - `panel-import-file-list.png` - over `FileListDataSource` (`import-file-list`): the path-list editor
        - `panel-roi3d-statistics.png` - over `ExtractRoi3dStatisticsAlgorithm` (`ij3d-measure-roi3d`): the dynamic-set
          editor (Weka feature set fallback: `Roi3dMeasurementSetParameter`)
        - `panel-combo-boolean.png` - over a `comboBoxStyle=true` node (e.g. `MorphologicalReconstruction2DAlgorithm`)
        - `dialog-color.png` - the color dialog at a fixed size over a plain color (once Task 7 exists; before that,
          render `JColorChooser` for the *before* baseline)
        - `settings-appearance.png` - a representative settings sheet (form-panel consumer baseline)
    - `ScreenshotRenderer.renderComponent(Component c, int width, Path outPng): void` - sizes the component
      (`c.setSize(width, c.getPreferredSize().height)`), `c.doLayout()` + `printAll` into a
      `BufferedImage(TYPE_INT_ARGB)`, writes PNG (`ImageIO.write`). Runs on the EDT.
    - Exit code 0 on success; non-zero + stack trace on any render exception.

**How it works (implementation guidance):**

1. Parse args (`--out`, `--only`).
2. Boot the GUI stack *in-process* exactly like `GuiCommand.startGui` (ImageJ context, `JIPipeGUICommand.run()` path -
   set the extension settings silent flag to skip dialogs; look at how `GuiCommand`/`HeadlessRunCommand` handle this -
   `JIPipeExtensionApplicationSettings.getInstanceFromRaw()`), then wait for `JIPipe.getInstance()` to be initialized
   (poll `JIPipe.isInstantiated()` + `ensureInitialized`).
3. On the EDT, for each showcase node id: `JIPipe.createNode(id)`, wrap it (and for `JIPipeAlgorithm`s, no project
   needed if the parameter panel constructor allows - otherwise create a minimal `JIPipeProject` and `addNode` into its
   graph, mirroring `JIPipeCreatePipelineNodesFromDraggedDataDragAndDropBehavior` usage), build the
   `JIPipeDesktopParameterFormPanel`, `renderComponent(panel, 480, out)`.
4. Dialogs: construct the dialog's content pane directly (no `setVisible(true)` needed for `printAll`), render at fixed
   380×560.
5. Write all PNGs, then `JIPipe.exitLater(0)`.

**Before/after workflow (the plan's per-task VLM steps use this):**

- *After* (feature branch): `mvn -q -pl jipipe-launcher -am package -DskipTests` then
  `java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/javax.swing=ALL-UNNAMED -cp <launcher classpath> org.hkijena.jipipe.launcher.JIPipeLauncher screenshot-ui --out /tmp/opencode/1339/after`
- *Before* (baseline): `git stash`-free approach - `git worktree add ../jipipe-before master` (or the merge-base),
  build + run there with `--out /tmp/opencode/1339/before`. (The worktree exists only to render baselines; remove
  after.)
- VLM check: `da_describe_image` on the pair (or a stitched side-by-side) with a per-screenshot expectation (see each
  task's step).

- [ ] **Step 1: Implement the command + renderer** (no showcase panels beyond the 5 above; keep the set small and
  stable).
- [ ] **Step 2: Build + smoke-run on the current branch** (pre-overhaul UI is the expected output - it *becomes* the
  "before" set):
  Run:
  `mvn -q -pl jipipe-launcher -am package -DskipTests && java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/javax.swing=ALL-UNNAMED -cp "$(mvn -q -pl jipipe-launcher dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" org.hkijena.jipipe.launcher.JIPipeLauncher screenshot-ui --out /tmp/opencode/1339/before`
  Expected: exit 0, 6 PNGs in `/tmp/opencode/1339/before` (current UI).
- [ ] **Step 3: VLM sanity pass on the baseline** - `da_describe_image` on `panel-stack-to-montage.png` with prompt
  "Describe the parameter panel: list each row (name + control type)." Expected: recognizable rows (enum combos with 🔍
  buttons, checkbox-list, etc.) - confirms the harness renders real content, not a blank panel.
- [ ] **Step 4: Commit.**

```bash
git add jipipe-launcher/src/main
git commit -m "feat(launcher): screenshot-ui command for VLM-driven UI verification (#1339)"
```

---

### Task 1: `JIPipeDesktopSwitch` component

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/toggle/JIPipeDesktopSwitch.java`

**Interfaces:**

- Consumes: nothing (pure Swing).
- Produces: `public class JIPipeDesktopSwitch extends JToggleButton` - a compact on/off switch (pill ~34×19, themed with
  `ThemeUtils.getCurrentStyle()` colors: off = `Button.borderColor`-family track, on = `getSuccessColor()`),
  animated-free (instant state change), fully keyboard-accessible (space toggles, focus border). API:
  `JIPipeDesktopSwitch()`, `JIPipeDesktopSwitch(String text)`, standard `setSelected`/`isSelected`. Painted via
  `paintComponent` (track + knob), no external resources. Used by Task 3 (boolean editor).

- [ ] **Step 1: Implement the component.**
  Subclass `JToggleButton`; override `paintComponent` to draw the pill track + circular knob (knob left = off, right =
  on); `setOpaque(true)` background = panel background so it blends into rows; preferred size 34×19. No `setModel`
  overrides - plain toggle semantics.
- [ ] **Step 2: Compile check.**
  Run: `mvn -q -pl jipipe-core compile`
  Expected: BUILD SUCCESS.
- [ ] **Step 3: Commit.**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/toggle/JIPipeDesktopSwitch.java
git commit -m "feat(desktop): add JIPipeDesktopSwitch toggle component (#1339)"
```

---

### Task 2: Layout foundation - `isUIInline()` + stacked rows in the form panel

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/api/JIPipeDesktopParameterEditorUI.java` (contract, ~
  line 170 `isUILabelEnabled()`, lines 41–43 `CONTROL_STYLE_*`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/panels/JIPipeDesktopFormPanel.java`
  (column constants lines 96–100, `addToForm` 322–356, `addWideToForm` 395–416, group header 470–491)
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/parameters/JIPipeDesktopParameterFormPanel.java`
  (per-parameter loop 499–601; decision 553–559; sort comparator 495–507; options column 627–711)

**Interfaces:**

- Consumes: existing `JIPipeDesktopParameterEditorUI` contract.
- Produces (later tasks rely on these exact names):
    - `JIPipeDesktopParameterEditorUI.isUIInline(): boolean` - **default `true`**. `false` = stacked row (name line on
      top, full-width editor below).
    - `JIPipeDesktopParameterEditorUI.isUILabelEnabled()` - **now always `true`**; javadoc updated: "the form panel
      provides the parameter's name label; editors must never draw it themselves."
    - Form panel stacked rendering reuses the existing label + options-column helpers, so help `?` and the options menu
      trigger render identically in both shapes.

- [ ] **Step 1: Add `isUIInline()` to the base class** with the semantics above; repoint the existing label-off editors:
    - `isUIInline() = false` (stacked): list, parameter-collection-list, matrix, margin, fixed-margin, inner-margin,
      script (`JIPipeDesktopScriptParameterEditorUI` + the 4 external-editor subclasses).
    - `isUIInline() = true` (inline) after this task: dynamic set (its internals become the selection box in Task 5 -
      inline from the start) and boolean (its control changes in Task 3 - inline in both modes from the start).
      All of these keep `isUILabelEnabled() = true`.
- [ ] **Step 2: Implement the stacked row in `JIPipeDesktopParameterFormPanel`.**
  In the per-parameter loop (lines ~553–559), replace the `isUILabelEnabled()` branch with:
  ```java
  if (ui.isUIInline()) { addToForm(wrappedUI, labelPanel, documentation); }
  else { addToFormStacked(wrappedUI, labelPanel, documentation); }
  ```
  `addToFormStacked` (new, in `JIPipeDesktopFormPanel`): row 1 = label WEST + options component EAST (reuse
  `createAndAddEntryPropertiesComponent`), row 2 = editor with `gridx=0, gridwidth=3, weightx=1, fill=HORIZONTAL`. Group
  headers, pinning, documentation placement, and search filtering keep their existing behavior.
- [ ] **Step 3: Update the sort comparator** (lines 495–507): remove the `!isUILabelEnabled()` grouping clause (no
  label-less editors anymore); keep `isPinned()` → `getUIOrder()` → `getUIControlStyleType()` → name.
- [ ] **Step 4: Remove self-drawn name labels from the self-header editors.** Paths:
  `plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java`,
  `…/JIPipeDesktopParameterCollectionListEditorUI.java`, `…/JIPipeDesktopDynamicSetParameterEditorUI.java`,
  `…/JIPipeDesktopMatrix2DParameterEditorUI.java`,
  `plugins/parameters/ui/library/JIPipeDesktopMarginParameterEditorUI.java`,
  `…/JIPipeDesktopFixedMarginParameterEditorUI.java`, `plugins/parameters/library/roi/InnerMarginEditorUIDesktop.java`
  (boolean is handled in Task 3; script editors draw no name - skip). In this task only: delete the name `JLabel` from
  their toolbars/headers; keep their other chrome (buttons, collapse) until Tasks 4–5 rework those editors. If an
  editor's header becomes empty, remove the header.
- [ ] **Step 5: Compile + screenshot verification (VLM).**
  Run: `mvn -q -pl jipipe-core compile && mvn -q -pl jipipe-launcher -am package -DskipTests`
  Run the Task 0 harness (`screenshot-ui --out /tmp/opencode/1339/after`) and compare with `da_describe_image`:
    - `panel-stack-to-montage.png` (pair vs. before): stacked editors (list/matrix/script) show *name line on top,
      full-width control below*; no editor repeats its name inside the widget; enum/number/boolean rows keep the compact
      inline row.
    - `settings-appearance.png` (pair vs. before): unchanged (Review Focus #8).
      Expected VLM findings: stacked shape present, no double names, no empty panels.
      Then manual verification in the app (user's standing ruling): settings panel, run dialog, list/matrix/script
      nodes.
- [ ] **Step 6: Commit.**

```bash
git add -A jipipe-core/src/main/java/org/hkijena/jipipe/desktop
git commit -m "refactor(desktop): form panel provides parameter names; stacked rows for compound editors (#1339)"
```

---

### Task 3: Boolean editor - switch + inline

**Files:**

- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopBooleanParameterEditorUI.java`
  (current: checkbox-with-name at line ~79; combo mode when `@BooleanParameterSettings(comboBoxStyle=true)` line ~71;
  `isUILabelEnabled()` lines 39–41)

**Interfaces:**

- Consumes: `JIPipeDesktopSwitch` (Task 1); `isUIInline()` contract (Task 2).
- Produces: boolean editor is inline in all modes. Default mode: switch control only. `comboBoxStyle=true` mode:
  `JComboBox` with the two labels from `trueLabel`/`falseLabel` (current values become the selection - no behavior
  change to the value).

- [ ] **Step 1: Rewrite the editor.**
  `isUILabelEnabled() = true`, `isUIInline() = true`. If `@BooleanParameterSettings(comboBoxStyle=true)` is present →
  build the 2-item combo using `trueLabel`/`falseLabel` (existing code path, keep the value mapping `index 0 = true` as
  today). Otherwise → single `JIPipeDesktopSwitch`, selected = current value, action listener pushes via existing
  `setParameter(...)`. No text on either control - the name is the label column.
- [ ] **Step 2: Compile + screenshot verification (VLM).**
  Run: `mvn -q -pl jipipe-core compile && mvn -q -pl jipipe-launcher -am package -DskipTests`
  Harness + `da_describe_image`:
    - `panel-stack-to-montage.png`: the plain boolean ("Add ZCT annotations") is a pill *switch* (no checkbox text),
      name in the label column.
    - `panel-combo-boolean.png`: the 2-value labeled combo ("By dilation"/"By erosion") is still a combo - not a switch.
      (Review Focus #7.)
      Expected: switch present on plain booleans, labeled combos intact.
      Then manual: same two checks in the app.
- [ ] **Step 3: Commit.**

```bash
git commit -am "feat(desktop): boolean parameter editor - switch (inline), keep labeled combos (#1339)"
```

---

### Task 4: Enum & dynamic-enum editors - threshold rule + RichSelectionBox

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/EnumUiMode.java` (enum: `COMBO`,
  `BOX`)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/EnumUiSelection.java` (decision
  logic + constant `int COMBO_MAX_VALUES = 5`)
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopEnumParameterEditorUI.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopDynamicEnumParameterEditorUI.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/selection/JIPipeDesktopRichSelectionBox.java`
  (`Option` + optional tooltip)
- Delete:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/pickers/JIPipeDesktopPickEnumValueDialog.java`,
  `JIPipeDesktopPickDynamicEnumValueDialog.java` (only if zero remaining callers - verify)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/ui/EnumUiSelectionTest.java`

**Interfaces:**

- Consumes: `JIPipeDesktopRichSelectionBox` (single-select mode, `Option(value, displayText, icon, group)` + new tooltip
  overload), `JIPipeEnumParameterItemInfo` (label/icon/tooltip), `JIPipeDynamicEnumParameter.isEditable()`.
- Produces:
    - `EnumUiSelection.mode(int optionCount, boolean dynamic, boolean editable): EnumUiMode` - pure static.
    - `JIPipeDesktopRichSelectionBox.Option(T value, String displayText, Icon icon, String group, String tooltip)` - new
      constructor overload; `String getTooltip()`. The list cell renderer shows the tooltip when non-null.

- [ ] **Step 1: Write the failing decision test.**
  ```java
  class EnumUiSelectionTest {
      @Test void editableDynamicAlwaysCombo() {
          assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(50, true, true));
      }
      @Test void fiveOrFewerCombo() {
          assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(1, false, false));
          assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(5, false, false));
          assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(5, true, false));
      }
      @Test void moreThanFiveBox() {
          assertEquals(EnumUiMode.BOX, EnumUiSelection.mode(6, false, false));
          assertEquals(EnumUiMode.BOX, EnumUiSelection.mode(42, true, false));
      }
  }
  ```
- [ ] **Step 2: Run to verify it fails.**
  Run: `mvn -q -pl jipipe-core test -Dtest=EnumUiSelectionTest`
  Expected: compile failure (classes don't exist).
- [ ] **Step 3: Implement `EnumUiMode` + `EnumUiSelection`** (rule: `editable && dynamic → COMBO`; else
  `count <= COMBO_MAX_VALUES → COMBO`; else `BOX`).
- [ ] **Step 4: Run to verify it passes.**
  Run: `mvn -q -pl jipipe-core test -Dtest=EnumUiSelectionTest`
  Expected: PASS.
- [ ] **Step 5: Add the `Option` tooltip overload** to `JIPipeDesktopRichSelectionBox` (keep the existing 4-arg
  constructor delegating with `null`).
- [ ] **Step 6: Rework both enum editors.**
  In `reload()`: compute the option list (existing logic), then `EnumUiSelection.mode(n, dynamic, editable)`:
    - `COMBO`: plain `JComboBox` (dynamic-editable keeps `setEditable(true)`), **no search button**, value →
      `setParameter`.
    - `BOX`: single-select `JIPipeDesktopRichSelectionBox` (baseLabel = current value's display text; selection handler
      sets the parameter from the first selected option). Options mapped from `JIPipeEnumParameterItemInfo` including
      the new tooltip field. On value change, update the button's label.
      Rebuild the control (not just its items) when the mode flips - guard with `preventReload` as the editors already
      do; never drop the current value on rebuild.
- [ ] **Step 7: Delete the modal pick dialogs** (first
  `grep -rn "PickEnumValueDialog\|PickDynamicEnumValueDialog" --include=*.java` across the repo; if other callers exist,
  migrate them to the box instead of deleting).
- [ ] **Step 8: Compile + full core test suite.**
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS, no regressions.
- [ ] **Step 9: Screenshot verification (VLM).**
  Harness + `da_describe_image` on `panel-stack-to-montage.png` (pair):
    - `InterpolationMethod` (3 values) → plain combo, no 🔍 button.
    - `Anchor` / `Label location` (9 values) → button showing the current value (icon + label), no modal.
      Expected: no 🔍 search buttons remain; big enums render as value-buttons.
      Then manual verification (in app): click the value-button → popup with icons + search + close-on-pick; editable
      dynamic enum (TrackMate feature filter / plugin categories) keeps free-text combo; a >5-value dynamic enum flips
      combo→box without losing the value. (Review Focus #3, #4.)
- [ ] **Step 10: Commit.**

```bash
git add -A jipipe-core/src/main jipipe-core/src/test
git commit -m "feat(desktop): enum editors - combo for <=5 values, rich selection box above (#1339)"
```

---

### Task 5: Dynamic set editor - popup multi-select, drop `collapsed`

**Files:**

- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopDynamicSetParameterEditorUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/api/options/JIPipeDynamicSetParameter.java`
  (remove `collapsed`, lines ~35, 63–71)
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/selection/JIPipeDesktopRichSelectionBox.java`
  (add a "Select all" button next to Clear, only when a per-box flag is set)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/api/options/DynamicSetParameterJsonTest.java`

**Interfaces:**

- Consumes: multi-select `JIPipeDesktopRichSelectionBox` (summary label, groups via `JIPipeEnumParameterItemInfo`).
- Produces:
    - `JIPipeDesktopRichSelectionBox.setHasSelectAll(boolean)` - when true and `multiSelect`, the popup header row shows
      `Select all` next to `Clear`.
    - `JIPipeDynamicSetParameter` without `collapsed`; its Jackson deserializer ignores a legacy `collapsed` field.

- [ ] **Step 1: Write the failing leniency test.**
  Serialize a `JIPipeDynamicSetParameter` subclass (use an existing one, e.g. `DynamicStringSetParameter`) to JSON via
  `JsonUtils`, then deserialize a JSON string that *includes* `"collapsed": true` at the parameter level, and assert: no
  exception, `collapsed` behavior gone (no such field/reader), and re-serialization contains no `collapsed` key. (Test
  the parameter's own (de)serialization as the project reader uses it - same `JsonUtils.getObjectMapper()` path
  `ParameterUtils` uses.)
- [ ] **Step 2: Run to verify it fails** (field still exists / still serialized).
  Run: `mvn -q -pl jipipe-core test -Dtest=DynamicSetParameterJsonTest`
  Expected: FAIL (assertion: re-serialized JSON contains "collapsed").
- [ ] **Step 3: Remove `collapsed` from the model** (field, getter/setter, copy ctor, (de)serialization) + lenient read
  of the legacy key (one `@JsonIgnoreProperties`-style or explicit path check - match how the class currently reads
  JSON).
- [ ] **Step 4: Add `setHasSelectAll`** to the box (button appears only when set; behavior = select all current options
  through the existing selection-handler path).
- [ ] **Step 5: Rewrite the dynamic set editor.**
  Inline row (name from label column - Task 2 already removed its header). Control = multi-select
  `JIPipeDesktopRichSelectionBox` with `summaryLabel` = the parameter's name (button shows "<name> (n)" when non-empty,
  plain name when empty), `setHasSelectAll(true)`, options from the set's allowed values + item info (icons, groups,
  tooltips). Selection handler writes the set's values. Live - no OK, no collapse, no "items are hidden" label.
- [ ] **Step 6: Run tests to verify pass.**
  Run: `mvn -q -pl jipipe-core test -Dtest=DynamicSetParameterJsonTest`
  Expected: PASS. (Review Focus #6.)
- [ ] **Step 7: Compile + screenshot verification (VLM).**
  Run: `mvn -q -pl jipipe-core compile && mvn -q -pl jipipe-launcher -am package -DskipTests`
  Harness + `da_describe_image` on `panel-roi3d-statistics.png` (pair): the dynamic set renders as a compact **button
  with count summary** ("… (n)") - no vertical checkbox list, no Collapse button, no "items are hidden" label, no
  self-drawn header.
  Then manual: popup toggles live with group headers, Select all / Clear work; an old project file with `collapsed`
  opens fine.
- [ ] **Step 8: Commit.**

```bash
git add -A jipipe-core/src/main jipipe-core/src/test
git commit -m "feat(desktop): dynamic set editor - multi-select popup, drop persisted collapse (#1339)"
```

---

### Task 6: `ColorSpaces` math (pure functions)

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/color/ColorSpaces.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/color/ColorSpacesTest.java`

**Interfaces:**

- Consumes: `java.awt.Color` only.
- Produces (all `public static`):
    - `double[] toHsb(Color c)` → `{h 0-360, s 0-100, b 0-100}`
    - `Color fromHsb(double h, double s, double b)` (wraps/modulos inputs into range)
    - `double[] toLab(Color c)` → `{L 0-100, a, b}` in CIE L\*a\*b\* (D65)
    - `Color fromLab(double L, double a, double b)` → **nearest sRGB** (clamped; this is the gamut clamp - the returned
      color is always a valid sRGB)
    - `String toHex(Color c)` → `#RRGGBB` when alpha == 255 else `#RRGGBBAA`
    - `Color fromHex(String s)` → accepts `#RGB`, `#RRGGBB`, `#RRGGBBAA` (and without `#`); returns a color with the
      given alpha

- [ ] **Step 1: Write the failing tests** (assertions, exact values):
  ```java
  class ColorSpacesTest {
      @Test void hsbRoundTrip() {
          Color c = new Color(59, 157, 255);
          double[] hsb = ColorSpaces.toHsb(c);
          Color back = ColorSpaces.fromHsb(hsb[0], hsb[1], hsb[2]);
          assertTrue(near(back.getRed(), c.getRed()));
          assertTrue(near(back.getGreen(), c.getGreen()));
          assertTrue(near(back.getBlue(), c.getBlue()));
      }
      @Test void hsbKnownValue() {
          // red: h=0, s=100, b=100
          assertArrayEquals(new double[]{0, 100, 100}, ColorSpaces.toHsb(Color.RED), 0.01);
      }
      @Test void labRoundTripInGamut() {
          Color c = new Color(59, 157, 255);
          double[] lab = ColorSpaces.toLab(c);
          Color back = ColorSpaces.fromLab(lab[0], lab[1], lab[2]);
          assertTrue(near(back.getRed(), c.getRed(), 1.0));   // 1/255 tolerance
          assertTrue(near(back.getGreen(), c.getGreen(), 1.0));
          assertTrue(near(back.getBlue(), c.getBlue(), 1.0));
      }
      @Test void labOutOfGamutClamps() {
          Color c = ColorSpaces.fromLab(100, 100, 100);
          // must be a valid sRGB (in-range), and a saturated red-ish color
          assertTrue(c.getRed() > 150 && c.getGreen() < 150 && c.getBlue() < 150);
      }
      @Test void hexRoundTripWithAlpha() {
          assertEquals("#3B9DFF", ColorSpaces.toHex(new Color(0x3b, 0x9d, 0xff)));
          assertEquals("#3B9DFF80", ColorSpaces.toHex(new Color(0x3b, 0x9d, 0xff, 0x80)));
          Color c = ColorSpaces.fromHex("#3b9dffb3");
          assertEquals(0x3b, c.getRed()); assertEquals(0xb3, c.getAlpha());
      }
      private static boolean near(int got, int want) { return near(got, want, 0.5); }
      private static boolean near(int got, int want, double tol) { return Math.abs(got - want) <= tol; }
  }
  ```
- [ ] **Step 2: Run to verify failure.**
  Run: `mvn -q -pl jipipe-core test -Dtest=ColorSpacesTest`
  Expected: compile failure.
- [ ] **Step 3: Implement.**
  HSB via `Color.RGBtoHSB`/`HSBtoRGB` (scaled). LAB: sRGB → linear → XYZ (D65 sRGB matrix) → L\*a\*b\* with the standard
  f (t) cube-root function; inverse with the sRGB transfer function and **nearest-sRGB clamp**: when the inverse RGB is
  out of gamut, clamp via iterative channel clipping (reduce a/b toward 0 until all channels are in [0,1], then clamp) -
  a simple, deterministic loop (≤ 30 iterations, converge to the closest representable color).
- [ ] **Step 4: Run to verify pass.**
  Run: `mvn -q -pl jipipe-core test -Dtest=ColorSpacesTest`
  Expected: PASS. (Review Focus #1 - the clamp is unit-pinned.)
- [ ] **Step 5: Commit.**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/color jipipe-core/src/test/java/org/hkijena/jipipe/utils/color
git commit -m "feat: color space math - RGB/HSB/LAB with sRGB gamut clamp (#1339)"
```

---

### Task 7: Modern color dialog + `selectColor` delegation

**Files:**

- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/color/JIPipeDesktopColorChooserDialog.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/color/JIPipeColorHistory.java`
  (recents + saved persistence)
- Create: `jipipe-core/src/main/resources/org/hkijena/jipipe/resources/colors/gnome-hig.json` (12 hues × 5 steps,
  names + hex - real values from developer.gnome.org/hig, e.g. Blue 3 `#3584e4`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/UIUtils.java` (`selectColor`, lines 2636–2638)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/color/JIPipeColorHistoryTest.java`

**Interfaces:**

- Consumes: `ColorSpaces` (Task 6); `ResourceUtils` for the palette file; app-settings/user-dir JSON (see
  `JIPipeUserDirManager` or the existing app-settings pattern - use whichever the repo uses for small per-user state).
- Produces:
    -
    `JIPipeDesktopColorChooserDialog.show(Component parent, String title, Color initial, boolean withTransparency): Color` -
    **static**, modal, returns the chosen color or `null` (same contract as today's `selectColor`, so the one-line
    delegation in `UIUtils` is the only call-site change).
    - `JIPipeColorHistory.get(): JIPipeColorHistory` with `List<Color> getRecent()`, `void remember(Color)`,
      `List<NamedColor> getSaved()`, `void save(NamedColor)`, `void removeSaved(NamedColor)` - pure persistence logic
      (testable headless).

- [ ] **Step 1: Write the failing history test.**
  `JIPipeColorHistory` persistence: `remember(c)` puts it first, dedupes, caps at ~8;
  `save(NamedColor("my red", #e01b24))` persists + round-trips; `removeSaved` works. Use a temp user-dir (check how
  other tests isolate user state; if none, make `JIPipeColorHistory`'s storage path injectable via a package-private
  ctor for the test).
  Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeColorHistoryTest`
  Expected: compile failure.
- [ ] **Step 2: Implement `JIPipeColorHistory`** (+ the injectable path) and the GNOME HIG palette resource (parse once,
  static list of `NamedColor`).
- [ ] **Step 3: Run history test → PASS.**
- [ ] **Step 4: Build the dialog.**
  Modal `JDialog` (~380×560). Single `Color` state (with alpha channel). Space tabs RGB / HSB / LAB:
    - RGB: 4 sliders (R,G,B,A when `withTransparency`).
    - HSB: 2D well (sat × value at current hue) + hue/sat/bright rails.
    - LAB: 2D a/b plane (crosshair axes + corner tags) + L rail (0–100) + a/b numeric rails; **uses
      `ColorSpaces.fromLab` so out-of-gamut clamps live** and the hex readout always shows the valid sRGB.
    - Always: hex field (`#RRGGBB`/`#RRGGBBAA`, two-way), swatch on checkerboard when alpha < 255.
    - Palettes section: Pure colors, GNOME HIG (name tooltips), Last selected (`JIPipeColorHistory.getRecent()`), Saved
      (star-to-save button; right-click deletes).
    - OK applies `remember(...)` + returns; Cancel returns null. All mouse-driven widgets are plain Swing (JComponent
      with mouse listeners) - no new deps.
- [ ] **Step 5: Delegate `UIUtils.selectColor`.**
  Replace the `JColorChooser.showDialog` one-liner with
  `JIPipeDesktopColorChooserDialog.show(parent, title, defaultValue, withTransparency)`.
- [ ] **Step 6: Compile + full suite.**
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS.
- [ ] **Step 7: Screenshot verification (VLM).**
  Harness + `da_describe_image` on `dialog-color.png` (pair vs. the `JColorChooser` baseline):
    - Space tabs RGB / HSB / LAB visible; hex field + swatch present; palette rows (pure colors, named GNOME HIG grid,
      recents, saved) below the picker.
    - LAB tab: a/b plane with axes + L rail (switch the harness's dialog screenshot to render the LAB tab as a second
      file `dialog-color-lab.png` - extend `--only` as needed).
      Expected: modern picker present, no Swing-internal chooser.
      Then manual (user checks in app):
    - Color parameter (e.g. "Canvas background" on Stack to montage): switch spaces; type a hex; pick from GNOME HIG;
      save one; reopen - it's in Saved and Last selected.
    - Alpha-capable color parameter: α rail + checkerboard visible; 8-digit hex round-trips. (Review Focus #2)
    - LAB: drag a/b to extremes - hex shows a clamped valid color, no crash. (Review Focus #1)
- [ ] **Step 8: Commit.**

```bash
git add -A jipipe-core/src/main jipipe-core/src/test
git commit -m "feat(desktop): modern multi-space color dialog (RGB/HSB/LAB) with palettes and history (#1339)"
```

---

### Task 8: `JIPipeFontParameter` model + serialization

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontParameter.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java` (register
  parameter type `jipipe:font`)
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontParameterTest.java`

**Interfaces:**

- Consumes: `Color` JSON handling (existing pattern - check how `Color` parameters serialize in `JsonUtils`; reuse the
  same codec so hex/array formats stay consistent), `Font` idioms from `FontStyleParameter.toFont(...)`.
- Produces:
    - `public class JIPipeFontParameter` (plain POJO, copy-able, `equals`/`hashCode`):
        - fields `String family; int size; int style; Color color` (all four always present)
        - `JIPipeFontParameter()` - defaults: `family = new Font("Dialog", Font.PLAIN, 12).getFamily()`, `size = 12`,
          `style = Font.PLAIN`, `color = Color.WHITE`
        - `java.awt.Font toFont()`
        - `JIPipeFontParameter copy()`
        - `void migrateFrom(String family, int size, int style, Color color)` - explicit constructor path used by node
          migrations (Task 10)
    - Registered parameter type id: **`jipipe:font`** (string literal) in `StandardParametersPlugin.register(...)` -
      **type only** in this task; the editor registration happens in Task 9 (after the model exists, before the
      migration in Task 10, so migrated nodes have a UI when they land).

- [ ] **Step 1: Write the failing serialization test.**
  Round-trip through `JsonUtils.getObjectMapper()`: a `JIPipeFontParameter` with all four fields set serializes and
  deserializes to an equal instance; a JSON with a missing `family` key deserializes with the default family (no NPE);
  `toFont()` returns the right size/style.
- [ ] **Step 2: Run → FAIL (class missing).**
  Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeFontParameterTest`
- [ ] **Step 3: Implement the model + register the type.**
- [ ] **Step 4: Run → PASS.**
- [ ] **Step 5: Commit.**

```bash
git add -A jipipe-core/src/main jipipe-core/src/test
git commit -m "feat: dedicated FontParameter (family+size+style+color) with migration constructor (#1339)"
```

---

### Task 9: Font parameter editor (preview row + popout)

**Files:**

- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopFontParameterEditorUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java` (register
  editor for `JIPipeFontParameter`)

**Interfaces:**

- Consumes: `JIPipeFontParameter` (Task 8), `JIPipeDesktopRichSelectionBox` (family search popup, single-select),
  `UIUtils.selectColor` → new dialog (Task 7), `JIPipeColorHistory` (recent colors in the embedded row), number
  formatting from `JIPipeDesktopNumberParameterEditorUI`.
- Produces: registered editor for parameter type `jipipe:font`. Inline row = one clickable preview control (sample text
  "Aa Label text 12" rendered in the current font, on the control's background). Popout = `JPopupMenu`-style panel
  (reuse the RichSelectionBox popup sizing family, 300×~420): preview band top; Family (button → search popup of system
  families grouped serif/sans/mono/other via `GraphicsEnvironment.getAvailableFontFamilyNames()`); Size (field +
  spinner); Style (B/I toggle buttons); Color (current swatch + pure-colors mini-row + `JIPipeColorHistory` recents +
  `Custom…` button). **`Custom…`/swatch click → hide the popout first, then `UIUtils.selectColor(...)`** (no stacked
  popups); on return, re-open the popout anchored at the same row (if that proves awkward in Swing, accept
  popout-stays-closed + updated inline preview - the user-visible contract is "no two stacked popups, value applied").
  Live: every change updates preview + `setParameter`.
    - No UI-structure test (policy); the logic worth testing (color history, font model) is already covered in Tasks
      7/8.
    - At this task no node uses `jipipe:font` yet (migration is Task 10) - behavior is verified there.

- [ ] **Step 1: Implement the editor** per the interface.
- [ ] **Step 2: Register** in `StandardParametersPlugin` (type registered in Task 8; editor now).
- [ ] **Step 3: Compile + full suite.**
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS.
- [ ] **Step 4: Screenshot smoke test.**
  Temporarily render the editor in the harness (e.g. a showcase entry over a parameter collection holding one
  `JIPipeFontParameter`) and `da_describe_image` it: the row shows the preview control, not a bunched cluster. (The full
  panel screenshot lands with Task 10's `panel-draw-text-roi.png`.)
- [ ] **Step 5: Commit.**

```bash
git commit -am "feat(desktop): FontParameter editor - inline preview + popout with embedded colors (#1339)"
```

---

### Task 10: Font parameter node migration

**Files:**

- Modify (pattern, repeated per node): each affected node/parameter collection:
    -
    `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/draw/DrawTextRoiAlgorithm.java`
    (reference implementation - do this one fully, others follow)
    - `.../nodes/roi/generate/TableToTextROIAlgorithm.java`
    - `.../nodes/roi/convert/RoiToRGBAlgorithm.java`
    - `.../nodes/calibration/DrawScaleBarAlgorithm.java`, `.../nodes/roi/draw/DrawScaleBarRoiAlgorithm.java`,
      `.../nodes/calibration/ScaleBarGenerator.java`
    - `.../utils/MontageCreator.java` (nested `LabelParameters` collection)
    - `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/canvasnotes/nodes/DefaultTextBoxGraphCanvasNote.java`
    - `plugins/jipipe-plugin-ij/src/main/java/org/hkijena/jipipe/plugins/imagejdatatypes/util/RoiDrawer.java` (only if
      it is a parameter collection - check first)
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/library/primitives/FontParameterMigrationTest.java`

**Interfaces:**

- Consumes: `JIPipeFontParameter` (Task 8), `JIPipeGraphNode.fromJson` →
  `onDeserialized(JsonNode, issues, notifications)` hook (verified: called after
  `ParameterUtils.deserializeParametersFromJson`, with the raw node JSON), `ParameterUtils` unique-key format (parameter
  keys as stored in node JSON, e.g. `label-font-family`).
- Produces: per node - one `JIPipeFontParameter` field with `@JIPipeParameter("<node>-font")` (exact key chosen per
  node, documented in the diff), `onDeserialized` override composing legacy keys when present. `FontFamilyParameter`/
  `FontStyleParameter` stay registered (other/old projects still load); migrated nodes stop referencing them. The Task 9
  editor renders the new parameter automatically.

**Per-node procedure (identical):**

1. Record the node's legacy parameter keys (family/size/style/color) from its current getters' `@JIPipeParameter`.
2. Replace the fields + getters/setters with one `JIPipeFontParameter` field, `@JIPipeParameter("<new-key>")`,
   `@SetJIPipeDocumentation(name = "…font")`.
3. Replace usages: `toFont()` (replaces the `fontStyle.toFont(family, size)` idiom) and `fontParameter.getColor()`
   (replaces the separate color field - if the node's color is used *elsewhere* than this font, keep the color field and
   pass it only into the migration composition).
4. `onDeserialized`: for each legacy key present in the raw JSON, read its value and call `migrateFrom(...)` with
   defaults for the absent keys (family default, size default 12, style default plain, color default white). After the
   next save the legacy keys are gone (serialization writes only current keys) → idempotent.

- [ ] **Step 1: Write the failing migration test** (reference node `DrawTextRoiAlgorithm`):
  Build the algorithm instance, serialize it with the *new* key, then take that JSON, **remove the new key and add the
  legacy split keys** (family/size/style/color), and `fromJson` it → assert the composed `JIPipeFontParameter` equals
  the expected values. Second variant: JSON with the new key only → unchanged (idempotent, legacy branch not taken).
  Third: legacy JSON missing the family key → default family, no NPE. (Review Focus #5.)
  Run: `mvn -q -pl jipipe-core test -Dtest=FontParameterMigrationTest`
  Expected: FAIL (node's new field/getter don't exist yet - implement the reference node's field first if needed for
  compilation; the assertion fails until `onDeserialized` exists).
- [ ] **Step 2: Implement the reference node migration** per the procedure.
- [ ] **Step 3: Run → PASS.**
- [ ] **Step 4: Apply the same procedure to the remaining nodes** (batch; each is a mechanical change).
- [ ] **Step 5: Grep gate** - no *node* (anything under `nodes/` or a registered parameter collection) references
  `FontFamilyParameter`/`FontStyleParameter` anymore:
  Run:
  `grep -rln "FontFamilyParameter\|FontStyleParameter" --include=*.java plugins/*/src/main jipipe-core/src/main | grep -v "library/primitives\|StandardParametersPlugin\|FontStyleParameter.java"`
  Expected: empty (or only non-parameter utilities that don't persist).
- [ ] **Step 6: Compile + full suite.**
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS.
- [ ] **Step 7: Screenshot verification (VLM).**
  Add `panel-draw-text-roi.png` to the harness showcase set (over `DrawTextRoiAlgorithm`, `ij1-roi-draw-text`).
  Harness + `da_describe_image` (pair): the split font rows ("Font family" / "Font size" / "Font style" / color) are
  gone, replaced by **one "…font" row with the preview control** (Task 9).
  Then manual: open a pre-migration project (user has some, e.g. an old montage/scale-bar pipeline) → font settings
  intact; save → legacy keys gone from the JSON; popout works (family search, size, style, embedded color + Custom →
  full dialog, no stacking); preview updates live. (Review Focus #5.)
- [ ] **Step 8: Commit** (one commit per 2–3 nodes is fine; message
  `feat: migrate <node> font params to FontParameter (#1339)`).

---

### Task 11: List editors - footer chrome + reorder behavior

**Files:**

- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopParameterCollectionListEditorUI.java`

**Interfaces:**

- Consumes: Task 2 layout (name from label column); existing row machinery (grip selection, `createEditorInstance` with
  `JIPipeListParameterItemParameterAccess`, `@ListParameterSettings.withScrollBar`).
- Produces: both list editors with:
    - compact **footer**: `＋ Add` button, count label (`"<n> <singular/plural>"`), `⋯` menu (Select all / Invert / Clear
      list); the old always-visible Add/Remove/Reorder toolbar is removed.
    - rows unchanged in substance: grip (click / Ctrl / Shift select + highlight), per-row item editor, per-row delete
      (hover ✕ + right-click menu Delete/Move up/Move down). Reorder = existing drag via grip (the `reorderModeButton`
      global toggle is removed; drag-reorder and the context menu remain the reorder paths - if drag-reorder does not
      already exist, implement it as a transfer handler on the rows; keep the context menu as the guaranteed path).
    - empty list: existing empty hint.

- [ ] **Step 1: Rework `JIPipeDesktopListParameterEditorUI`** per the interface.
- [ ] **Step 2: Rework `JIPipeDesktopParameterCollectionListEditorUI`** identically (rows are nested mini form panels -
  only chrome changes).
- [ ] **Step 3: Compile + full suite.**
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS.
- [ ] **Step 4: Screenshot verification (VLM).**
  Harness + `da_describe_image` on `panel-import-file-list.png` (pair): the always-visible Add/Remove/Reorder toolbar is
  gone; a compact footer (`＋ Add` + count + `⋯`) sits under the rows; rows keep grip + per-row editor; name comes from
  the label column.
  Then manual: add/remove/clear/invert/select-all; reorder two rows (drag and via right-click); per-row editors work;
  with-scroll lists keep their fixed height.
- [ ] **Step 5: Commit.**

```bash
git commit -am "feat(desktop): list parameter editors - compact footer, grip reorder (#1339)"
```

---

### Task 12: Consistency audit + final verification

**Files:**

- Modify: the remaining `*ParameterEditorUI*` classes as needed (56 in jipipe-core, 4 in plugins - audit list:
  `find jipipe-core/src/main plugins/*/src/main -name "*ParameterEditorUI*.java" -not -path "*/target/*"`)

**Interfaces:**

- Consumes: everything above.
- Produces: no new API. A consistent editor surface.

- [ ] **Step 1: Audit pass.** For each editor (skip the ones already reworked): confirm (a) no self-drawn name, (b)
  `isUIInline()` correct - `false` (stacked): list, collection-list, matrix, margin, fixed-margin, inner-margin, script;
  `true` (inline): everything else, **including the dynamic set** (its box is compact); (c) inline controls use the
  standard control border + 27px height family, (d) options column (`?` + `⋮`) present via the form panel, (e)
  expression editors keep their RSyntaxTextArea widget + `Edit` button but normalized to the inline row contract (#1124
  seam intact).
- [ ] **Step 2: Full build + tests.**
  Run: `mvn -q compile` (all modules)
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS everywhere.
- [ ] **Step 3: Final VLM sweep.**
  Re-run the harness on the full showcase set and walk every before/after pair with `da_describe_image` (one prompt per
  pair, same expectation list as the individual tasks) - a final consistency pass across all panels at once (catches
  cross-editor regressions the per-task checks miss).
- [ ] **Step 4: User verification pass** - the user walks the parameter panel in the app across a representative set of
  nodes (threshold, montage, file list, dynamic set node, expression/script node, settings sheets). Remaining nits
  become follow-ups, not blockers (standing ruling: the user checks the UI).
- [ ] **Step 5: Commit** (per audit cluster; `style(desktop): parameter editor consistency audit (#1339)`).
