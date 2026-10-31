# Font Parameter Family Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single merged `JIPipeFontParameter` with a family of four parameter classes (font / +style /
+color / +style+color), remap every consuming node to the class matching its real needs, and fix `RoiToRGBAlgorithm`'s
label color to follow the ROI line color with an optional override.

**Architecture:** Four Jackson value classes share a `JIPipeFontValue` interface plus two capability markers
(`JIPipeFontStyleCapable`, `JIPipeFontColorCapable`); one adaptive editor UI (generic in the value type) gates its
style/color sections by `instanceof`. `RoiDrawer` gains a label-color mode so `RoiToRGBAlgorithm` can render each label
in its ROI's effective line color.

**Tech Stack:** Java 21, Maven multi-module, Jackson, JUnit 5, Swing (headless-testable logic only; UI verified via the
screenshot harness + VLM).

**Spec:** `docs/superpowers/specs/2026-10-28-font-parameter-family-design.md` (all open decisions resolved there
2026-10-28).

## Global Constraints

- **Java 21** (`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` - the system Java is 25).
- **Registration IDs are stable string literals** - the exact four, verbatim:
  `jipipe:font-family-size`, `jipipe:font-family-size-style`, `jipipe:font-family-size-color`,
  `jipipe:font-family-size-style-color`.
  The development ID `jipipe:font` is **deleted** (branch unmerged; user ruling).
- **No Swing-UI-structure tests** (project policy) - editor behavior is verified through the screenshot harness + VLM;
  pure logic (values, migration, drawer mode) gets JUnit tests.
- **Do not touch** `ImageJLegacyPatcherLauncherSessionListener` registrations; the ij plugin already has JUnit 5 + the
  launcher-listener service file.
- **Commit suffix** `(#1339)` on every commit; push branch `1339-parameter-ux-overhaul` only (no MR until the user says
  so).
- Maven output is German-locale ("Fehler" = error). Full core suite baseline: **622 tests, 0 failures, 1 skipped**
  (before this work).

## Review Focus

1. **Pre-#1339 legacy project load** - a node JSON with split keys (`label-font-family`, `label-size`,
   `label-foreground`, `font-family`, `font-style`, `text-settings/text-color`, `title-style/font-family`…) must load
   into the right concrete class, idempotently (second load no-ops). Pinned by the migration tests in Tasks 3 (prefixed
   canvas-note keys), 5 (draw-text keys + no color field), and 8 (RoiToRGB keys + label-foreground → labelColor).
2. **`field-class-id: "jipipe:font"` (branch-era dev saves)** - loading one must fail with the *registry* error
   `Unknown parameter type ID`, not a ClassCastException (the type is simply gone). Pinned by Task 2 Step 1
   (`FontRegistrationIdsTest`).
3. **`RoiDrawer` standalone usages unchanged** - image-viewer settings / ROI manager build a `RoiDrawer` without
   touching the new mode; their labels must stay `FIXED`/white. Pinned by Task 7 (`defaultModeIsFixed`) + the existing
   `RoiDrawer` copy-constructor contract.
4. **Per-ROI label color correctness in `RoiToRGBAlgorithm`** - with `labelColor` disabled and ROIs of different stroke
   colors, each label uses *its own* ROI's line color (override-aware); a single fixed color would be the bug. Pinned by
   Task 7's `fromRoiLineUsesTheRoisOwnLineColor` (two ROIs).
5. **Adaptive editor must not offer controls a class lacks** - a size-style value must not expose a color section (the
   color would be silently dropped/ignored), a family-size value must not expose style toggles. Pinned by Task 2 Step 7
   (VLM) - this is the spec's core contract.

---

### Task 1: Interfaces + the 3 lighter font classes (jipipe-core, additive)

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontValue.java`
- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontStyleCapable.java`
- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontColorCapable.java`
- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontFamilySizeParameter.java`
- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontFamilySizeStyleParameter.java`
- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontFamilySizeColorParameter.java`
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/library/primitives/FontParameterFamilyTest.java`

**Interfaces:**

- Produces (exact signatures later tasks compile against):

```java
public interface JIPipeFontValue {
    String getFamily();            void setFamily(String family);
    int getSize();                 void setSize(int size);
    java.awt.Font toFont();        JIPipeFontValue copy();
}
public interface JIPipeFontStyleCapable { int getStyle(); void setStyle(int style); }
public interface JIPipeFontColorCapable { java.awt.Color getColor(); void setColor(java.awt.Color color); }

public class JIPipeFontFamilySizeParameter implements JIPipeFontValue { ... }
public class JIPipeFontFamilySizeStyleParameter implements JIPipeFontValue, JIPipeFontStyleCapable { ... }
public class JIPipeFontFamilySizeColorParameter implements JIPipeFontValue, JIPipeFontColorCapable { ... }
```

- The existing `JIPipeFontParameter` is **untouched in this task** (it is renamed to the 4th class in Task 2). The new
  classes are pure additions → the module compiles and the whole suite stays green.
- Shared statics live in `JIPipeFontFamilySizeStyleParameter` (single owner, it has all the pieces):
  `public static String defaultFamily()`,
  `public static int styleNameToFlags(String styleName)`,
  `public static Color legacyColor(com.fasterxml.jackson.databind.JsonNode colorNode)`
  - **copied** from the current `JIPipeFontParameter` (the old class keeps its own copies until Task 2 deletes it; the 3
  style-less/color-less classes call `JIPipeFontFamilySizeStyleParameter.defaultFamily()`).
- Migration helper, one **concrete static per class** (no generics - each
  returns its own type; Task 2 gives the renamed full class the same):
  `public static JIPipeFontFamilySizeStyleParameter fromLegacyKeys(JsonNode json, String familyKey, String sizeKey, String styleKey, String colorKey, int defaultSize)`
  (same signature shape on the other classes, each returning its own type).
  Absent keys → defaults; `styleKey`/`colorKey` may be null (ignored by classes
  lacking that field). Built on the shared statics. This is the idempotent
  migration entry every node calls.
- The size-style class additionally gets the 3-arg
  `public void migrateFrom(String family, int size, int style)` (the no-color
  variant of the current class's `migrateFrom`) - nodes set their fresh defaults
  with it. The full (renamed, Task 2) class keeps the existing 4-arg
  `migrateFrom(String family, int size, int style, Color color)`.

- [ ] **Step 1: Write the failing test** - `FontParameterFamilyTest` (same package):
    - `sizeStyleRoundTrip()`: `JIPipeFontFamilySizeStyleParameter` with family "DejaVu Sans", size 24, style
      `BOLD|ITALIC` → serialize/deserialize via `JsonUtils.getObjectMapper()` → `assertEquals`; assert the JSON object
      has `family`/`size`/`style` keys and **no `"color"` key**.
    - `familySizeToFontIsPlain()`: family "Dialog", size 30 → `toFont()` size 30, style `Font.PLAIN`.
    - `familySizeColorRoundTrip()`: family/size/color → `assertEquals`; JSON has `"color"`, **no `"style"` key**.
    - `missingKeysFallBackToDefaults()`: `fromLegacyKeys` on `{}` → defaults per class.
    - `fromLegacyKeysComposesStyle()`: a JSON with family/size/style keys →
      `JIPipeFontFamilySizeStyleParameter.fromLegacyKeys` yields exact family/size/`styleNameToFlags` result (reuse the
      values from the current `FontParameterMigrationTest.composesAllLegacyKeys`); the color key is ignored by this
      class.
    - `fromLegacyKeysComposesColor()`: same for `JIPipeFontFamilySizeColorParameter` (family/size/color, style key
      ignored).

- [ ] **Step 2: Run test to verify it fails**

Run:
`cd /data/src/jipipe-3 && export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && mvn -pl jipipe-core test -Dtest=FontParameterFamilyTest`
Expected: compile error - classes do not exist yet.

- [ ] **Step 3: Implement** the 3 interfaces + 3 classes + `fromLegacyKeys` per the Interfaces block. Each class is a
  plain Jackson value: non-null fields with defaults (family = `defaultFamily()`, size 12, style `Font.PLAIN`, color
  `Color.WHITE`), `@JsonGetter/@JsonSetter` per field (same JSON key names as the current `JIPipeFontParameter`),
  lenient deserialization, `equals`/`hashCode`/`copy()`/`toString()` over exactly its own fields. `toFont()` for the
  style-less classes uses `Font.PLAIN`; for `JIPipeFontFamilySizeStyleParameter` it mirrors the current `toFont()`
  including the family-availability fallback.

- [ ] **Step 4: Run test to verify it passes, then the full suite**

Run: `mvn -pl jipipe-core test -Dtest=FontParameterFamilyTest` then `mvn -pl jipipe-core test`
Expected: both green (additive change - nothing else touched).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src
git commit -m "feat(core): font parameter family - interfaces + family-size / family-size-style / family-size-color classes (#1339)"
```

---

### Task 2: Rename the full class, registration, adaptive editor, core call sites (jipipe-core)

**Files:**

- Rename (git mv + edit):
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/JIPipeFontParameter.java` →
  `JIPipeFontFamilySizeStyleColorParameter.java` (class renamed; implement
  `JIPipeFontValue, JIPipeFontStyleCapable, JIPipeFontColorCapable`; add `fromLegacyKeys` identical to Task 1's pattern;
  **delete** the now-duplicated `defaultFamily`/`styleNameToFlags`/`legacyColor` - delegate to
  `JIPipeFontFamilySizeStyleParameter`'s; drop the old `composeFromLegacy` pair in favor of `fromLegacyKeys`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java:359-365` (the
  `jipipe:font` registration)
- Rename+adapt:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopFontParameterEditorUI.java` →
  generic base `JIPipeDesktopFontParameterEditorUI<T extends JIPipeFontValue>`; create 4 thin subclasses in the same
  package, the full one named `JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI`
- Modify (rename-only):
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/canvasnotes/nodes/DefaultTextBoxGraphCanvasNote.java` (Task 3
  refines its migration)
- Test: rename `JIPipeFontParameterTest` → `JIPipeFontFamilySizeStyleColorParameterTest` (keep its 4 tests, adapt refs);
  adapt `FontParameterMigrationTest` (its `composeFromLegacy` calls → `fromLegacyKeys` on the appropriate class); create
  `FontRegistrationIdsTest`

**Interfaces:**

- Consumes: Task 1's classes + interfaces + `fromLegacyKeys` pattern.
- Produces:
    - `StandardParametersPlugin` registers **four** types via the existing
      `registerParameterType(id, clazz, archetype, instanceCreator, copyCreator, name, description, editorUI)`
      overload - IDs verbatim per Global Constraints, all `JIPipeParameterArchetype.Value`, names "Font (family + size)
      ", "Font (family + size + style)", "Font (family + size + color)", "Font (family + size + style + color)", each →
      its thin editor. The `jipipe:font` registration is **deleted**.
    - `JIPipeDesktopFontParameterEditorUI<T extends JIPipeFontValue>` - generic base; constructor
      `(Class<T> clazz, InitializationParameters)`; keeps the `public JComponent getPopoutContent()` seam (the harness
      depends on it) and the `previewBackground`/`SAMPLE_TEXT_PREFIX` live-size logic (already on the branch).
    - Four thin editors: `JIPipeDesktopFontFamilySizeParameterEditorUI`, `…SizeStyle…`, `…SizeColor…`,
      `…SizeStyleColor…`, each one constructor.

- [ ] **Step 1: Write the failing test** - `FontRegistrationIdsTest`: after `JIPipe.ensureInstance()`, for each of the 4
  IDs assert `JIPipe.getParameterTypes().getInfoById(id) != null` and `getFieldClass()` equals the expected class;
  assert `getInfoById("jipipe:font") == null` (Review Focus #2: the dev ID is gone).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -Dtest=FontRegistrationIdsTest`
Expected: FAIL (IDs not registered).

- [ ] **Step 3: Rename the full class** (`JIPipeFontParameter` → `JIPipeFontFamilySizeStyleColorParameter`), make it
  implement the 3 interfaces, add `fromLegacyKeys`, drop the duplicated statics/`composeFromLegacy`.

- [ ] **Step 4: Register the 4 types** in `StandardParametersPlugin` (replace the single `jipipe:font` block) with the 4
  thin editors (make the current editor generic + gate: in `getPopoutContent()`,
  `boolean hasStyle = getParameter() instanceof JIPipeFontStyleCapable;` /
  `boolean hasColor = getParameter() instanceof JIPipeFontColorCapable;` - Style row only when `hasStyle`, Color row
  only when `hasColor`; the preview auto-contrast color = `((JIPipeFontColorCapable) getParameter()).getColor()` when
  `hasColor`, else `Color.BLACK`).

- [ ] **Step 5: Rename all remaining jipipe-core call sites** - `DefaultTextBoxGraphCanvasNote` (fields,
  `titleFontWithSize`, its `composeFromLegacy` → `fromLegacyKeys`), the two test files.
  `grep -rn 'JIPipeFontParameter\b' jipipe-core/src/main jipipe-core/src/test` must return nothing.

- [ ] **Step 6: Run the full jipipe-core suite**

Run: `mvn -pl jipipe-core test`
Expected: green (baseline 622 + new `FontRegistrationIdsTest`; renamed tests pass).

- [ ] **Step 7: VLM adaptivity check (Review Focus #5)** - the core editor is generic; verify the full type still
  renders all 4 sections: `cd /tmp/opencode/1339 && ./run.sh /tmp/opencode/1339/font9a --only panel-font,popup-font`
  then `da_describe_image`: preview "Aa Label text 24", popout rows Family/Size/Style/Color. (The size-style variant
  showcase comes in Task 9.)

- [ ] **Step 8: Commit**

```bash
git add -A jipipe-core/src
git commit -m "feat(core): font family registration (4 IDs, drop jipipe:font) + adaptive editor (#1339)"
```

---

### Task 3: Canvas note migration pin (jipipe-core)

**Files:**

- Modify:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/library/primitives/FontParameterFamilyTest.java`
- (Task 2 already renamed `DefaultTextBoxGraphCanvasNote` to the full type and its `onDeserialized` to `fromLegacyKeys`;
  this task only *pins* the prefixed-key migration + the title/content size defaults.)

**Interfaces:**

- Consumes: `JIPipeFontFamilySizeStyleColorParameter.fromLegacyKeys` (Task 2).
- Produces: none beyond the test.

- [ ] **Step 1 (TDD): add `canvasNoteStyleMigratesPrefixedKeys`** to `FontParameterFamilyTest` - a JSON object with
  `title-style/font-family: "Dialog"`, `title-style/font-size: 18`, `content-style/color: "#000000"` →
  `JIPipeFontFamilySizeStyleColorParameter.fromLegacyKeys(json, "title-style/font-family", "title-style/font-size", "title-style/font-style", "title-style/color", 18)`
  yields family "Dialog", size 18, color black, style PLAIN. (This is the exact key set + prefix the node's
  `onDeserialized` uses - if the node's keys ever drift, this test's inputs are the record.)
- [ ] **Step 2: Verify the node's defaults** - read `DefaultTextBoxGraphCanvasNote`: title font must default to size
  **18** (`titleFontWithSize(18)`) and content to **12**, both black; if Task 2's rename lost either default, restore
  it.
- [ ] **Step 3:** `mvn -pl jipipe-core test -Dtest=FontParameterFamilyTest` → green.
- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src
git commit -m "test(core): pin canvas-note prefixed font migration + title/content size defaults (#1339)"
```

---

### Task 4: Scale-bar remap (ij-algorithms)

**Files:**

- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/calibration/DrawScaleBarAlgorithm.java`
- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/draw/DrawScaleBarRoiAlgorithm.java`

**Interfaces:**

- Consumes: `JIPipeFontFamilySizeStyleColorParameter` (Task 1).
- Produces: none.

- [ ] **Step 1:** `TextSettings.font` → `JIPipeFontFamilySizeStyleColorParameter` in both; default stays
  `migrateFrom(defaultFamily(), 12, BOLD, WHITE)`; `onDeserialized` uses `fromLegacyKeys` with keys
  `text-settings/text-font`, `text-settings/text-font-size`, `text-settings/font-style`, `text-settings/text-color`,
  default size 12 (identical values to today).
- [ ] **Step 2:**
  `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am install -DskipTests 2>&1 | grep -aE 'ERROR|BUILD'`
  (compile check; `-am` builds jipipe-core first).
- [ ] **Step 3: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/calibration/ plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/draw/DrawScaleBarRoiAlgorithm.java
git commit -m "refactor(ij-alg): scale bar fonts use the explicit family type (#1339)"
```

---

### Task 5: DrawTextRoi + TableToTextROI remap (ij-algorithms)

**Files:**

- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/draw/DrawTextRoiAlgorithm.java`
- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/generate/TableToTextROIAlgorithm.java`
- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/test/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/draw/DrawTextRoiFontMigrationTest.java`

**Interfaces:**

- Consumes: `JIPipeFontFamilySizeStyleParameter` (Task 1).
- Produces: none.

- [ ] **Step 1:** `font` field → `JIPipeFontFamilySizeStyleParameter` in both (drop the unused color field entirely; the
  text color was already `VisualLocationROIProperties.lineColor` - no drawing change). `onDeserialized` keys unchanged
  (`font-family`, `font-size`, `font-style`, **no color key**, default size 12).
- [ ] **Step 2 (TDD): update `DrawTextRoiFontMigrationTest`** - its legacy-JSON → `getFont()` assertions now target
  `JIPipeFontFamilySizeStyleParameter`; add `migratedFontHasNoColorField`: after migration, serializing the node's
  `font` access yields JSON without a `"color"` key (the spec's core contract for these two nodes).
- [ ] **Step 3: Run** `mvn -pl plugins/jipipe-plugin-ij-algorithms test -Dtest=DrawTextRoiFontMigrationTest` → green.
- [ ] **Step 4: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms
git commit -m "refactor(ij-alg): draw-text/table-to-text ROI fonts drop the unused color field (#1339)"
```

---

### Task 6: Montage remap (ij-algorithms)

**Files:**

- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/utils/MontageCreator.java`
  (`LabelParameters.labelFont`)
- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/montage/InputImagesToMontage2.java` +
  `StackToMontage2Algorithm.java` (their `composeFromLegacy` calls)

**Interfaces:**

- Consumes: `JIPipeFontFamilySizeStyleColorParameter` (Task 1).
- Produces: none.

- [ ] **Step 1:** `LabelParameters.labelFont` → full type; default `migrateFrom(defaultFamily(), 12, PLAIN, WHITE)`;
  both nodes' `onDeserialized` use `fromLegacyKeys` with their current keys (`label-font-family`, `label-size`,
  `label-foreground`, default size 12, prefixed by the label-parameters path).
- [ ] **Step 2:** compile check (Task 4 Step 2 command).
- [ ] **Step 3: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms
git commit -m "refactor(ij-alg): montage label font uses the explicit family type (#1339)"
```

---

### Task 7: RoiDrawer label-color mode (ij)

**Files:**

- Modify: `plugins/jipipe-plugin-ij/src/main/java/org/hkijena/jipipe/plugins/imagejdatatypes/util/RoiDrawer.java`
- Test:
  `plugins/jipipe-plugin-ij/src/test/java/org/hkijena/jipipe/plugins/imagejdatatypes/util/RoiDrawerLabelColorModeTest.java`

**Interfaces:**

- Produces:

```java
public enum RoiLabelColorMode { FIXED, FROM_ROI_LINE }
public void setLabelColorMode(RoiLabelColorMode mode);      // default FIXED
public RoiLabelColorMode getLabelColorMode();
// package-private seam for the unit test - the per-ROI foreground the draw loops use:
Color effectiveLabelForeground(Roi roi);
```

`effectiveLabelForeground(roi)` returns `labelForeground` when `mode == FIXED`, else the effective line color - the
**same expression** the outline already uses:
`(overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor()`.
Copy-constructor copies the new field. **There are exactly THREE per-ROI label call sites** in `RoiDrawer`, each already
in a loop with `roi` in scope and the effective line-color expression computed nearby: the `drawResult` per-ROI loop (~
line 390), `drawRoiOnProcessor` (~line 516), and `drawRoiOnGraphics` (~line 676). Replace `labelForeground` with
`effectiveLabelForeground(roi)` in ALL three.

- [ ] **Step 1 (TDD): write the failing test** - build a `RoiDrawer` and real `ij.gui.Roi`s (the module's test infra
  handles the ImageJ patcher):
    - `fromRoiLineUsesTheRoisOwnLineColor` (Review Focus #4): two ROIs, stroke RED and stroke GREEN, `overrideLineColor`
      disabled, mode `FROM_ROI_LINE` → `effectiveLabelForeground(roiRed) == RED` and
      `effectiveLabelForeground(roiGreen) == GREEN` - a *single fixed* color would fail this.
    - `overrideLineWins`: `overrideLineColor` enabled BLUE → `effectiveLabelForeground` BLUE for both ROIs.
    - `fixedModeIgnoresRoi`: mode `FIXED` with `labelForeground` set to MAGENTA → MAGENTA for both ROIs.
    - `defaultModeIsFixed` (Review Focus #3): a fresh `new RoiDrawer()` reports `FIXED`.

- [ ] **Step 2: Run** `mvn -pl plugins/jipipe-plugin-ij test -Dtest=RoiDrawerLabelColorModeTest` → FAIL (method
  missing).

- [ ] **Step 3: Implement** the enum, field, setter/getter, copy, and `effectiveLabelForeground`; replace the per-ROI
  `labelForeground` usages with `effectiveLabelForeground(roi)` in ALL THREE call sites: the `drawResult` per-ROI loop
  (~line 390), `drawRoiOnProcessor` (~line 516), and `drawRoiOnGraphics` (~line 676) - see the Interfaces note above.

- [ ] **Step 4: Run** same command → PASS; then `mvn -pl plugins/jipipe-plugin-ij test` → whole module green.

- [ ] **Step 5: Commit**

```bash
git add plugins/jipipe-plugin-ij
git commit -m "feat(ij): RoiDrawer label color mode - fixed or from the ROI line color (#1339)"
```

---

### Task 8: RoiToRGBAlgorithm (ij-algorithms) - the user's canonical case

**Files:**

- Modify:
  `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/convert/RoiToRGBAlgorithm.java`

**Interfaces:**

- Consumes: `JIPipeFontFamilySizeStyleParameter` (Task 1), `RoiDrawer.RoiLabelColorMode` + `setLabelColorMode`/
  `setLabelForeground` (Task 7).
- Produces: a new parameter access `@JIPipeParameter("label-color")` `OptionalColorParameter getLabelColor()` (default
  `new OptionalColorParameter(Color.WHITE, false)`).

- [ ] **Step 1:** `labelFont` → `JIPipeFontFamilySizeStyleParameter`; `defaultLabelFont()` →
  `new JIPipeFontFamilySizeStyleParameter()` + `migrateFrom(defaultFamily(), 9, PLAIN)` (no color). Copy-constructor
  copies `labelFont` + the new `labelColor`.
- [ ] **Step 2:** in `runIteration`: replace `drawer.setLabelForeground(labelFont.getColor())` with
  ```java
  if (labelColor.isEnabled()) {
      drawer.setLabelColorMode(RoiDrawer.RoiLabelColorMode.FIXED);
      drawer.setLabelForeground(labelColor.getContent());
  } else {
      drawer.setLabelColorMode(RoiDrawer.RoiLabelColorMode.FROM_ROI_LINE);
  }
  ```
  (the `drawer.setLabelFont(labelFont.toFont())` line is unchanged).
- [ ] **Step 3:** add the `labelColor` accessors with docs ("If enabled, the label color is fixed to this value;
  otherwise each label uses the color of its ROI's line."); `onDeserialized` →
  `JIPipeFontFamilySizeStyleParameter.fromLegacyKeys(node, "label-font-family", "label-size", null, null, 9)` for the
  font **plus** (Review Focus #1): if `node.has("label-foreground")` set `labelColor` to that color **enabled** (read
  via `JIPipeFontFamilySizeStyleParameter.legacyColor(node.get("label-foreground"))`), else leave it disabled
  (fresh-node default, Review Focus #3).
- [ ] **Step 4 (TDD): test** `plugins/jipipe-plugin-ij-algorithms/.../RoiToRGBLabelColorTest.java` - deserialize a
  minimal node JSON (use the node's Jackson path like `DrawTextRoiFontMigrationTest` does): (a) legacy
  `label-foreground: "#FF0000"` → `getLabelColor()` enabled red, `getLabelFont()` is a
  `JIPipeFontFamilySizeStyleParameter`; (b) a fresh `new RoiToRGBAlgorithm(info)` → `getLabelColor()` **disabled**; (c)
  idempotency: re-deserializing the migrated JSON keeps `labelColor` red.
- [ ] **Step 5: Run** `mvn -pl plugins/jipipe-plugin-ij-algorithms test -Dtest=RoiToRGBLabelColorTest` → green;
  module-wide `mvn -pl plugins/jipipe-plugin-ij-algorithms test` → green.
- [ ] **Step 6: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms
git commit -m "feat(ij-alg): Convert 2D ROI to RGB - label font without color, label follows the ROI line color (optional override) (#1339)"
```

---

### Task 9: Harness, verification, full-suite (jipipe-launcher + all)

**Files:**

- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java`

**Interfaces:**

- Consumes: everything. Produces: final verification evidence.

- [ ] **Step 1:** rename `JIPipeFontParameter` refs in the harness → `JIPipeFontFamilySizeStyleColorParameter`
  (`FontParameterShowcase`, the `panel-font` showcase, `renderFontPopout`'s cast - the cast target becomes the full-type
  **thin** editor `JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI`, whose `getPopoutContent()` seam is
  unchanged).
- [ ] **Step 2:** add showcases:
    - `PER_EDITOR.put("editor-font-family-size-style", JIPipeFontFamilySizeStyleParameter.class)` (adaptivity check: no
      color section)
    - `SHOWCASE.put("panel-convert-2d-roi-to-rgb", "ij1-roi-to-rgb")` - the `RoiToRGBAlgorithm` node (id verified in
      `ImageJAlgorithmsPlugin`) - so the VLM can see the node's "Label font" (size-style type) + new "Label color" rows
    - extend `renderFontPopout` with a second id `popup-font-size-style` rendering the size-style editor's
      `getPopoutContent()` (build via
      `JIPipe.getParameterTypes().createEditorInstanceWithEmbeddedValue(workbench, JIPipeFontFamilySizeStyleParameter.class)`
      like the existing full-type render).
- [ ] **Step 3: build + render**

Run:

```bash
cd /data/src/jipipe-3 && export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -q install -DskipTests 2>&1 | grep -aE 'ERROR|BUILD'
cd /tmp/opencode/1339 && ./run.sh /tmp/opencode/1339/font9 --only panel-font,popup-font,popup-font-size-style,editor-font-family-size-style,panel-convert-2d-roi-to-rgb
```

- [ ] **Step 4: VLM-verify** (`da_describe_image`):
    - `panel-font`: preview shows "Aa Label text 24" (live size), full popout shows Family/Size/Style/ **Color**.
    - `popup-font-size-style` / `editor-font-family-size-style`: Family + Size + Style rows, **no Color section**
      (Review Focus #5).
    - `panel-convert-2d-roi-to-rgb`: the node panel has a "Label font" row (adaptive editor, **no color section in its
      popout** - verify via a crop if needed) and a separate "Label color" row with the optional checkbox.
- [ ] **Step 5: full suites**

Run:

```bash
cd /data/src/jipipe-3 && export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -pl jipipe-core test 2>&1 | grep -aE 'Tests run: [0-9]+, Failures|BUILD'
mvn -pl plugins/jipipe-plugin-ij test 2>&1 | grep -aE 'Tests run: [0-9]+, Failures|BUILD'
mvn -pl plugins/jipipe-plugin-ij-algorithms test 2>&1 | grep -aE 'Tests run: [0-9]+, Failures|BUILD'
```

Expected: all green (core ≥ 622 + new tests; the two plugin modules green).

- [ ] **Step 6: Commit + push**

```bash
git add -A
git commit -m "test(desktop): font family - harness showcases + full-suite verification (#1339)"
git push origin 1339-parameter-ux-overhaul
```

---

## Sequencing note

Task 1 is purely additive (old class untouched → whole core suite green at its
boundary). Task 2 is the breaking step (rename + registration + core call sites)
but ends green within the module. Tasks 3–8 are independent of each other after
Task 2 (any order) but all depend on 1–2, and Task 8 depends on 7 (the drawer
mode). Task 9 is last (harness + verification). If executing subagent-driven, the
natural reviewer gates are: after 2 (core green), after 7 (drawer contract),
after 8 (the behavior change), and 9.
