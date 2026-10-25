# Parameter Editor UX — Pass 2: List Refinement, Color Dialog, Number Slider, HTML Editor

**Date:** 2026-10-25 · **Branch:** `1339-parameter-ux-overhaul` · **Work item:** [#1339](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1339) (milestone 6.0.0)
**Related:** pass 1 spec `2026-10-25-parameter-ux-overhaul-design.md` (D1–D9, Tasks 0–12) · [#1124] new code editor — follow-up, seams only
**State:** design approved in conversation (2026-10-25). This pass refines the editors shipped in pass 1 based on the user's live-app review (3 screenshots) and adds two net-new pieces: a slider number style and the HTML editor widget modernization.

## 1. Scope

Eight concrete items (S1–S7) plus one audit task (S8, executed last, still part of this branch):

| # | Area | Kind |
|---|------|------|
| S1 | Boolean switch: ON-track color success → primary | 1-line fix |
| S2 | List editors: controls into the name row, compact-but-editable inline rows, load-more, modal bulk editor, grip menu + caret icons | rework (2 editors + 1 new dialog) |
| S3 | `JIPipeDesktopRichSelectionBox`: keyboard navigation + left-aligned value | shared component |
| S4 | Font popout: size field width, column alignment | small fix |
| S5 | Color dialog: pill tabs, palette layout, 16 pure colors, label alignment, tooltips with RGB/HSB/LAB, rail/well input + state-sync fix | rework (1 dialog) |
| S6 | Number parameter: new opt-in **slider** style (GIMP-like, value editable in the control) | net-new (annotation enum + shared control + editor branch) |
| S7 | HTMLText: form-contract normalization + `JIPipeDesktopHTMLEditor` widget modernization | rework (shared widget) |
| S8 | Render out **each** registered parameter editor, VLM-judge, fold agreed findings in | audit task (after S7 verified) |

## 2. S1 — Switch color

`JIPipeDesktopSwitch` paints the ON track with `style.getSuccessColor()` (green). Change to `style.getPrimaryColor()` (blue, more neutral).

- The knob is already white → stays visible on the primary track.
- The focus ring already uses primary; keep it (2px outline around the track) — it remains distinct because the ring is *around* the track, not on it.
- File: `jipipe-core/.../desktop/commons/components/toggle/JIPipeDesktopSwitch.java` (line 86).
- Verification: re-render `panel-combo-boolean` + `panel-roi3d-statistics`, VLM: switches blue when on, green no longer used by switches.

## 3. S2 — List editors

Applies identically to `JIPipeDesktopListParameterEditorUI` and `JIPipeDesktopParameterCollectionListEditorUI` (the collection variant's rows are nested mini form panels; only the chrome changes).

### 3.1 Layout contract

The list editor stays `isUIInline() = false` (stacked: name row on top, body below). The top **name row** (currently spacious and empty) carries the list controls, left-associated with the label:

```
Files          [＋ Add] [Edit]   10 items            [⋯]
┌─────────────────────────────────────────────────────┐
│ ≡  item 1 (fully editable inline)                    │
│ ≡  item 2                                            │
│ ≡  item 3                                            │
│ ≡  item 4                                            │
│ ≡  item 5                                            │
│              [ +5 more ]        (only if n > 5)       │
└─────────────────────────────────────────────────────┘
```

- **Controls on the name row**, not in a bottom footer: full-label **Add** button, **Edit** button (opens the modal, §3.3), the count label (`n items`, muted), and the **⋯** menu. Compact flat buttons, tight spacing, baseline-aligned with the parameter label. Placement via the `createHeaderControls()` extension point (§3.2).
- **Inline rows are fully editable** (user ruling: "because lists pass a parameter, users need to be able to edit the items — otherwise the UX is shit"). Each row = grip + the same per-item editor as today (single-value editors) or the same nested mini form panel (collection lists). Deletion only via the grip menu (§3.1) or the modal (§3.3).
- **Load-more pagination:** up to **5** rows visible by default; when `n > 5`, a footer button `+k more` (k = n − visible) reveals the next 5 each click, until all are shown; a **Show less** button collapses back to 5. No scroll pane in the inline body. `@ListParameterSettings.withScrollBar` (fixed-height scrolling) is **kept** as an override: when set, the body scrolls at the fixed height and no pagination is applied (some lists are long and heterogeneous; this preserves the old escape hatch).
- **Grip:** small **vertical** grip icon (`actions/grip-lines-vertical.png`). **Left click opens the per-row context menu** (Delete / Move up / Move down) — this replaces pass-1's click-to-select on the grip, since bulk selection now lives in the modal (§3.3). **Drag** still reorders (keep the pass-1 `TransferHandler`). Move up/down icons → centered **caret** icons: `actions/go-up.png` / `actions/go-down.png` if present, else draw a 12px caret via `UIUtils` (no new icon files unless a suitable one is missing).
- **No hover-delete button** (user ruling): pass-1's finicky hover-× is removed entirely; per-row deletion goes through the grip menu (Delete) or the modal (Remove selected / row menu). The row's left gutter therefore only contains the grip.
- **⋯ menu (inline):** Clear list only. (Add and Edit are buttons on the name row; Select all / Invert live in the modal, §3.3.)

### 3.2 Implementation note (name-row controls)

`JIPipeDesktopFormPanel` is a 3-column grid: col 0 = label (auto width from the label text), col 1 = content (`weightx=1`, `fill=HORIZONTAL`), col 2 = options (`?`/`⋮`). `addToFormStacked(component, description, documentation)` currently renders: line 1 = `description` at `gridx=0, gridwidth=2` (label spanning into the content column) + options at col 2; line 2 = the component at `gridx=0, gridwidth=2`.

The extension point: `JIPipeDesktopParameterEditorUI` gains `protected JComponent createHeaderControls() { return null; }`. In `addToFormStacked`, when the editor's `createHeaderControls()` is non-null, line 1 becomes: `description` at `gridx=0, gridwidth=1` (label col only), header controls at `gridx=1, gridwidth=1, fill=HORIZONTAL, weightx=1` (content col, left-associated, baseline-aligned with the label), options at col 2 unchanged. Line 2 (the body) is unchanged. Inline rows already use exactly this label-in-0 / control-in-1 shape, so the label column width behaves identically. Only the two list editors override `createHeaderControls()`. This keeps the pass-1 D1 rule (form panel owns the row, editors never draw the name) intact: the editor contributes *controls* to the shared header line, not a name.

### 3.3 Edit modal (`JIPipeDesktopListEditorDialog`, new)

A modal `JDialog` (app-modal) that hosts the **full** editor for the same list instance. It has **no OK/Cancel** (user ruling): changes are applied to the parameter immediately, so the only button is **Close** (and the window X / Esc close it the same way).

- Unlimited rows (no pagination), each fully editable exactly like the inline rows.
- **Bulk operations** (the selection machinery now lives here): **Select all / Invert selection / Clear selection** + **Remove selected** + **Clear list**. Selection is grip-based exactly as in pass 1 (grip click selects, Ctrl toggles, Shift ranges; row content clicks edit, not select), with the existing background highlight. Right-click on grip/row → the per-row menu (Delete / Move up / Move down); drag reorders.
- **Search/filter box** (list editor only, where items are strings/paths — collection lists get it too, filtering by each item's short description text); filtering hides non-matching rows (selection state preserved).
- Drag-reorder via the grips (same transfer handler as inline).
- Edits the **same** `JIPipeListParameter` instance in place and live — no copy, no commit step: every edit (including structural ops like Remove selected / Clear list) is applied to the parameter immediately, matching pass-1 behavior. The dialog is a window, not a transaction, so there is nothing to confirm or discard (hence Close-only, above).
- Fixed size ~640×520, resizable.

After any change, the inline compact view refreshes (row editors publish `setParameter(..., true)` as today → the form panel's structure/UI events already trigger `reload()`).

### 3.4 Removed

- The pass-1 bottom footer (Add/count/⋯) — controls move to the name row.
- The inline `withScrollBar` bottom empty-label placement — empty state stays ("This list is empty" hint under the name row, no body).

## 4. S3 — RichSelectionBox keyboard + alignment

`JIPipeDesktopRichSelectionBox` (shared: enum boxes, dynamic set, font-family picker) is a `JButton` showing the current value + a searchable popup (`JPanel` with `searchField` + one or two `JList`s).

- **Left-align the value:** `setHorizontalAlignment(SwingConstants.LEADING)` on the button (and keep `setFocusPainted(false)`), so it matches combo boxes (which left-align).
- **Keyboard model** in the popup (focus is already on `searchField` when opened):
  - **↓** from the search field: moves focus to the (first visible) list and selects row 0.
  - **↑/↓** in the list: move the selection; the list scrolls to keep it visible.
  - **↑** on row 0: back to the search field.
  - **Tab / Shift-Tab**: between search field and list(s) (two-pane mode: Tab cycles search → list A → list B).
  - **Enter in the list**: single-select → select the item, close, fire the selection handler; multi-select → toggle the item, keep open (Esc/Enter-again or click-outside closes; **Enter in the search field** in multi mode closes and confirms the current selection).
  - **Enter in the search field** (special behavior, user-specified): single-select → confirm the **first** filtered item and close; multi-select → close and keep the current toggles.
  - **Esc**: close without changing the selection.
  - Selection is initialized to the currently-selected value(s) on open, so an immediate Enter confirms the status quo.
- Non-list key handling (type-to-filter) is unchanged: typing in the search field filters the list; the first match is auto-selected while typing (existing behavior) so Enter-in-search naturally picks it.

This is shared plumbing: a small `PopupKeyModel` (or the equivalent inline) inside the box; the two-pane and single-pane modes both use it. No public API change except the visual alignment.

## 5. S4 — Font popout

`JIPipeDesktopFontParameterEditorUI` (pass-1 Task 9):

- **Size field width *and* height** (user ruling — the screenshot's size field clipped text vertically too): the spinner currently pins `preferredSize(60, 22)` — 22px is shorter than the theme font's line height, so the digits get cut off. Fix both dimensions: fixed comfortable width (min 64px field + arrows, total ~80px) for 3-digit sizes, and a height that fits the content — derive it from the spinner editor's own default preferred height (i.e. don't override the height at all, or use `getPreferredSize().height` before sizing width) instead of the hardcoded 22.
- **No clipped control heights anywhere in the popout:** the same 22px-pin problem applies to the B/I toggle buttons (`28×22`). Rule for every fixed-size control in the popout (spinner, toggles, label cells): width may be pinned for layout, but height must be the control's natural default preferred height (or larger) under the active theme — never a hardcoded value that undercuts the theme font's line height.
- **Column alignment:** the preview band, family box, size spinner, B/I toggles, and color section all align their control start to one shared left inset. Use the **form panel system** for the popout's rows (user ruling: "just use the form panel system which was designed for that") — `JIPipeDesktopFormPanel` (`addToForm`) — instead of the hand-rolled `fieldRow` label cells: its GridBagLayout automatically sizes the label column to the widest label under the active theme font (font-dependent), which also fixes the VLM-noted left-shift of the top two rows. Rows without a label use an empty label cell of the same column.
- Keep the embedded color section and the popout-hides-before-custom-dialog behavior as-is.

## 6. S5 — Color dialog

`JIPipeDesktopColorChooserDialog` (pass-1 Task 7). Eight items:

1. **No horizontal scrollbar in the palette area.** The palette panel (`buildPalettes`) currently can exceed the dialog width (GNOME HIG 9×5 row). Change swatch rows to **wrap** (flow layout with wrapping: a `JPanel` per row of ≤ N swatches where N fits the dialog width at 18px swatches + gaps), so only vertical scrolling occurs. Palette scroll pane: `HORIZONTAL_SCROLLBAR_NEVER`.
2. **Pill tabs.** Apply the existing `JIPipeDesktopModernPillTabbedPaneUI` to `spaceTabs` when the modern theme is active (mirror how `JIPipeDesktopTabPane` does it; fall back to the default UI for the classic theme). The tab area shrinks accordingly (the pill UI has its own preferred size).
3. **RGB label alignment.** The three labels left-align but right-edges are ragged ("Green" widest). Change `railRow` label cells to a **fixed width** (e.g. 34px, `setPreferredSize`) right-aligned, so the rails all start at the same x and the labels form a clean column. Applies to all three tabs (RGB R/G/B/α, HSB H/S/B/α, LAB L/a/b/α) — single shared `railRow`.
4. **Rail/well input + state-sync bug** (user-confirmed symptoms: sliders worked, 2D areas did not; and regardless of rail changes, the color preview and hex field did not update). Root cause found in code (supersedes pass-1's thin-hit-target theory): **`refresh()` is dead code from `setColor()`** — `setColor` sets `updating = true` then calls `refresh()`, whose first line is `if (updating) return;`, so the body never runs. Rails merely *look* alive because their own `setValue → repaint` moves the thumb; the wells and the hex/preview never repaint. Fix: keep the re-entrancy guard (it exists for the real cycle `refresh → rail.setValue → listener → setColor → refresh`), but move it so it guards *recursion*, not the top-level call: `setColor` only assigns `currentColor` and calls `refresh()`; `refresh` checks-and-sets the `updating` flag around its body — a nested `refresh` (from a rail listener mid-update) returns early, but the outer call runs to completion and updates rails, wells, hex field, and preview swatch. **Regression test** (headless, non-Swing-structure): instantiate the dialog, dispatch a synthetic press on each rail/well and assert the hex field text updates (i.e. `refresh()` ran) — fails on current code (the bug), passes after the fix; keep the rail-row hit-target widening (whole row ~22px forwards presses to the rail) as an additional usability fix, as the rails are also genuinely thin.
5. **Pure colors → all 16 4-bit colors** (user ruling). Replace the 9-entry set with the 16×16×16 palette: `#000000, #550000, #AA0000, #FF0000, #005500, #00AA00, #00FF00, #555500, #AAAA00, #FFFF00, #000055, #0000AA, #0000FF, #550055, #AA00AA, #FF00FF` (standard 4-bit cube order: B, G, R planes). Each swatch tooltip shows the hex + RGB/HSB/LAB (item 8).
6. **Swatch/title alignment.** One shared left margin for section titles and swatch rows; swatch cells a uniform 18×18 with 4px gap; the "Saved" row's star button aligns to the same right edge as section titles.
7. **Merge "Pure colors" + "GNOME HIG" into one untitled block.** The two render consecutively with no section titles (they're not user-relevant names); a single 8px gap separates them. "Last selected" and "Saved" keep their titles.
8. **Swatch tooltips include RGB / HSB / L\*a\*b\*.** Every swatch tooltip (pure, HIG, last selected, saved) shows: name (if any), `#RRGGBB`, `RGB(r, g, b)`, `HSB(h°, s%, b%)`, `L*a*b*(L, a, b)` — computed via the existing `ColorSpaces.toHsb` / `toLab`. The current swatch header (top-right) tooltip gets the same.

## 7. S6 — Number slider style (net-new, opt-in)

### 7.1 Annotation

`NumberParameterSettings` (exists: `min`, `max`, `step`) gains:

```java
enum Style { FIELD, SLIDER }
Style style() default Style.FIELD;
```

Nodes opt in per-parameter, e.g. opacity: `@NumberParameterSettings(min = 0, max = 1, step = 0.01, style = NumberParameterSettings.Style.SLIDER)`. No serialization impact (annotation-only, not persisted). `FIELD` is the default → existing parameters are untouched.

### 7.2 Control: `JIPipeDesktopNumberSlider` (new, shared)

A single-control GIMP-style slider+editor in one component (theme-aware, custom-painted). **Design is VLM-driven** (user ruling: "heavily suggest usage of vlm to properly design the control properly"): iterate the visual design by rendering the control (screenshot harness, as in pass 1) and running VLM critiques (state, hit targets, thumb affordance, theme fit, disabled state, focus ring, alignment with the other editors) until the design is approved — the spec's bullet list below is the *functional* baseline; the visual proportions/affordances come out of that loop.

- **Track**: horizontal bar (~6px) with a filled portion up to the value (primary color), a **thumb** (circle ~12px), drawn over the control's background; min/max labels are *not* shown (they live in the tooltip / docs).
- **Direct manipulation**: press/drag anywhere on the track sets the value (`x → value`), with **step snapping** (round to `step`, clamp to min/max). Shift-drag = fine (step/10). Arrow keys ±step (component is focusable; Left/Down −, Right/Up +, Home/End = min/max).
- **Value editing in the same control**: double-click (or F2) turns the control into an inline text field (overlaid, monospace) pre-filled with the current value; Enter commits (parse, clamp, snap; invalid input reverts), Esc reverts, focus lost reverts. While editing, the thumb is hidden.
- **Tooltip** (persistent off): `value` + `(min – max, step)`.
- API: `getValue()/setValue(double)` (fires `ChangeListener` on user-caused changes), `setRange(min, max, step)`, `setPrecision(int decimals)` (from the parameter's integer-ness).

### 7.3 Editor branch

`JIPipeDesktopNumberParameterEditorUI`: when the parameter's `NumberParameterSettings.style() == SLIDER` (and `max > min` — a bounded range; if unbounded, fall back to FIELD with a one-time log warning), render `JIPipeDesktopNumberSlider` instead of field+spinners, keeping the same outer border, height family, and value sync (the existing document-listener pattern becomes a change-listener). The FIELD path is unchanged.

**No new registered parameter type** (user ruling: enum on the existing settings). No node migration needed; adopting nodes come in later (or in S8 follow-ups) — this pass ships the capability + one or two example adoptions if trivially safe (e.g. opacity 0–1 in a node that already annotates min/max), otherwise none.

### 7.4 Tests

- `JIPipeDesktopNumberSlider` math (headless): snap-to-step, clamp to min/max, fine-step (Shift) factor, value→fraction and fraction→value round-trip, parse/commit of the inline edit (valid/invalid/empty), arrow-key stepping (simulate key events on the component).
- Annotation: `NumberParameterSettings` default style == FIELD (registry/behavior guard, not UI structure).

## 8. S7 — HTMLText editor (contract + widget)

Two layers:

### 8.1 Form-contract normalization (`JIPipeDesktopHTMLTextParameterEditorUI`)

Today it embeds the full `JIPipeDesktopHTMLEditor` (toolbar + WYSIWYG pane) directly in the row — a tall, busy control. Normalize to the pass-1 contract:

- `isUIInline() = false` (stacked — the widget is compound), **or** a compact inline value + **Edit** button if the compact widget proves usable (decide in implementation, verify with VLM; the #1124 seam — "editor component + Edit button" — is preserved either way).
- Consistent border/height, no self-drawn name (already true).

### 8.2 Widget modernization (`JIPipeDesktopHTMLEditor`, shared — used by the parameter editor and the expression/function-builder contexts)

Keep the public API stable: `Mode` enum, `getHTML()/setText()/getDocument()`, `WITH_DIALOG_EDITOR_BUTTON` flag.

Changes:
- **Compact mode becomes the default shape**: a single-row icon-only toolbar (B, I, U, foreground color, insert image, |, mode toggle, Edit-in-dialog) with hover tooltips, tight 24px height; the janky "mode" popup menu (compact/full/HTML) is replaced by a simple **compact ⇄ expanded** toggle (expanded = the current Full toolbars).
- **Live preview**: an optional second tab or split (toggle button in the toolbar) rendering the current HTML read-only (`JEditorPane` non-editable, same HTML) — "WYSIWYG / Preview / Source" 3-state toggle. Source mode = `RSyntaxTextArea`-free plain `JTextArea` with `text/html` (no syntax dependency; #1124 will replace this later).
- **Consistent chrome**: `UIUtils.createControlBorder()` on the whole widget; toolbar buttons flat 24×24; no redundant outer border from the parameter editor (the editor drops its `setBorder` since the widget now carries it).
- The dialog editor button (`getHTMLByDialog`) stays for the "edit in a big window" case.

## 9. S8 — Render-out audit (after S7, same branch)

Extend the `screenshot-ui` harness with a **per-editor showcase**: for every registered parameter type without a dedicated showcase yet, a synthetic one-parameter form panel (via `JIPipeDynamicParameterCollection.addParameter(key, class, name, desc)`) rendered at the standard 480px width. Include at minimum: boolean, number (field), number (slider, via a synthetic annotated type or a dedicated test parameter class), integer list, string, string-or-double, HTML text, enum (small / large), dynamic enum, dynamic set, path list, parameter-collection list, matrix, margin (fixed/inner), font, color, optional-boolean, optional-color, expression, runtime-partition ref.

Then:
1. Render the full set, run `da_describe_image` per render with a fixed expectation prompt (border? height family? left-aligned value? options column? no self-drawn name? obvious clipping?).
2. Compile a findings list; **with the user**, decide which findings are in-scope nits (fixed in this branch) vs. follow-up issues.
3. Fix the agreed nits (commits tagged `(#1339)`), re-render, and include the final full-set render in the branch's verification record.

S8 is the gate before the merge request: the branch is "done" when S1–S7 are committed + verified and S8's in-scope findings are fixed.

## 10. Verification & testing (whole pass)

- **Tests (non-trivial logic only, per repo policy):** number-slider math (§7.4); list load-more pagination + modal bulk-op state (pagination index math, selection-preservation across load-more); RichSelectionBox Enter-in-search-first-item selection (drive the key model headlessly via the popup's components); color-dialog rail/well drag regression (§6.4). No Swing-structure tests.
- **VLM + pixel scans** for every visual item: S1 (switch color), S2 (name-row controls, load-more, grip menu, modal), S3 (alignment; keyboard can't be VLM-verified — the key model is unit-driven), S4 (size field, alignment), S5 (tabs, palette, labels, tooltips — tooltip content via a headless render of the tooltip or code assertion), S6 (slider render incl. thumb position + inline edit state), S7 (compact toolbar, preview toggle), S8 (full sweep).
- **Full build + core + ij-algorithms suites** green after each cluster.
- Commit per cluster with the `(#1339)` suffix; ledger entries in `.superpowers/sdd/.../progress.md`.

## 11. Non-goals

- No changes to parameter *serialization* (the slider is annotation-only; lists keep their format).
- No new external dependencies.
- #1124 (code editor) remains a follow-up; the HTML Source mode and expression editors keep their seams.
- No changes to the image-viewer ROI drawer or other pass-1 scope.
- The per-editor audit (S8) decides *additional* nits; it does not re-open pass-1 decisions unless a genuine defect is found.
