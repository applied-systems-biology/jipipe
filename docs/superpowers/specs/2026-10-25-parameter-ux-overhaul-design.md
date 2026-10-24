# Parameter Editor & Form Panel UX Overhaul — Design

**Date:** 2026-10-25 · **Branch:** `1339-parameter-ux-overhaul` · **Work item:** [#1339](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1339) (milestone 6.0.0)
**Related:** [#1124](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1124) new code editor — *follow-up, this spec leaves seams only*
**State:** design approved in conversation (visual-companion iteration, 2026-10-24/25)

## 1. Problem

The node parameter panel is the most-used UI surface in JIPipe. Its current state:

- **Inconsistent layout.** The form panel (`JIPipeDesktopFormPanel`) uses a 3-column
  grid (Name | Editor | Options). Eight to nine editors return
  `isUILabelEnabled() == false` and render as *wide* rows with a **self-drawn header**
  that embeds the parameter name into the widget (boolean uses the name as checkbox
  text; list, dynamic set, matrix, margin editors draw their own toolbar header).
  Result: the same panel mixes two row shapes with no visible rule.
- **Enum editors are stale.** `JIPipeDesktopEnumParameterEditorUI` /
  `…DynamicEnumParameterEditorUI` = `JComboBox` + a search button opening a **modal**
  pick dialog (`JIPipeDesktopPickEnumValueDialog`). The new panel API
  (`JIPipeDesktopRichSelectionBox` — searchable popup, icons, grouping, no OK) was
  built for the node browser and its own Javadoc names these editors as the planned
  next use case.
- **Dynamic set editors confuse users.** Vertical checkbox list with a *persisted*
  Collapse toggle (`JIPipeDynamicSetParameter.collapsed` is serialized into the
  parameter JSON), "items are hidden" eye-slash label, always-on.
- **List editors need a consistency pass.** `JIPipeDesktopListParameterEditorUI`
  (and `…ParameterCollectionListEditorUI`) carry an always-visible Add/Remove/Reorder
  toolbar, self-drawn name, per-row grip + item editor.
- **Colors use `JColorChooser.showDialog`** (confirmed: `UIUtils.selectColor` is a
  one-line pass-through to it) — no HSB/LAB, no palettes, no alpha, no recents.
- **Font settings are split** into 3+ parameters (family enum, size number, style
  enum, separate color) in ~8 visualization nodes, which the issue's target users
  constantly combine when styling labels/scale bars.

## 2. Goals

1. One consistent, modern form layout with a visible rule (see Part 1).
2. Enum-like parameters: new panel API for large option sets, combo for tiny ones.
3. Dynamic set: popup-based multi-select; no collapse.
4. List editors: modernized, consistent.
5. New dedicated `FontParameter` (family + size + style + color) with migration for
   affected nodes.
6. Modern multi-space color dialog (RGB / HSB / LAB) with palettes, alpha, recents,
   saved colors.
7. Consistency audit across all ~60 parameter editors.

## 3. Non-goals

- **Replacing RSyntaxTextArea** (#1124) — expression/script editors keep their
  current widget; the seam (editor component + `Edit` → function-builder button) is
  preserved so the new code editor slots in later without touching this redesign.
- Node browser / add-nodes panel changes (already delivered in #1324/#1340).
- Changes to parameter *serialization format* beyond the font migration and the
  dynamic-set `collapsed` drop (6.0.0 is a breaking major; no legacy compat carried,
  but loaders stay lenient about the two dropped keys).
- Non-parameter forms (settings sheets etc.) are *consumers* of the layout engine —
  they inherit the new layout without dedicated work.

## 4. Decisions (user rulings during design)

| # | Decision |
|---|----------|
| D1 | **Layout: option B — rule by complexity.** Scalar editors (number, string, enum, boolean, color, expression, optional, quantity…) keep a compact inline row `label … control … ?`. Compound editors (list, matrix, collection, code) get name-above + full-width control. Exception: the dynamic-set editor's *new* control is a compact selection box, so it stays inline (its old self-header disappears). Every editor gets its name from the form panel; **no editor draws its own name anymore.** |
| D2 | **Enum:** values ≤ 5 → plain combo (no search button). Values > 5 → single-select `JIPipeDesktopRichSelectionBox` (closes on pick; no modal). Threshold constant `COMBO_MAX_VALUES = 5`. |
| D3 | **Dynamic enums, editability per case:** `JIPipeDynamicEnumParameter.isEditable()` (overridden per class, default `false`) — editable ones **keep the (editable) combo** in all cases; non-editable ones follow D2. |
| D4 | **Dynamic set:** multi-select `RichSelectionBox`; button shows count summary ("Measurements (3)"); Clear doubles as select-none; live toggle; grouped rows where the set's item info defines groups. **The persisted `collapsed` field is removed** from the model (lenient deserialization of old projects). |
| D5 | **List editor:** compact footer (＋ Add + count) instead of the always-visible toolbar; per-row grip (click/ctrl/shift select, drag or right-click to reorder/delete) + item editor + delete; empty-state hint; `@ListParameterSettings.withScrollBar` behavior preserved. |
| D6 | **`FontParameter` is a dedicated class** (user ruling: class-driven, not annotation-driven, because node-side annotations may drop in complex cases while the parameter class stays): `family + size + style + color`, all always present. |
| D7 | **Font editor:** the inline row shows the **preview only** (font rendered in itself); clicking opens a **popout panel** (live preview band + family search + size + style + **embedded** color row + `Custom…`). **No popout-over-popout:** `Custom…` opens the full color dialog as its own window; the popout closes first; on OK the popout reopens with the updated font. |
| D8 | **Color dialog:** spaces **RGB / HSB / LAB** (two-way sync; hex `#RRGGBB`/`#RRGGBBAA` always available); **alpha** support (checkerboard swatches, α rail when the parameter is alpha-capable); **palettes**: pure colors, black/white/greys, **GNOME HIG named palette** (12 hues × 5 steps, real values from developer.gnome.org/hig), **last selected** (auto), **saved** (user, star-to-save, persisted in app settings); still modal with OK/Cancel. LAB: 2D a/b plane + L rail; out-of-gamut clamps to nearest sRGB on OK. |
| D9 | **#1124 is a follow-up** on the same milestone; this branch ships the overhaul first. |

## 5. Part 1 — Layout foundation (option B)

**Files:** `desktop/commons/components/panels/JIPipeDesktopFormPanel.java`,
`desktop/commons/components/parameters/JIPipeDesktopParameterFormPanel.java`,
`desktop/api/JIPipeDesktopParameterEditorUI.java`, + the 8–9 self-header editors.

### 5.1 New layout contract

- `JIPipeDesktopParameterEditorUI` gains the layout switch. The existing
  `isUILabelEnabled()` (true = 3-col row, false = wide row with self-drawn header)
  is **repurposed**: it now means *"this editor's name is provided by the form
  panel's label column"* and **returns `true` for every editor**. The *shape*
  (inline vs. stacked) is a new method, e.g. `boolean isUIInline()` (default `true`),
  overridden to `false` by compound editors: list, parameter-collection-list,
  dynamic set, matrix, margin/fixed-margin/inner-margin, code/script editors.
  `CONTROL_STYLE_*` constants remain for the existing grouping sort; their meaning
  is preserved, not redefined.
- `JIPipeDesktopFormPanel` rendering:
  - **Inline:** unchanged 3-column grid — label WEST, editor CENTER (weightx 1,
    fill HORIZONTAL), properties component (help `?` + options menu trigger) EAST.
  - **Stacked:** a new row shape — label (and the properties component) on one line
    WEST/EAST of each other, editor full-width below spanning all three columns
    (the old "wide" grid rows keep working; the label line reuses the same
    label/properties helpers so fonts, help tooltips, and the options menu are
    identical between shapes).
- Group headers, pinned parameters, search bar, collapse carets, and the
  label/without-label separation sorting stay as-is; the sort comparator's
  "label-less editors grouped first" clause is removed (there are no label-less
  editors anymore).

### 5.2 De-embedding names (the 8–9 offenders)

Every editor that today draws its own name stops doing so; the form panel supplies
the label:

| Editor | Today | After |
|---|---|---|
| `JIPipeDesktopBooleanParameterEditorUI` | name-as-checkbox-text (wide) / combo mode (`@BooleanParameterSettings(comboBoxStyle=true)`, custom `trueLabel`/`falseLabel`) — dual inconsistency | **checkbox → switch (inline)** for the default case; `comboBoxStyle=true` booleans (e.g. "By dilation"/"By erosion", "value > threshold"/"value < threshold") **keep their 2-value combo**, rendered inline with `trueLabel`/`falseLabel` as the items. Name from the label column in both modes — the inconsistency (name embedded in the widget) is gone |
| `JIPipeDesktopListParameterEditorUI` | self-header with name label | name from label column; header toolbar → compact footer (Part 4) |
| `JIPipeDesktopParameterCollectionListEditorUI` | same | same |
| `JIPipeDesktopDynamicSetParameterEditorUI` | self-header + Collapse/All/None | replaced by the selection box (Part 3) |
| `JIPipeDesktopMatrix2DParameterEditorUI` | self-header | name from label column, stacked |
| Margin editors (`…MarginParameterEditorUI`, `…FixedMargin…`, `InnerMarginEditorUIDesktop`) | label-off, self headers | name from label column (stacked — margin editors are multi-field) |

Everything else (≈51 editors) already uses the 3-col row and needs no structural
change beyond the audit pass (Part 6).

### 5.3 Effect on non-parameter consumers

`JIPipeDesktopFormPanel` is shared by settings sheets, run dialogs, the HTML editor,
etc. Their calls pass plain components + labels; the inline/stacked decision for
non-parameter rows stays on the caller side (the existing `addToForm` vs
`addWideToForm` calls are untouched). Only parameter rows change behavior, because
only parameter editors implement the new `isUIInline()`.

## 6. Part 2 — Enum-like parameters

**Files:** `plugins/parameters/ui/api/JIPipeDesktopEnumParameterEditorUI.java`,
`…DynamicEnumParameterEditorUI.java`, `desktop/commons/components/selection/JIPipeDesktopRichSelectionBox.java`,
delete `desktop/commons/components/pickers/JIPipeDesktopPickEnumValueDialog.java` +
`JIPipeDesktopPickDynamicEnumValueDialog.java` (verify no other callers first).

### 6.1 Selection rule (evaluated at `reload()`)

```
n = number of option values
if (dynamic && parameter.isEditable())          -> editable JComboBox (as today)      [D3]
else if (n <= COMBO_MAX_VALUES /*5*/)           -> plain JComboBox, no search button   [D2]
else                                            -> single-select RichSelectionBox      [D2]
```

The editor rebuilds its control when the value count crosses the threshold
(dynamic enums change at runtime).

### 6.2 RichSelectionBox adoption

- Single-select mode; clicking a row applies the value and closes the popup —
  no modal, no OK (the box's documented behavior).
- Options are mapped from `JIPipeEnumParameterItemInfo` (label, icon, tooltip) into
  `Option<T>`. **Small extension to `Option`: add an optional `tooltip` field**
  (constructor overload + list-cell renderer) — enum items already carry tooltips
  and the box currently drops them.
- Grouping: enums whose item info defines groups render group headers in the popup
  (the box already supports this).
- The button's idle text shows the current value's label (+ icon); for multi-value
  displays it reuses the summary-label mechanism.

### 6.3 Affected "enum-like" combos (consistency)

The threshold rule applies to the two registered enum editors. Other combo-based
pickers (plot data-series column, table column generator, IJ weka classifier, OME
accessor) keep their existing combos — they are domain-specific editors outside the
issue's enum scope, but they get the audit pass (Part 6) for label/consistency.

## 7. Part 3 — Dynamic set parameters

**Files:** `plugins/parameters/ui/api/JIPipeDesktopDynamicSetParameterEditorUI.java`,
`plugins/parameters/api/options/JIPipeDynamicSetParameter.java`.

- The editor becomes a **multi-select `RichSelectionBox`** (name from the label
  column, per Part 1 — the box sits inline; the box's button width is bounded by
  the summary-label mechanism, so the row stays compact).
  - Button: icon (checkbox-checked when non-empty) + "Count summary (n)" / first
    few names when empty-ish (existing legacy-label mode).
  - Popup: live multi-toggle (no OK), Clear = select none, group headers where the
    set's `JIPipeEnumParameterItemInfo` defines groups.
  - "Select all": the popup's Clear handles none; **all** = re-click Clear state is
    ambiguous, so add a small "Select all" action inside the popup header row next
    to Clear (two flat buttons, same visual family).
- `JIPipeDynamicSetParameter.collapsed` field **deleted**. The Jackson deserializer
  accepts and ignores a legacy `collapsed` key (one lenient line — 6.0.0 carries no
  legacy compat, but we don't reject old files either).
- No "items are hidden" state exists anymore, so the user-confusion source is gone.

## 8. Part 4 — List editors

**Files:** `plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java`,
`JIPipeDesktopParameterCollectionListEditorUI.java`.

- Name moves to the label column (Part 1); the self-header's name label is removed.
- **Compact footer** replaces the always-visible toolbar: `＋ Add` + count label
  ("3 paths") + a `⋯` menu (Select all / Invert / Clear list). Reorder is no longer
  a global toggle: the grip supports drag-reorder, and the right-click menu keeps
  Delete / Move up / Move down (existing menu, kept).
- Rows keep: grip (click / Ctrl / Shift selection with background highlight —
  existing behavior), per-row item editor (created via the same
  `createEditorInstance` with the synthetic item access), per-row delete (hover
  affordance + context menu).
- `@ListParameterSettings.withScrollBar` (fixed-height scroll) preserved; empty list
  shows the existing empty hint.
- `ParameterCollectionList` gets the identical treatment (rows are nested mini form
  panels — only the chrome changes).

## 9. Part 5 — `FontParameter` (dedicated class)

**New files:** `plugins/parameters/library/primitives/JIPipeFontParameter.java`,
`plugins/parameters/ui/library/JIPipeDesktopFontParameterEditorUI.java`,
`plugins/parameters/api/options/FontParameterSettings.java` (if any per-usage flags
are needed, e.g. `withColor=false` is **not** added — D6 keeps all four fields).

### 9.1 Model

```java
// plain parameter POJO (like PathList / OptionalIntegerParameter) — no interface
public class JIPipeFontParameter {
    private String family;    // display name; defaults to the system default family
    private int    size;      // pt, default 12
    private int    style;     // Font.PLAIN / BOLD / ITALIC / BOLD_ITALIC
    private Color  color;     // always present
    public java.awt.Font toFont();          // family + style + size (existing idiom)
    public JIPipeFontParameter copy(); ...
}
```

- Registered as parameter type **`jipipe:font`** (stable string literal) with the
  new editor.
- JSON: `{"family": "…", "size": 12, "style": 1, "color": "#RRGGBBAA"}` — plain
  fields, no indirection.

### 9.2 Editor (D7)

- **Inline row:** one control = live preview (a sample string rendered in the
  chosen family/size/style/color, e.g. "Aa Label text 12"), clickable.
- **Popout** (same popup family as the RichSelectionBox, positioned over the row):
  - top: large preview band (sample text + numbers)
  - Family → opens the panel-API search popup (system font families, grouped by
    serif/sans/mono)
  - Size → number field + spinner (reuses the number editor's formatting)
  - Style → B / I toggles
  - Color → **embedded**: current swatch + pure-colors mini-row + recent colors +
    `Custom…` button
  - `Custom…` / current-swatch → opens the full color dialog (Part 6) as its own
    window; the popout **closes first** (no stacked popups); on return, the popout
    reopens with focus on the preview, showing the updated font.
- Live: every change updates the preview band immediately and pushes the parameter
  (existing `setParameter` event flow).

### 9.3 Migration (per-node, idempotent)

Mechanism (verified): `JIPipeGraphNode.fromJson()` deserializes by *current* keys
into *current* classes, then calls `onDeserialized(JsonNode node, issues,
notifications)` with the raw node JSON. Each affected node:

1. Replace the split fields/getters with one `JIPipeFontParameter` field +
   `@JIPipeParameter("<key>")` (new key per node, e.g. `label-font`).
2. Override `onDeserialized`: if the raw JSON still contains the legacy keys
   (`label-font-family`, `label-font-size`, `label-font-style`, and the node's
   legacy color key), compose them into the new parameter; afterwards the legacy
   keys are gone on the next save (deserialization no longer writes them).

Affected sites (found in code, family/size/style currently split):
`DrawTextRoiAlgorithm`, `TableToTextROIAlgorithm`, `RoiToRGBAlgorithm`,
`DrawScaleBarAlgorithm`, `DrawScaleBarRoiAlgorithm`, `ScaleBarGenerator`,
`MontageCreator.LabelParameters` (nested), `DefaultTextBoxGraphCanvasNote`,
`RoiDrawer` (utility, not a node — migrate if it is parameterized).

`FontFamilyParameter` (`font-family-enum`) and `FontStyleParameter`
(`font-style`) **stay registered** (their type IDs are persisted in projects of
other/old nodes; deleting them would break unrelated loads) — the migrated nodes
simply stop using them. A grep gate in the plan verifies no *node* references the
old split params after migration.

## 10. Part 6 — Modern color dialog

**New files:** `desktop/commons/components/color/JIPipeDesktopColorChooserDialog.java`
(+ `ColorSpace` helper + palette data), modified: `utils/UIUtils.java`
(`selectColor` delegates to the new dialog — one call-site change, all color
editors inherit it).

### 10.1 Structure

- Modal `JDialog`, ~380×560, OK/Cancel.
- **Space tabs:** RGB / HSB / LAB, editing the same underlying color (two-way sync
  through a single `Color` state; hex + swatch always visible).
  - **RGB:** R/G/B/A sliders.
  - **HSB:** 2D saturation×value well + hue / sat / bright rails.
  - **LAB (CIE L\*a\*b\*):** 2D a/b chromaticity plane (crosshair axes, corner
    tags) + L rail (0–100); a/b rails for numeric entry; **out-of-gamut values
    clamp to the nearest sRGB on OK** (with the clamped value shown in the hex
    readout live).
  - Hex field: `#RRGGBB` or `#RRGGBBAA`; typing updates all widgets.
- **Alpha:** α rail + checkerboard behind the swatch, shown only when the caller
  passes `withTransparency` (from `ColorParameterSettings`) — pure colors are
  rendered on the checkerboard.
- **Palettes** (below the picker, same set in every space):
  - *Pure colors* — black, white, grey, + the 9–10 pure hues.
  - *GNOME HIG* — the 12 hues × 5 steps named palette (real values from
    developer.gnome.org/hig; shipped as a bundled resource so tooltips show names,
    e.g. "Blue 3").
  - *Last selected* — most recent ~8 picks, auto-tracked.
  - *Saved* — user colors (star-to-save from the dialog; persisted in app settings
    with an optional name; delete on right-click).
- **Persistence:** recents + saved colors live in application settings (JSON),
  shared by all color editors and the font popout.

### 10.2 Consumers

- `JIPipeDesktopColorParameterEditorUI` — the swatch button now opens this dialog
  (via `UIUtils.selectColor`).
- `JIPipeFontParameterEditorUI` — `Custom…` (Part 5).
- Any future color usage (e.g. canvas notes) inherits it.

## 11. Part 7 — Consistency audit

Scope: all ~60 `*ParameterEditorUI*` classes (56 in jipipe-core, 4 in plugins).

- Every editor: no self-drawn name; label supplied by the form panel (Part 1).
- Inline controls: uniform height (27px), uniform border (existing control border),
  right-aligned in the editor column, options column (`?` + `⋮`) identical.
- Boolean → switch (inline); `comboBoxStyle=true` booleans keep their 2-value
  `trueLabel`/`falseLabel` combo, inline. Optional → keep the `Enabled`/`On` chip +
  green border (already good); verify the chip aligns with the inline row.
- Quantity, number, range editors: spinner/step affordances kept; verify label
  alignment with decimals.
- Expression editors: unchanged widget (RSyntaxTextArea, #1124 seam), but the
  bordered `Edit` + hint layout is normalized to the inline row contract.
- Plugin-side editors (plot column enum, table column generator, weka classifier,
  OME accessor): label/consistency fixes only, no behavior change.

## 12. Testing (per AGENTS.md test policy)

**No Swing UI-structure tests** (the user verifies the panel in the running app).
Tests only for non-trivial logic:

1. **Color space math** — RGB↔HSB and RGB↔LAB round-trips (tolerance 0.5/255),
   LAB→sRGB **gamut clamping** (e.g. L=100, a=+100, b=+100 clamps correctly),
   hex parse/format incl. 8-digit alpha. Pure functions, no UI.
2. **FontParameter** — serialization round-trip (`family/size/style/color`), and
   **migration**: feed a legacy node JSON (split keys, no new key) through
   `fromJson` → composed parameter matches expected; feed an already-migrated JSON
   → idempotent (no double-migration); legacy keys absent after re-serialization.
3. **Enum threshold rule** — the selection decision (editable / ≤5 combo / >5 box)
   as a pure unit, incl. a dynamic set crossing the threshold at runtime.
4. **Dynamic set leniency** — a JSON containing the legacy `collapsed` key
   deserializes without error and the field is gone on re-serialize.
5. **RichSelectionBox Option tooltip** — trivial extension, no test (per policy:
   no re-verification of obvious code).
6. Existing gates keep passing: full `jipipe-core` suite, launcher taxonomy
   validation, plugin-aware registry tests (presence, not counts).

UI verification of every panel change is by the user in the app (standing ruling).

## 13. Component / file summary

**New**
- `JIPipeFontParameter` (+ editor UI) — Part 5
- `JIPipeDesktopColorChooserDialog` (+ space math, palette resources, settings
  persistence for recents/saved) — Part 6
- `isUIInline()` on the editor base class; stacked-row rendering in the form panel — Part 1

**Changed**
- `JIPipeDesktopFormPanel` / `JIPipeDesktopParameterFormPanel` (layout, sort
  clause), `JIPipeDesktopParameterEditorUI` (contract)
- 8–9 self-header editors (de-embedded names; boolean → switch)
- enum + dynamic-enum editors (D2/D3 rule), dynamic-set editor (popup), list
  editors (footer)
- `JIPipeDesktopRichSelectionBox.Option` (+tooltip), `UIUtils.selectColor`
  (delegate), `JIPipeDynamicSetParameter` (−collapsed), ~8 nodes (+font migration)

**Deleted**
- `JIPipeDesktopPickEnumValueDialog`, `JIPipeDesktopPickDynamicEnumValueDialog`
  (after verifying zero remaining callers)

## 14. Phasing (for the implementation plan)

1. **Layout foundation** — base-class contract + form-panel stacked rows +
   de-embedded names for all 8–9 offenders + boolean switch. (Largest blast radius;
   audit baseline.)
2. **Enum & dynamic set** — threshold rule, RichSelectionBox adoption (+Option
   tooltip), popup multi-select, delete modal pick dialogs, drop `collapsed`.
3. **List editors** — footer chrome, reorder behavior, both list editors.
4. **Color dialog** — spaces, math, palettes, alpha, recents/saved, `selectColor`
   delegation, font *not yet*.
5. **FontParameter** — model + editor + popout + migration for the ~8 nodes.
6. **Consistency audit pass** — remaining 50 editors, polish, docs.

Each phase ships independently reviewable; phase 1's layout is the only one that
visibly touches *every* node (intended — that is the overhaul).

## 15. Open items (resolved during design, recorded for the plan)

- Combo threshold = 5 (D2). ✓
- Editable dynamic enums keep editable combo, per-class `isEditable()` (D3). ✓
- LAB plane = a/b with L rail; out-of-gamut clamps on OK (D8). ✓
- GNOME HIG values from the official palette page (verified 2026-10-25). ✓
