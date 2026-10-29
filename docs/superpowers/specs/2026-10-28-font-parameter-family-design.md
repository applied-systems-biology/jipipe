# Font parameter family — design (#1339)

## Problem

`#1339` introduced a single `JIPipeFontParameter` (family + size + style + color)
used by every text-rendering node. But the intent was **distinct parameter classes
for each combination of font attributes a node actually needs** (font, font+style,
font+color, font+style+color). The merged class leaks a color into nodes whose text
color legitimately comes from elsewhere (e.g. the ROI's line color), and a style
into nodes that only set family/size.

Canonical example: **Convert 2D ROI to RGB** (`RoiToRGBAlgorithm`) has a label *font* (family + size, maybe style) but
its label *color* belongs to the ROI / an
optional override — not the font value.

## Investigation: how the label text color is actually derived

Traced every `JIPipeFontParameter` consumer:

| Node (param key)                                                                                              | font fields genuinely used | where the **text color** comes from today                                                                                                                                                                                       |
|---------------------------------------------------------------------------------------------------------------|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RoiToRGBAlgorithm` (`label-font`)                                                                            | family, size, style        | **`labelFont.getColor()`** (always the font's color). But the node already computes a per-ROI *effective line color* (`overrideLineColor` if enabled, else `roi.getStrokeColor()`) — that is the color the label should follow. |
| `DrawTextRoiAlgorithm` (`font`)                                                                               | family, size, style        | **NOT the font.** The `TextRoi`'s color comes from the registered sub-param `VisualLocationROIProperties.lineColor` (`OptionalColorParameter`, default enabled/yellow). `font.getColor()` is never called.                      |
| `TableToTextROIAlgorithm` (`font`)                                                                            | family, size, style        | **NOT the font.** Same as above (`VisualROIProperties.lineColor`).                                                                                                                                                              |
| `DrawScaleBarAlgorithm` (`text-settings/font`)                                                                | family, size, style, color | `textSettings.getFont().getColor()` (default BOLD WHITE).                                                                                                                                                                       |
| `DrawScaleBarRoiAlgorithm` (`text-settings/font`)                                                             | family, size, style, color | same as above.                                                                                                                                                                                                                  |
| `MontageCreator.LabelParameters` (`label-font`; used by `StackToMontage2Algorithm` + `InputImagesToMontage2`) | family, size, color        | `labelParameters.labelFont.getColor()`.                                                                                                                                                                                         |
| `DefaultTextBoxGraphCanvasNote` (`title-style/font`, `content-style/font`)                                    | family, size, style, color | `titleStyle.getColor()` / `contentStyle.getColor()` (default BLACK).                                                                                                                                                            |

Consequence: only the scale-bar nodes, the montage nodes, and the canvas note
truly need **font+style+color**. `RoiToRGB` needs **font+style** (color from ROI /
optional). `DrawTextRoi`/`TableToTextROI` need **font+style** (color already in
`VisualLocationROIProperties`). No current node uses exactly font+color or bare
font — those classes complete the family for future nodes.

## Design

### 1. The class family (all 4 combinations)

All in `org.hkijena.jipipe.plugins.parameters.library.primitives`. Each class has a
stable string-literal registration ID (persisted in saved projects — never
code-derived).

| Class                                     | Fields                     | Registration ID                       |
|-------------------------------------------|----------------------------|---------------------------------------|
| `JIPipeFontFamilySizeParameter`           | family, size               | `jipipe:font-family-size`             |
| `JIPipeFontFamilySizeStyleParameter`      | family, size, style        | `jipipe:font-family-size-style`       |
| `JIPipeFontFamilySizeColorParameter`      | family, size, color        | `jipipe:font-family-size-color`       |
| `JIPipeFontFamilySizeStyleColorParameter` | family, size, style, color | `jipipe:font-family-size-style-color` |

**Naming rationale:** the base `FontFamilySize` makes explicit that the *family* is
always part of the value (a bare `FontSizeParameter` would read as "a font size
number"); `Style` / `Color` are appended only for the combinations that carry
them. **All four registration IDs are new and consistent** — the branch is
unmerged, so the earlier development ID `jipipe:font` is simply replaced (user
ruling). The existing class is renamed `JIPipeFontParameter` →
`JIPipeFontFamilySizeStyleColorParameter`.

**Persistence note:** a parameter access is persisted as `field-class-id` = the
parameter type's **registration ID** (`JIPipeMutableParameterAccess`), and on load
resolved via `JIPipe.getParameterTypes().getInfoById(...)` — which throws
`Unknown parameter type ID` for an id that no longer exists. So changing the ID
means projects saved *during this branch's development* (carrying
`field-class-id: "jipipe:font"`) fail to load until re-saved. Accepted consequence (branch unmerged, per user ruling).
**Pre-#1339 legacy projects are unaffected**:
they contain no `label-font`/`font` access at all — only the split legacy keys (`label-font-family`, `label-size`, …),
which the per-node `onDeserialized`
migration reads directly from the raw JSON.

**Common abstraction** so the adaptive editor and `toFont()` callers depend on one
type:

```java
public interface JIPipeFontValue {
    String getFamily(); void setFamily(String family);
    int getSize();       void setSize(int size);
    Font toFont();                       // style-less classes use Font.PLAIN
    JIPipeFontValue copy();
}
// optional capabilities the editor probes with instanceof:
public interface JIPipeFontStyleCapable { int getStyle(); void setStyle(int s); }
public interface JIPipeFontColorCapable { Color getColor(); void setColor(Color c); }
```

Each class is a plain Jackson-serializable value (as the current
`JIPipeFontParameter` is): non-null fields with defaults, lenient deserialization (missing key → default), `equals`/
`hashCode`/`copy()`.

### 2. One adaptive editor

`JIPipeDesktopFontParameterEditorUI` becomes
`JIPipeDesktopFontParameterEditorUI<T extends JIPipeFontValue>` and shows exactly
the sections the concrete value supports:

- **family** box — always
- **size** spinner — always (the sample text's trailing number is the live size,
  the item-7 fix)
- **style** B/I toggles — only if the value `instanceof JIPipeFontStyleCapable`
- **color** section (swatch, pure colors, recents, Custom…) — only if the value
  `instanceof JIPipeFontColorCapable`

`reload()`, `getPopoutContent()` and `previewBackground()` are reused; only the
`buildStyleToggles()` / `buildColorSection()` calls are gated, and the preview
color used for the auto-contrast background falls back to a neutral tone when the
class has no color.

Because the parameter-type → editor-UI mapping is by class, four **thin** editor
subclasses are registered (each just `super(XxxFontParameter.class, params)`); the
shared logic lives in the generic base. `getParameter()` already resolves the
concrete value via the access's field class, so the base works unchanged.

### 3. The per-node remapping

| Node                                              | before                                                                     | after                                                                                                                 |
|---------------------------------------------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `RoiToRGBAlgorithm`                               | `JIPipeFontParameter labelFont`, label foreground = `labelFont.getColor()` | `JIPipeFontFamilySizeStyleParameter labelFont` + new `OptionalColorParameter labelColor` (default **disabled**)       |
| `DrawTextRoiAlgorithm`                            | `JIPipeFontParameter font`                                                 | `JIPipeFontFamilySizeStyleParameter font` (color was already from `VisualLocationROIProperties` — no behavior change) |
| `TableToTextROIAlgorithm`                         | `JIPipeFontParameter font`                                                 | `JIPipeFontFamilySizeStyleParameter font` (same)                                                                      |
| `DrawScaleBarAlgorithm` `TextSettings`            | `JIPipeFontParameter font`                                                 | `JIPipeFontFamilySizeStyleColorParameter font`                                                                        |
| `DrawScaleBarRoiAlgorithm` `TextSettings`         | `JIPipeFontParameter font`                                                 | `JIPipeFontFamilySizeStyleColorParameter font`                                                                        |
| `MontageCreator.LabelParameters`                  | `JIPipeFontParameter labelFont`                                            | `JIPipeFontFamilySizeStyleColorParameter labelFont`                                                                   |
| `DefaultTextBoxGraphCanvasNote` (title + content) | `JIPipeFontParameter font` ×2                                              | `JIPipeFontFamilySizeStyleColorParameter font` ×2                                                                     |

`JIPipeFontFamilySizeParameter` (bare font) is defined for completeness but not yet used.

### 4. `RoiToRGBAlgorithm` label-color mechanism (the actual fix)

The label foreground becomes: **`labelColor` when enabled, otherwise the ROI's
effective line color** (i.e. `overrideLineColor` if enabled, else
`roi.getStrokeColor()` — the exact color the ROI outline is drawn with).

`RoiDrawer` today takes one fixed `labelForeground` `Color`. It gains a
label-color mode:

```java
enum RoiLabelColorMode { FIXED, FROM_ROI_LINE }
setLabeLColorMode(mode)  // default FIXED (legacy behavior for the standalone usages)
```

In `drawResult`/`drawRoiOnProcessor` (where the effective line color is already
computed for the outline), the per-ROI label foreground is:
`mode == FROM_ROI_LINE ? effectiveLineColor : labelForeground`.

`RoiToRGBAlgorithm` sets:

- `labelColor.isEnabled()` → `FIXED` + `setLabelForeground(labelColor.getContent())`
- else → `FROM_ROI_LINE`

The standalone `RoiDrawer` usages (image-viewer settings, ROI manager) are
unaffected: they keep `FIXED` with their existing `labelForeground` (default white).

> Behavior note: a *fresh* `Convert 2D ROI to RGB` now draws labels in the ROI's
> line color instead of the font's (default white) color. That is the intended
> "color is from the roi" outcome; the old white can still be forced by enabling
> `labelColor`.

### 5. Migration

The branch is unmerged, so the only real-world projects are **pre-#1339 legacy**
(split `label-font-family` / `label-size` / `label-foreground` / `font-family` /
`font-style` / `text-color` keys). Each node's existing `onDeserialized` +
`composeFromLegacy` builds the correct concrete class:

- `RoiToRGBAlgorithm`: legacy keys → `JIPipeFontFamilySizeStyleParameter` (no color). A
  legacy `label-foreground`, if present, is written into the **new** `labelColor`
  **enabled** with that color (old label colors preserved). A *new* node (no
  legacy keys) gets `labelColor` **disabled** (label follows the ROI line color).
- `DrawTextRoi` / `TableToTextROI`: legacy family/size/style →
  `JIPipeFontFamilySizeStyleParameter`.
- scale-bar / montage / canvas-note: legacy family/size/style/color →
  `JIPipeFontFamilySizeStyleColorParameter`.

`JIPipeFontParameter.composeFromLegacy` generalizes to target a concrete class (e.g. a
`composeFromLegacy(..., targetClass)` overload, or a per-class
`fromLegacy` static).

## Files

**New (jipipe-core, `plugins/parameters/library/primitives/`):**

- `JIPipeFontValue`, `JIPipeFontStyleCapable`, `JIPipeFontColorCapable`
- `JIPipeFontFamilySizeParameter`
- `JIPipeFontFamilySizeStyleParameter`
- `JIPipeFontFamilySizeColorParameter`
- `JIPipeFontFamilySizeStyleColorParameter` (renamed `JIPipeFontParameter`)

**Modified (jipipe-core):**

- `JIPipeDesktopFontParameterEditorUI` → generic adaptive base + 4 thin subclasses
- `StandardParametersPlugin` → register the 4 types under the 4 new IDs (`jipipe:font-family-size[-style][-color]`); the
  development-only `jipipe:font`
  registration is replaced
- `DefaultTextBoxGraphCanvasNote` → remap ×2 to the full type + migration
- tests: `JIPipeFontParameterTest` / `FontParameterMigrationTest` follow the rename;
  round-trip tests added for the 3 new classes

**Modified (plugins/jipipe-plugin-ij):**

- `RoiDrawer` → `RoiLabelColorMode` + per-ROI effective-line-color label

**Modified (plugins/jipipe-plugin-ij-algorithms):**

- `RoiToRGBAlgorithm` → size-style font + `labelColor` + drawer mode + migration
- `DrawTextRoiAlgorithm`, `TableToTextROIAlgorithm` → size-style font + migration
- `DrawScaleBarAlgorithm`, `DrawScaleBarRoiAlgorithm` → full type + migration
- `MontageCreator` → full type + migration
- `DrawTextRoiFontMigrationTest` updated

## Testing (TDD for the non-trivial pure logic; no Swing-structure tests)

- Round-trip + lenient-deserialization + `toFont()` for each of the 4 classes (extend the existing
  `JIPipeFontParameterTest` pattern).
- `RoiDrawer` label-color mode: a pure unit test that `FROM_ROI_LINE` yields the
  effective line color per ROI and `FIXED` yields the explicit color (and that the
  default remains `FIXED`, so standalone usages are unchanged).
- Migration: each node's `onDeserialized` composes the right concrete class from
  legacy keys (extend `FontParameterMigrationTest` + the ij-algorithms migration
  test). `RoiToRGB` specifically: legacy `label-foreground` lands in `labelColor`
  **enabled**, the font value carries **no** color, and a fresh node has
  `labelColor` disabled.
- Editor adaptivity verified via the screenshot harness: render the full type (color section present) and the size-style
  type (color section absent) through a
  synthetic showcase; VLM-confirmed.

## Decisions (all resolved — approved for implementation)

1. **Resolved (2026-10-28):** rename `JIPipeFontParameter` →
   `JIPipeFontFamilySizeStyleColorParameter` and move it to the new
   `jipipe:font-family-size-style-color` ID (the development `jipipe:font` ID is
   dropped — branch unmerged, per user ruling).
2. **Resolved (2026-10-28):** include **all four** classes now — including the two
   currently unused (`JIPipeFontFamilySizeParameter`,
   `JIPipeFontFamilySizeColorParameter`) — to complete the combination set.
3. **Resolved (2026-10-28):** `RoiToRGBAlgorithm` fresh-node default = label
   follows the ROI line color, overridable by the new `labelColor`. Confirmed by
   the user.
