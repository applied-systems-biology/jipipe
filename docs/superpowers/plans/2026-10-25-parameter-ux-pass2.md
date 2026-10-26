# Parameter Editor UX — Pass 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the pass-1 parameter editors (switch color, list editors, RichSelectionBox, font popout, color dialog) and add two net-new pieces (an opt-in slider number style and the modernized HTML editor widget), then run a per-editor render-out audit.

**Architecture:** Each item reuses pass-1 infra. Lists gain a `createHeaderControls()` extension point on the shared editor base so the form panel renders editor-provided controls on the name row (the form panel still owns the name). A Close-only modal hosts the bulk list editor. The color dialog's dead-code `refresh()` is fixed so rail/well input propagates to the hex field. The number slider is a self-contained custom-painted control driven by a VLM design loop. #1124 (code editor) stays a follow-up.

**Tech Stack:** Java 21, Swing, Jackson, JUnit 5 (jipipe-core test module only), no new external dependencies.

**Spec:** `docs/superpowers/specs/2026-10-25-parameter-ux-pass2-design.md` (read it; S1–S8 are normative). Pass-1 spec `docs/superpowers/specs/2026-10-25-parameter-ux-overhaul-design.md` (D1–D9) stays binding.

## Global Constraints

- Java 21, Maven. Build check: `mvn -q -pl jipipe-core compile`. Tests: `mvn -q -pl jipipe-core test -Dtest=<Class>`. Full build: `mvn -q install -DskipTests` (needed before the screenshot harness — its classpath resolves jipipe modules from `~/.m2`).
- **No new external dependencies.** Swing only.
- Registration IDs are **stable string literals** — never code-derived (AGENTS.md). No new registered parameter types in this pass (the slider is an annotation enum on the existing `NumberParameterSettings`).
- **No Swing UI-structure tests** (tab titles, component visibility, insets, renderer details, dialog layout, button enabled-state). UI is verified by the user in the running app; tests only for non-trivial logic (spec §10): list pagination index math, RichSelectionBox key model, color-dialog refresh regression, number-slider math.
- ImageJ legacy patcher constraint (AGENTS.md): do not call `LegacyInjector.preinit()` from tests, do not add `--add-opens` workarounds. Tests that boot `JIPipe.ensureInstance()` are fine — the `LauncherSessionListener` handles ordering.
- AWT-backed tests (any test that shows a `JDialog`/popup) must **skip when headless** (`Assumptions.assumeTrue(false, "headless environment")` in `@BeforeAll`, mirroring `TemplateManagementDialogTest`). This dev has `DISPLAY=:0` + `xvfb` so they run here; CI may be headless so they must skip cleanly.
- Commit per task, message suffix `(#1339)`. Do NOT edit `dist/zip/build*.sh` (generated) or remove `ImageJLegacyPatcherLauncherSessionListener`.
- **VLM verification** (screenshot harness, `da_describe_image`): every visual task renders the affected showcase panel and a VLM prompt verifies the expected change is present and no obvious regression (empty/overlapping/truncated controls, error text) is visible. The VLM verdict is a *signal*, not a gate: ambiguous findings go to the user, the final UI judge (standing ruling).
- **Harness run command** (canonical, reuse in every VLM step):
  ```
  mvn -q install -DskipTests
  java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/javax.swing=ALL-UNNAMED \
       --add-opens=java.desktop/sun.font=ALL-UNNAMED -Djava.awt.headless=false \
       -cp "$(mvn -q -pl jipipe-launcher dependency:build-classpath -Dmdep.outputFile=/dev/stdout | tail -1)" \
       org.hkijena.jipipe.launcher.JIPipeLauncher screenshot-ui --out /tmp/opencode/1339/after
  ```
  Then `da_describe_image` on the PNGs in `/tmp/opencode/1339/after`. (Baseline "before" set is already in `/tmp/opencode/1339/before`.)

## Review Focus

These input classes / failure modes the spec implies but no unit test fully exercises — each gets a check in its owning task:

1. **`@ListParameterSettings(withScrollBar=true)` lists** — the fixed-height scrolling escape hatch must keep working and pagination must NOT apply (spec §3.1). A reasonable person expects the old override to still scroll, not paginate. → Task 3 (pagination model test: `paginationEnabled()==false` → `visibleCount()==total`, `loadMoreRemaining()==0`) + VLM (a `withScrollBar` list, if reachable in the showcase; else manual).
2. **Unbounded number param annotated `style=SLIDER`** (`max==POSITIVE_INFINITY`) — must fall back to FIELD with a one-time log warning, never render a broken/degenerate slider (spec §7.3). → Task 8 (pure `shouldUseSlider` decision test: unbounded → false) + VLM (the `panel-number-slider` showcase shows the slider for a bounded param; an unbounded SLIDER param shows a field — verify by adding an unbounded SLIDER field to the showcase or manual).
3. **Alpha-capable color dialog after the `refresh()` fix** — dragging a rail must update the hex field *including* the alpha digits (`#RRGGBBAA`); a non-alpha dialog hides the α rail and 8-digit input normalizes to 6-digit (spec §6.4). → Task 7 (refresh regression test drives a rail and asserts the hex field text changes) + VLM (`dialog-color.png` shows a changed hex; the pass-1 alpha round-trip still holds).
4. **Grouped (two-pane) RichSelectionBox keyboard nav** (e.g. the font-family picker's Monospace/Serif/Sans-serif groups) — Tab cycles search → list A → list B and Enter-in-search confirms the first filtered item in multi-mode too (spec §4). A reasonable person using a grouped picker expects the same keyboard model as the single-pane case. → Task 5 (key-model test covers the two-pane focus cycle + Enter-in-search).
5. **Modal editing a *collection* list** (nested mini form panels, not flat strings) — the modal must render fully-editable nested rows and its search filter matches each item's short description text (spec §3.3). → Task 4 (VLM: render a collection-list modal; a plain string list's modal is the easier path — verify both).
6. **Font popout under a large theme font** — the "no clipped control heights" rule (spec §5) must hold when the active theme font is bigger than the default; the size field must not clip at any theme font size. → Task 6 (VLM: render `panel-font` at the default theme; if the user runs a larger-font theme, re-verify manually).

---

### Task 1: S1 — switch ON-track color → primary

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/toggle/JIPipeDesktopSwitch.java:86`

**Interfaces:**
- Consumes: `JIPipeDesktopModernThemeStyle.getPrimaryColor()` (already exists; `getSuccessColor()` is the current wrong color).
- Produces: none (visual-only).

- [ ] **Step 1: Change the ON-track color**

In `JIPipeDesktopSwitch.paintComponent`, line 86:

```java
// before
Color trackColor = selected ? style.getSuccessColor() : style.getBorderColor();
// after
Color trackColor = selected ? style.getPrimaryColor() : style.getBorderColor();
```

Keep the focus ring (already `getPrimaryColor()`), the white knob, and everything else unchanged.

- [ ] **Step 2: Compile**

Run: `mvn -q -pl jipipe-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: VLM-verify**

Run the harness command (Global Constraints) with `--only panel-combo-boolean,panel-roi3d-statistics`. `da_describe_image` each PNG: the switch ON track is now **blue (primary)**, not green; the knob (white) and focus ring are still visible; no green remains on any switch. Report the verdict.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/toggle/JIPipeDesktopSwitch.java
git commit -m "fix(desktop): boolean switch ON-track uses primary color, not success (#1339)"
```

---

### Task 2: S2a — `createHeaderControls()` extension point + list header controls (additive)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/api/JIPipeDesktopParameterEditorUI.java` (add `createHeaderControls()`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/panels/JIPipeDesktopFormPanel.java` (add 4-arg `addToFormStacked` overload)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/parameters/JIPipeDesktopParameterFormPanel.java:~545` (pass header controls)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java` (override `createHeaderControls()`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopParameterCollectionListEditorUI.java` (same)

**Interfaces:**
- Produces (consumed by Tasks 3, 4):
  - `JIPipeDesktopParameterEditorUI#createHeaderControls(): JComponent` (returns `null` by default).
  - `JIPipeDesktopFormPanel#addToFormStacked(T component, Component description, MarkdownText documentation, JComponent headerControls)`.
  - Each list editor overrides `createHeaderControls()` returning a `JPanel` holding the **Add** button, the **count** `JLabel` (`n items`, muted), and the **⋯** menu (Clear list only). **This task is additive: the pass-1 bottom footer is NOT removed yet** (Task 3 removes it when it moves the chrome into the shared base), so during Task 2 the controls appear on the name row *and* the footer still exists. **Edit is added in Task 4** (when the modal exists to open).

- [ ] **Step 1: Add `createHeaderControls()` to the editor base**

In `JIPipeDesktopParameterEditorUI`, after `isUIInline()`:

```java
/**
 * Optional controls rendered on the stacked row's name line, in the content
 * column (left of the options/properties cell). Editors that need row-level
 * chrome (the list editors' Add/count/menu) override this; most return null.
 *
 * @return the header controls component, or null for a plain name line
 */
protected JComponent createHeaderControls() {
    return null;
}
```

- [ ] **Step 2: Add the 4-arg `addToFormStacked` overload to `JIPipeDesktopFormPanel`**

Keep the existing 3-arg method, but make it delegate:

```java
public <T extends Component> T addToFormStacked(T component, Component description, MarkdownText documentation) {
    return addToFormStacked(component, description, documentation, null);
}

public <T extends Component> T addToFormStacked(T component, Component description, MarkdownText documentation, JComponent headerControls) {
    int labelRow = numRows;

    if (description != null) {
        GridBagConstraints labelPosition = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                // With header controls the label stays in the label column only;
                // without, it spans into the content column (existing behavior).
                gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
                gridwidth = (headerControls != null) ? 1 : 2;
                gridy = labelRow;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
            }
        };
        contentPanel.add(description, labelPosition);
    }

    if (headerControls != null) {
        GridBagConstraints hc = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = COLUMN_LABELLED_CONTENT;
                gridwidth = 1;
                gridy = labelRow;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        contentPanel.add(headerControls, hc);
    }

    // Options/help on the label line, right-aligned (unchanged).
    Component propertiesComponent = null;
    if (withDocumentation)
        propertiesComponent = createAndAddEntryPropertiesComponent(component, description, labelRow, documentation);

    // The editor spans the full content width on the next line (unchanged).
    GridBagConstraints bodyPosition = new GridBagConstraints() {
        {
            anchor = GridBagConstraints.WEST;
            gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
            gridwidth = 2;
            gridy = labelRow + 1;
            insets = UI_PADDING;
            fill = GridBagConstraints.HORIZONTAL;
            weightx = 1;
        }
    };
    contentPanel.add(component, bodyPosition);

    entries.add(new FormPanelEntry(labelRow, description, component, propertiesComponent, true));
    numRows = labelRow + 2;
    return component;
}
```

(`COLUMN_LABELLED_CONTENT == 1` is the content column; `COLUMN_PROPERTIES == 2`, `COLUMN_LABEL_OR_WIDE_CONTENT == 0` — the existing constants in the file.)

- [ ] **Step 3: Pass header controls from the parameter form panel**

In `JIPipeDesktopParameterFormPanel`'s render loop (around line 545), the stacked branch:

```java
} else {
    JComponent headerControls = (ui instanceof JIPipeDesktopParameterEditorUI e) ? e.createHeaderControls() : null;
    addToFormStacked(wrappedUI, labelPanel, documentation, headerControls);
    uiComponents.add(labelPanel);
}
```

- [ ] **Step 4: Override `createHeaderControls()` in both list editors (additive)**

In each of `JIPipeDesktopListParameterEditorUI` and `JIPipeDesktopParameterCollectionListEditorUI`, add the override (the pass-1 footer and its `addButton`/`menuButton` fields remain untouched in this task — Task 3 consolidates them):

```java
@Override
protected JComponent createHeaderControls() {
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    controls.setOpaque(false);

    JButton addButton = new JButton("Add", JIPipe.RESOURCES.getIcon16("actions/list-add.png"));
    applyFlatHeaderButton(addButton);
    addButton.addActionListener(e -> addNewEntry());
    controls.add(addButton);

    countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    controls.add(countLabel);

    JButton menuButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/hamburger-menu.png"));
    applyFlatHeaderButton(menuButton);
    menuButton.setToolTipText("List options");
    JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
    menu.add(UIUtils.createMenuItem("Clear list", "Removes all items", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::clearList));
    controls.add(menuButton);

    return controls;
}

private static void applyFlatHeaderButton(JButton b) {
    b.setOpaque(false);
    b.setContentAreaFilled(false);
    b.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    b.setFocusable(false);
}
```

If the editors share this exact method, keep a copy in each for now (Task 3 moves it into the shared base). Note: the header `countLabel` is the same field the footer uses — both will show the count until Task 3 removes the footer.

- [ ] **Step 5: Compile**

Run: `mvn -q -pl jipipe-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: VLM-verify**

Run the harness with `--only panel-import-file-list,panel-stack-to-montage`. `da_describe_image`: the list parameter's **name row** now carries `Add`, the `n items` count, and the `⋯` button, left-associated with the label. (The old bottom footer is still present in this intermediate state — expected, removed in Task 3; `Edit` is also absent until Task 4.) No other parameter's name row changed (no non-list editor overrides `createHeaderControls()`).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/api/JIPipeDesktopParameterEditorUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/panels/JIPipeDesktopFormPanel.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/parameters/JIPipeDesktopParameterFormPanel.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopParameterCollectionListEditorUI.java
git commit -m "feat(desktop): createHeaderControls() extension point; list Add/count/menu on the name row (#1339)"
```

---

### Task 3: S2b — shared list-editor base; pagination, grip menu, no hover-×, no footer

**Why a base class:** `JIPipeDesktopListParameterEditorUI` (499 lines) and `JIPipeDesktopParameterCollectionListEditorUI` (508 lines) are ~95% duplicated chrome — `initialize()`, `reload()`, `EntryComponents`, `GripDragTransferHandler`, `GripTransferable`, `handleSelection`, `selectAll/selectNone/invertSelection`, `updateSelectionVisualizations`, `removeSelectedEntries`, `clearList`, `addNewEntry`, `updateCountLabel`, `indexByEntry`, `moveEntryUp/Down/To`, and the **static** `moveItem(list, from, to)`. The spec says S2 "applies identically" to both, and Task 4's modal must reuse the row builder from a single place. So this task extracts a shared base for the **stateless / parameter-level** chrome; the pass-1 *selection* methods are the one exception — inline drops them (grip = menu, no selection) and the modal re-implements them over its own state (see the note in the Interfaces block). The concrete classes become thin subclasses.

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeListParameterEditorUI.java` (the shared base — holds all the chrome)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/ListPaginationModel.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/ui/api/ListPaginationModelTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java` (thin subclass)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopParameterCollectionListEditorUI.java` (thin subclass)
- (No change to `ListReorderMoveTest` — the static `moveItem` moves to the base, and static members are still reachable via the subclass name: `JIPipeDesktopListParameterEditorUI.moveItem(...)` keeps compiling.)

**Interfaces:**
- Produces (consumed by Task 4, the modal):
  ```java
  // new file, package org.hkijena.jipipe.plugins.parameters.ui.api
  public abstract class JIPipeListParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeListParameter> {
      // moved from the two concrete editors (verbatim, where identical):
      protected void initializeBase();                 // formPanel/center/emptyLabel wiring (no footer)
      public void reload();                            // pagination + MENU rows + footer buttons (inline view)
      protected void onBeforeGenerateEntries();        // hook: template fixup (collection) / no-op (plain)
      protected abstract JComponent buildItemContent(int index, JIPipeListParameterItemParameterAccess<?> access);
      public abstract String itemDescription(int index);   // for the modal's search filter
      // stateless / parameter-level ops, shared by inline chrome AND the modal:
      public void clearList();                         // parameter.clear(); setParameter(p, true)
      public void addNewEntry();                       // parameter.addNewInstance(); setParameter(p, true)
      public static boolean moveItem(JIPipeListParameter<?> list, int from, int to);   // pass-1 reorder math
      // row + menu factories (pure — the modal reuses them with its OWN state):
      static class EntryComponents { /* entryPanel, grip, selected flag — moved from the editor */ }
      JComponent buildListRow(int index, RowGripBehavior behavior, ListRowCallbacks cbs);  // grip + buildItemContent + drag
      JPopupMenu buildRowMenu(int index);             // Delete / Move up / Move down (per-row)
      // callbacks the caller supplies (so the modal owns its selection state):
      interface ListRowCallbacks {
          void onGripActivate(int index, boolean shift, boolean ctrl);  // left-click grip (SELECT behavior)
      }
      enum RowGripBehavior { MENU, SELECT }            // MENU = inline (grip left-click opens the menu), SELECT = modal
      protected JComponent createHeaderControls();     // Add, count, ⋯ (Edit added in Task 4)
      ListPaginationModel pagination;                  // current pagination state (rebuilt in reload())
  }
  ```
  **Note — selection is NOT in the base.** In pass 2 the inline view has **no selection** (grip = menu, spec §3.1), so the pass-1 selection machinery (`handleSelection`/`selectAll`/`invertSelection`/`selectNone`/`updateSelectionVisualizations`/`removeSelectedEntries`/the `selectedEntryComponents` + `entryComponentsList` state) is **not** kept as base instance state. It is **re-implemented in the modal** (Task 4), which owns its *own* `List<EntryComponents>` + `Set<EntryComponents>` (the modal is a `JDialog` over the SAME editor instance — it must not touch the inline view's state, and its rows cover the full list whereas the inline shows only the visible page). The modal reuses only the stateless pieces above: `buildListRow(i, SELECT, modalCallbacks)`, `buildRowMenu(i)`, `moveItem`, `clearList`, and the `EntryComponents` type.
  - `JIPipeDesktopListParameterEditorUI`: `extends JIPipeListParameterEditorUI`; `buildItemContent` = the per-item editor (as today: `JIPipe.getParameterTypes().createEditorInstance(...)`); `itemDescription` = `JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(item)` (falls back to `String.valueOf`); `onBeforeGenerateEntries` = no-op.
  - `JIPipeDesktopParameterCollectionListEditorUI`: `extends JIPipeListParameterEditorUI`; `buildItemContent` = the nested mini form panel (as today); `itemDescription` = the collection item's short description (its first/primary field's value, fallback `String.valueOf`); `onBeforeGenerateEntries` = the `ParameterCollectionListTemplate` fixup block (moved verbatim from its `reload()`).
  - **Identical members → base, no override:** `addNewEntry()` (both call `parameter.addNewInstance()` — a `JIPipeListParameter` method), `clearList()`, `updateCountLabel()`, `isUIInline()` (false in both). (`removeSelectedEntries` is **not** a base member — the modal re-implements it over its own selection state.)

- `ListPaginationModel` API (headless-testable):

```java
public final class ListPaginationModel {
    public ListPaginationModel(int total, int pageSize, boolean paginationEnabled);
    public boolean paginationEnabled();
    public int visibleCount();          // paginationEnabled ? min(revealed,total) : total
    public int loadMoreRemaining();     // paginationEnabled ? max(0, total-visibleCount()) : 0
    public boolean showLoadMore();      // loadMoreRemaining() > 0
    public boolean showShowLess();      // paginationEnabled && visibleCount() > pageSize
    public void loadMore();             // revealed = min(revealed+pageSize, total); no-op if !enabled
    public void showLess();             // revealed = pageSize; no-op if !enabled
    public int size();                  // always == total
}
```

- [ ] **Step 1: Write the failing pagination test**

`ListPaginationModelTest` (package `org.hkijena.jipipe.plugins.parameters.ui.api`):

```java
class ListPaginationModelTest {
    @Test void loadMoreRevealsPageSizeAtATime() {
        ListPaginationModel m = new ListPaginationModel(12, 5, true);
        assertEquals(5, m.visibleCount());
        assertEquals(7, m.loadMoreRemaining());
        assertTrue(m.showLoadMore());
        m.loadMore();
        assertEquals(10, m.visibleCount());
        assertEquals(2, m.loadMoreRemaining());
        m.loadMore();
        assertEquals(12, m.visibleCount());
        assertEquals(0, m.loadMoreRemaining());
        assertFalse(m.showLoadMore());
        assertTrue(m.showShowLess());
        m.loadMore();                 // at the end: no-op
        assertEquals(12, m.visibleCount());
    }
    @Test void showLessCollapsesBackToPageSize() {
        ListPaginationModel m = new ListPaginationModel(12, 5, true);
        m.loadMore(); m.loadMore();
        m.showLess();
        assertEquals(5, m.visibleCount());
        assertTrue(m.showLoadMore());
        assertFalse(new ListPaginationModel(3, 5, true).showLoadMore()); // n < pageSize
    }
    @Test void withScrollBarDisablesPagination() {
        ListPaginationModel m = new ListPaginationModel(50, 5, false);
        assertEquals(50, m.visibleCount());   // all rows, scrolling body
        assertEquals(0, m.loadMoreRemaining());
        m.loadMore(); m.showLess();           // no-ops
        assertEquals(50, m.visibleCount());
        assertFalse(m.showLoadMore());
        assertFalse(m.showShowLess());
    }
    @Test void emptyList() {
        ListPaginationModel m = new ListPaginationModel(0, 5, true);
        assertEquals(0, m.visibleCount());
        assertFalse(m.showLoadMore());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=ListPaginationModelTest`
Expected: FAIL — `ListPaginationModel` does not exist (compile error / "cannot find symbol").

- [ ] **Step 3: Implement `ListPaginationModel`**

Implement the API exactly as specified. Pure state, no Swing.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=ListPaginationModelTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Create `JIPipeListParameterEditorUI` and move the chrome**

Create the base class. Move the members listed in the Interfaces block from the two concrete editors into it (from `JIPipeDesktopListParameterEditorUI` as the source of truth — the collection editor's copies are deleted, keeping only its deltas: the `onBeforeGenerateEntries()` template-fixup override and the `buildItemContent()` nested-form-panel override; `isUIImportantLabelEnabled()` only if it differs). Identical members that are **parameter-level / stateless** (`addNewEntry` — both `parameter.addNewInstance()`, `clearList`, `updateCountLabel`, `isUIInline` false, `moveItem`, the `EntryComponents` type, the row/menu/drag factories) move to the base once. The concrete classes keep only: constructors, the `buildItemContent`/`itemDescription`/`onBeforeGenerateEntries` overrides, and any flag difference.

**Delete (pass-1 selection machinery — inline has no selection in pass 2; the Task-4 modal re-implements it):** `handleSelection`, `selectAll`, `selectNone`, `invertSelection`, `removeSelectedEntries` (the instance-state version), `updateSelectionVisualizations`, and the `selectedEntryComponents` / `entryComponentsList` / `lastClickedIndex` *instance fields*. Keep the `EntryComponents` **type** (the modal reuses it) carrying `entryPanel` + grip + a `selected` flag. Remove the inline ⋯ menu's Select-all / Clear-selection / Invert items (it becomes Clear-list only).

- [ ] **Step 6: Apply the new S2 behavior in the base `reload()` / `buildListRow()`**

In the base (the single place — this is what "applies identically" means):
- **Remove the footer** (the pass-1 `JPanel footer` in `initializeBase()` — Add/count/⋯ live on the name row via `createHeaderControls()` from Task 2; move that method into the base and delete the per-editor copies). The inline ⋯ menu is **Clear list only**.
- **Pagination:** hold `ListPaginationModel pagination` (rebuilt each `reload()` from `parameter.size()`, `PAGE_SIZE=5`, `paginationEnabled = settings == null || !settings.withScrollBar()`). `reload()` iterates `i < pagination.visibleCount()` (only the visible page — the Task-4 modal iterates all). After the rows, a small centered footer panel holds **up to two** buttons, each shown when its condition holds (they coexist when partially expanded): `+k more` when `showLoadMore()` (k = `loadMoreRemaining()`) → `pagination.loadMore(); reload();`, and `Show less` when `showShowLess()` → `pagination.showLess(); reload();`. Hide the panel when both are false.
- **`withScrollBar` override (spec §3.1):** when `paginationEnabled()==false`, the body keeps the existing `JIPipeDesktopFormPanel.WITH_SCROLLING` flag + fixed height (already in `initialize()`) and no pagination footer. **Do not change** that path.
- **Empty state (spec §3.4):** empty list → no rows, no footer; just the "This list is empty" hint (`emptyLabel`) under the name row (both the plain and `withScrollBar` cases).
- **Row builder (`buildListRow(index, behavior, cbs)`):** grip at gridx=0 + `buildItemContent(index, access)` at gridx=1 + drag handler (the `GripDragTransferHandler` moves into the base, resolving its target row via the caller-supplied row list). Per spec §3.1:
  - **Grip icon** → `actions/grip-lines-vertical.png` (vertical).
  - **Grip left-click:** `MENU` behavior (inline) → open `buildRowMenu(index)`; `SELECT` behavior (modal, built now, used in Task 4) → `cbs.onGripActivate(index, shift, ctrl)`. Right-click opens the same menu for both.
  - **Move up/down icons** → `actions/go-up.png` / `actions/go-down.png` (both exist, light+dark icons-16). Delete icon stays `actions/delete.png`.
  - **No hover-×:** remove the pass-1 `deleteButton` and its `mouseEntered`/`mouseExited` visibility listeners entirely (spec §3.1 — deletion is grip-menu / modal only).
- **`buildRowMenu(index)`** (pure, per-row): Delete → remove the single item (`list.remove(index)`) + `setParameter(p, true)`; Move up/down → `moveItem(list, index, index±1)` + `setParameter(p, true)`. Parameter-level, so the modal reuses it verbatim.

- [ ] **Step 7: Compile + full test**

Run: `mvn -q -pl jipipe-core compile` then `mvn -q -pl jipipe-core test -Dtest=ListPaginationModelTest,ListReorderMoveTest`
Expected: BUILD SUCCESS + both pass (`ListReorderMoveTest` unchanged — the static `moveItem` is reachable via the subclass name).

- [ ] **Step 8: VLM-verify**

Run the harness with `--only panel-import-file-list,panel-stack-to-montage`. `da_describe_image`: for a list with more than 5 items, the body shows **5 rows** then a `+k more` button (not a scroll view); each row has a **vertical grip** and no hover-× button; the name row shows Add/count/⋯; **no bottom footer** under the body. A list with ≤5 items shows all rows and no `+k more`. The collection-list panel (if in the set) shows nested mini-form rows with the same chrome. Report the verdict.

- [ ] **Step 9: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeListParameterEditorUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/ListPaginationModel.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/ui/api/ListPaginationModelTest.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListParameterEditorUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopParameterCollectionListEditorUI.java
git commit -m "refactor(desktop): shared list-editor base; inline pagination, grip menu, no hover-×, no footer (#1339)"
```

---

### Task 4: S2c — list editor modal (`JIPipeDesktopListEditorDialog`)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListEditorDialog.java` (the modal)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeListParameterEditorUI.java` (add **Edit** to `createHeaderControls()`)
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java` (add `dialog-list-editor` + `dialog-list-collection` showcases)

**Interfaces:**
- Consumes (from Task 3): the base `JIPipeListParameterEditorUI` — `buildListRow(index, RowGripBehavior.SELECT, callbacks)`, `buildRowMenu(index)`, `moveItem(list, from, to)`, `clearList()`, the `EntryComponents` type, and `itemDescription(i)`. (The selection state itself is the modal's own — see the note in Task 3.)
- Produces (consumed by the base's header controls):
  ```java
  public final class JIPipeDesktopListEditorDialog extends JDialog {
      public JIPipeDesktopListEditorDialog(Window owner, JIPipeListParameterEditorUI listEditor);
      // Close-only; hosts the full editor for listEditor's JIPipeListParameter.
  }
  ```

- [ ] **Step 1: Build the dialog**

`JIPipeDesktopListEditorDialog extends JDialog` (APPLICATION_MODAL):
- Ctor `(Window owner, JIPipeListParameterEditorUI listEditor)`. Title = the list parameter's name + " — Edit". Initial size `640×520`, resizable. `DISPOSE_ON_CLOSE`.
- **No OK/Cancel.** A single **Close** button (a plain `JButton` wired to `dispose()`). The window X and **Esc** also `dispose()`. (Spec §3.3 — Close-only.)
- **Top bar:** a search/filter `JTextField`. Filtering hides non-matching rows (match `listEditor.itemDescription(i)` against the needle, case-insensitive); selection state is preserved (hidden-but-selected rows stay selected).
- **Toolbar (above the rows):** **Select all** / **Invert selection** / **Clear selection** / **Remove selected** / **Clear list** — modal-local operations over the modal's own `rows`/`selected` state (re-implementing the pass-1 grip-selection handlers; **not** owner methods, which Task 3 removed from the base). After each, re-render the modal rows and the owner inline view. **Clear list** delegates to `listEditor.clearList()`.
- **Body:** a `JScrollPane` over a `JPanel` (BoxLayout Y) of **all** rows (no pagination). The modal owns its **own** row + selection state — it must NOT touch the inline view's (the modal and the inline view share the same editor instance, and the inline only renders the visible page while the modal renders all). Build `List<EntryComponents> rows` for every `i in 0..size` via `listEditor.buildListRow(i, RowGripBehavior.SELECT, modalCallbacks)`, where `modalCallbacks.onGripActivate(index, shift, ctrl)` updates the modal's own `Set<EntryComponents> selected` + `lastClickedIndex` (a re-implementation of the pass-1 `handleSelection` range/toggle logic over the modal's state) and repaints the selected rows' background highlight. Grip right-click → `listEditor.buildRowMenu(i)`. Row-content clicks edit, not select. Drag reorders (the base's `GripDragTransferHandler`, target resolved via the modal's `rows` list).
- **Selection ops** (modal-local, re-implemented over the modal's `rows`/`selected`): **Select all** = all rows; **Invert** = flip each; **Clear selection** = empty; **Remove selected** = for each selected row, `list.remove(index)` **in descending index order** (so earlier indices stay valid), then `setParameter(p, true)` + re-render the modal rows and the owner inline view; **Clear list** = `listEditor.clearList()` + re-render.
- **Live edits:** rows edit the **same** `JIPipeListParameter` instance in place (no copy). After any change (structural or value), re-render the modal rows and the owner's inline view. Structural ops act live. (The modal is app-modal and sits on top; the inline refresh is correct on return.)

- [ ] **Step 2: Wire the Edit button**

In the base's `createHeaderControls()` (Task 3), add an **Edit** button between Add and the count label:

```java
JButton editButton = new JButton("Edit"); // text-only; no icon added (verify actions/edit-list.png exists first, use it if it does)
applyFlatHeaderButton(editButton);
editButton.addActionListener(e -> new JIPipeDesktopListEditorDialog(
        getRootPane() != null ? getRootPane() : null, this).setVisible(true));
controls.add(editButton);
```

- [ ] **Step 3: Compile + full test**

Run: `mvn -q -pl jipipe-core compile` then `mvn -q -pl jipipe-core test -Dtest=ListPaginationModelTest,ListReorderMoveTest`
Expected: BUILD SUCCESS + both pass (no regression).

- [ ] **Step 4: VLM-verify (off-screen render — the modal is app-modal)**

Add harness showcases `dialog-list-editor` and `dialog-list-collection`: construct a synthetic list (a `PathList` with 8 items / a `JIPipeParameterCollectionList` with 2 items), get an editor instance for it (via `JIPipe.getParameterTypes().createEditorInstance(...)` on a synthetic parameter access, as the list editors are registered editors), and render `new JIPipeDesktopListEditorDialog(null, editor).getContentPane()` with `ScreenshotRenderer.renderFixed(contentPane, 640, 520, ...)`. `da_describe_image`: the modal shows **all rows** (no pagination), a **search box**, the **Select all / Invert selection / Clear selection / Remove selected / Clear list** toolbar, and a single **Close** button (no OK/Cancel). For the collection list: **nested fully-editable rows** (Review Focus #5). The showcases can stay (useful for the S8 audit). Report the verdict.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopListEditorDialog.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeListParameterEditorUI.java \
        jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java
git commit -m "feat(desktop): list editor modal — Close-only, bulk ops, search, grip selection (#1339)"
```

---


### Task 5: S3 — RichSelectionBox keyboard navigation + left-aligned value

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/selection/JIPipeDesktopRichSelectionBox.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/selection/RichSelectionBoxKeyModelTest.java`

**Interfaces:**
- Produces: a package-private, headless-testable **popup key model** that the box's key listeners delegate to. Shape:

```java
// nested or same-package final class
static final class PopupKeyModel {
    // state set by the box each time the popup is shown:
    //   boolean multiSelect, boolean twoPane (grouped), the visible options (filtered),
    //   the current focus region (SEARCH, or LIST + which list index + list index within pane)
    enum Focus { SEARCH, LIST }
    // ...
    KeyAction onKey(int keyCode, boolean shift, boolean ctrl);
}
enum KeyAction { NOOP, FOCUS_LIST, FOCUS_SEARCH, MOVE_ROW, SELECT_FIRST_AND_CLOSE,
                 SELECT_ROW_AND_CLOSE, TOGGLE_ROW, CLOSE_KEEP }
```

The exact field/method signatures are the implementer's, but the **decision matrix** is pinned by the test below.

- [ ] **Step 1: Write the failing test**

`RichSelectionBoxKeyModelTest` — drive the model with a fixed visible-option list and assert the action matrix (single- and multi-select, single- and two-pane):

```java
class RichSelectionBoxKeyModelTest {
    // helper builds a PopupKeyModel with visible options [A, B, C], a focus start, single/multi, panes.
    @Test void enterInSearchSingleSelectsFirstFilteredAndCloses() {
        // focus=SEARCH, key=ENTER, single-select, visible=[A,B,C] -> SELECT_FIRST_AND_CLOSE (target A)
    }
    @Test void enterInSearchMultiClosesKeepingToggles() {
        // focus=SEARCH, key=ENTER, multi-select -> CLOSE_KEEP (current toggles unchanged)
    }
    @Test void enterInListSingleSelectsRowAndCloses() {
        // focus=LIST row0, key=ENTER, single-select -> SELECT_ROW_AND_CLOSE (target row0)
    }
    @Test void enterInListMultiTogglesRowStaysOpen() {
        // focus=LIST row1, key=ENTER, multi-select -> TOGGLE_ROW (target row1, popup stays open)
    }
    @Test void downFromSearchFocusesListRow0() {
        // focus=SEARCH, key=DOWN -> FOCUS_LIST (row0 selected)
    }
    @Test void upFromListRow0ReturnsToSearch() {
        // focus=LIST row0, key=UP -> FOCUS_SEARCH
    }
    @Test void downInListMovesSelection() {
        // focus=LIST row0, key=DOWN -> MOVE_ROW (row1)
    }
    @Test void escClosesWithoutChange() {
        // any focus, key=ESCAPE -> CLOSE_KEEP with no selection change
    }
    @Test void tabCyclesSearchToPanes() {
        // two-pane: focus=SEARCH key=TAB -> FOCUS_LIST(pane0); focus=LIST pane0 key=TAB -> FOCUS_LIST(pane1); wrap back to SEARCH
    }
}
```

Write the assertions against the model's returned `KeyAction` (and its target index/value where the action carries one). The model must be constructible without a shown popup (pure state).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=RichSelectionBoxKeyModelTest`
Expected: FAIL — `PopupKeyModel` does not exist.

- [ ] **Step 3: Implement `PopupKeyModel`**

Implement the decision matrix exactly as the test pins it. The model owns: `multiSelect`, `twoPane`, the visible options (set from the filter), the focus region + current row index, and the current selected values. `onKey` returns the action; the box translates actions to component operations (move list selection, scroll-to-visible, close popup, fire the selection handler via the existing `fireSelectionChanged()`).
- Single-select Enter-in-search → confirm the **first visible/filtered** option and close.
- Single-select Enter-in-list → confirm the focused row and close.
- Multi-select Enter-in-list → toggle the focused row, stay open.
- Multi-select Enter-in-search → close keeping the current toggles.
- ↓ from search → list row 0; ↑ on row 0 → search; ↑/↓ in list move selection; Esc → close unchanged; Tab/Shift-Tab cycle search → pane A → pane B → search (single-pane: search → list → search).
- Initialize the model's selection to the currently-selected value(s) on popup open so an immediate Enter confirms the status quo.

- [ ] **Step 4: Wire the model into the box + left-align the value**

- In the box, on popup open (after `rebuildListModels`/`refreshListSelection`), construct/reset the model with the current visible options, `multiSelect`, `twoPaneGroups`, and the current selection.
- Add key handling to `searchField` and each `paneList` that calls `model.onKey(...)` and applies the returned action. Keep the existing type-to-filter behavior on `searchField` (typing filters; the first match auto-selects while typing).
- **Left-align the value:** in the button constructor, `setHorizontalAlignment(SwingConstants.LEADING)` (keep `setFocusPainted(false)`).

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=RichSelectionBoxKeyModelTest`
Expected: PASS.

- [ ] **Step 6: Compile + full test**

Run: `mvn -q -pl jipipe-core compile` then `mvn -q -pl jipipe-core test`
Expected: BUILD SUCCESS + all tests green (no regression to the enum/dynamic-set/font-family boxes).

- [ ] **Step 7: VLM-verify**

Run the harness with `--only panel-enum-box,panel-roi3d-statistics,panel-font`. `da_describe_image`: the box's value text is **left-aligned** (matching the combo boxes), not centered. (Keyboard behavior is not VLM-verifiable — it is covered by the key-model test.) Report the verdict.

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/selection/JIPipeDesktopRichSelectionBox.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/selection/RichSelectionBoxKeyModelTest.java
git commit -m "feat(desktop): RichSelectionBox keyboard navigation (key model) + left-aligned value (#1339)"
```

---

### Task 6: S4 — font popout: form-panel rows, size field W+H, no clipped heights

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopFontParameterEditorUI.java`
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java` (add the `popup-font` showcase)

**Interfaces:**
- Consumes: `JIPipeDesktopFormPanel` (`addToForm`), the existing `buildFamilyBox()`, `buildSizeSpinner()`, `buildStyleToggles()`, `buildColorSection()`, `buildPreviewBand()`.
- Produces: a **public seam** `JComponent getPopoutContent()` on `JIPipeDesktopFontParameterEditorUI` that builds (or returns) the popout's content `JPanel`. `togglePopup()` is refactored to use it, and the screenshot harness (Task 6 step 6) renders it directly — a shown `JPopupMenu` can't be captured, but its content panel can.

- [ ] **Step 1: Replace the hand-rolled rows with form-panel rows (behind a public seam)**

Refactor `togglePopup()` so the popout content is built by a new **public** method:

```java
public JComponent getPopoutContent() {
    JIPipeDesktopFormPanel form = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
    form.addToForm(buildPreviewBand(), new JLabel(""));           // preview band, empty label
    form.addToForm(buildFamilyBox(), new JLabel("Family"));
    form.addToForm(buildSizeSpinner(), new JLabel("Size"));
    form.addToForm(buildStyleToggles(), new JLabel("Style"));
    form.addToForm(buildColorSection(), new JLabel("Color"));
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(form, BorderLayout.NORTH);
    return wrap;
}

private void togglePopup() {
    if (popup != null && popup.isVisible()) { popup.setVisible(false); return; }
    popup = new JPopupMenu();
    popup.add(getPopoutContent());
    popup.show(preview, 0, preview.getHeight() + 2);
    preview.repaint();
}
```

The form panel's GridBagLayout sizes the **label column to the widest label** under the active theme font (font-dependent) — this is the alignment fix (spec §5) and removes the `fieldRow` helper. The preview band keeps its fixed preferred **width** (it drives the popout's column width); give it an empty `JLabel` so it shares the label column.

- [ ] **Step 2: Fix the size field width *and* height**

In `buildSizeSpinner()`, replace the hardcoded `spinner.setPreferredSize(new Dimension(60, 22))` with a **width** pin only, taking the **height from the spinner's natural default** (do not pin a height that undercuts the theme font):

```java
Dimension d = spinner.getPreferredSize();
spinner.setPreferredSize(new Dimension(80, Math.max(d.height, 24)));
```

(Width ~80px fits 3-digit sizes + arrows; height is the spinner editor's default or 24px whichever is larger — never a flat 22.)

- [ ] **Step 3: No clipped control heights anywhere in the popout**

In `buildStyleToggles()`, the B/I buttons are `setPreferredSize(new Dimension(28, 22))`. Apply the same rule: pin the width (28), take the height from each button's natural default (`Math.max(b.getPreferredSize().height, 24)`), never a flat 22. Do the same for any other fixed-size control in the popout that pins a sub-font height. (The label cells now come from the form panel and are not pinned.)

- [ ] **Step 4: Keep the preview band and color section behavior**

Do not change `buildPreviewBand()`'s width-driven logic, the embedded color section, or the popout-hides-before-custom-dialog behavior (spec §5 last bullet).

- [ ] **Step 5: Compile**

Run: `mvn -q -pl jipipe-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: VLM-verify (via the `getPopoutContent()` seam)**

The popout is a `JPopupMenu`, which the harness can't capture while shown. Render its **content** directly: in `ScreenshotUiCommand`, add a `popup-font` showcase that (a) builds the `panel-font` showcase panel, (b) finds the font editor via the form panel entry content (`panel.getEntries()` → the `JIPipeDesktopFontParameterEditorUI`), and (c) renders `fontEditor.getPopoutContent()` with `ScreenshotRenderer.renderComponent(content, 320, outDir.resolve("popup-font.png"))` (the preview band's fixed width drives the column width). `da_describe_image` on `popup-font.png`: the **Family / Size / Style / Color** labels form a clean left-aligned column (label column auto-sized, top rows no longer shifted left relative to the bottom); the **size field** digits are fully visible (not clipped top or bottom) and 3-digit values fit; the B/I toggles are not clipped. Report the verdict. (If the user runs a larger-font theme, re-verify — Review Focus #6.) The `popup-font` showcase can stay (useful for the S8 audit).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopFontParameterEditorUI.java \
        jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java
git commit -m "fix(desktop): font popout uses form-panel rows; size field width+height, no clipped control heights (#1339)"
```

---

### Task 7: S5 — color dialog rework (8 items)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/color/JIPipeDesktopColorChooserDialog.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/color/ColorChooserDialogRefreshTest.java`

**Interfaces:**
- Produces (test seams, package-private, in the dialog):
  ```java
  String getHexText();          // hexField.getText()
  JComponent getRedRail();      // rRail
  JComponent getHsbWell();      // hsbWell (null until the HSB tab is built)
  ```
  (Package-private so the same-package test can drive the input plumbing without Swing-structure assertions.)

- [ ] **Step 1: Write the failing regression test**

`ColorChooserDialogRefreshTest` (package `org.hkijena.jipipe.desktop.commons.components.color`):

```java
class ColorChooserDialogRefreshTest {
    @BeforeAll static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) { Assumptions.assumeTrue(false, "headless environment"); }
        } catch (Throwable t) { Assumptions.assumeTrue(false, "AWT not available: " + t); }
        JIPipe.ensureInstance();
    }
    static void press(JComponent c, int x, int y) {
        c.dispatchEvent(new MouseEvent(c, MouseEvent.PRESS, 0L, 0, x, y, 1, false));
    }
    @Test void railPressUpdatesHexField() {
        JIPipeDesktopColorChooserDialog d = new JIPipeDesktopColorChooserDialog(null, "t", new Color(0, 0, 0), false);
        String before = d.getHexText();
        JComponent rail = d.getRedRail();
        press(rail, Math.max(1, rail.getWidth() / 2), 5);   // press mid-rail -> Red ~ 127
        assertTrue(!before.equals(d.getHexText()),
                "hex field must change after a rail press (refresh() must run)");
    }
    @Test void wellPressUpdatesHexField() {
        JIPipeDesktopColorChooserDialog d = new JIPipeDesktopColorChooserDialog(null, "t", new Color(0, 0, 0), false);
        d.showLabTab();  // build the HSB/LAB wells
        String before = d.getHexText();
        JComponent well = d.getHsbWell();
        assertNotNull(well, "HSB well must exist after the HSB tab is built");
        press(well, Math.max(1, well.getWidth() / 2), Math.max(1, well.getHeight() / 2));
        assertTrue(!before.equals(d.getHexText()),
                "hex field must change after a well press (refresh() must run)");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=ColorChooserDialogRefreshTest`
Expected: FAIL — `getHexText`/`getRedRail` do not exist, and even after adding the seams the assertion fails on current code because `refresh()` is dead from `setColor()` (hex never updates).

- [ ] **Step 3: Fix the `refresh()` re-entrancy bug**

In `JIPipeDesktopColorChooserDialog`:
- `setColor(Color c)` becomes: assign `currentColor = c;` then `refresh();` — **remove** the `updating` guard/set from `setColor`.
- `refresh()` becomes:
  ```java
  private void refresh() {
      if (updating) return;         // re-entrancy guard for the real cycle
      updating = true;              // refresh -> rail.setValue -> listener -> setColor -> refresh
      try {
          // ...the existing body (set all rails, repaintGradient, hexField.setText, well/preview repaints)...
      } finally {
          updating = false;
      }
  }
  ```
  So a nested `refresh` (from a rail listener mid-update) returns early, but the top-level call runs to completion. Add the three package-private test seams (`getHexText`, `getRedRail`, `getHsbWell`).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=ColorChooserDialogRefreshTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Palette area — no horizontal scrollbar, wrap, 16 pure colors, merge, alignment, tooltips**

In `buildPalettes()` and the palette scroll setup:
- **Item 1:** set the palette `JScrollPane` to `HORIZONTAL_SCROLLBAR_NEVER`; make swatch rows **wrap** (a `JPanel` per row of ≤ N swatches where N fits the dialog width at 18px swatches + 4px gaps) so only vertical scrolling occurs.
- **Item 5:** replace `pureColors()` (currently 9 entries) with the 16 4-bit colors in this exact order: `#000000 #550000 #AA0000 #FF0000 #005500 #00AA00 #00FF00 #555500 #AAAA00 #FFFF00 #000055 #0000AA #0000FF #550055 #AA00AA #FF00FF`.
- **Item 7:** render "Pure colors" and "GNOME HIG" as **one untitled block** (no section titles between them; a single 8px gap). "Last selected" and "Saved" keep their titles.
- **Item 6:** one shared left margin for section titles and swatch rows; swatch cells uniform `18×18` with `4px` gap; the "Saved" row's star button right-aligns to the same edge as section titles.
- **Item 8:** every swatch tooltip (pure, HIG, last selected, saved) and the current-swatch header tooltip shows: name (if any), `#RRGGBB`, `RGB(r, g, b)`, `HSB(h°, s%, b%)`, `L*a*b*(L, a, b)` — via `ColorSpaces.toHsb` / `toLab`.

- [ ] **Step 6: Pill tabs + RGB label alignment + rail hit target**

- **Item 2:** apply `JIPipeDesktopModernPillTabbedPaneUI` to `spaceTabs` when the modern theme is active (mirror `JIPipeDesktopTabPane`); fall back to the default UI for the classic theme.
- **Item 3:** in `railRow`, give the label cell a **fixed width** (e.g. `34px`, `setPreferredSize`) right-aligned, so all rails (RGB/HSB/LAB incl. α) start at the same x and labels form a clean column.
- **Item 4 (usability part):** keep the rail visual height ~12px but widen the **row's clickable area** — the whole `railRow` (~22px) forwards press/drag to the rail (a mouse listener on the row panel converting x to the rail's span). (The actual no-update symptom is fixed in Step 3.)

- [ ] **Step 7: Compile + full test**

Run: `mvn -q -pl jipipe-core compile` then `mvn -q -pl jipipe-core test -Dtest=ColorChooserDialogRefreshTest`
Expected: BUILD SUCCESS + PASS.

- [ ] **Step 8: VLM-verify**

Run the harness with `--only dialog-color,dialog-color-lab`. `da_describe_image` each: (a) the space tabs render as **pills** (modern theme); (b) the palette area has **no horizontal scrollbar**, swatches wrap, the 16 pure colors + GNOME HIG form one untitled block; (c) the RGB/HSB/LAB rail labels form a clean right-aligned column; (d) swatch tooltips carry the hex + RGB/HSB/LAB (verify by hovering a swatch or asserting the tooltip string in code — the harness can't capture a hover, so **also assert the tooltip text** of one swatch in code or report it as a manual check). Report the verdict; note the refresh fix is test-covered, not VLM-covered.

- [ ] **Step 9: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/color/JIPipeDesktopColorChooserDialog.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/color/ColorChooserDialogRefreshTest.java
git commit -m "fix(desktop): color dialog refresh() re-entrancy bug + pill tabs, palette wrap, 16 colors, label column (#1339)"
```

---

### Task 8: S6 — number slider style (annotation enum + control + editor branch)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/NumberParameterSettings.java` (add `Style` enum + `style()`)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/slider/JIPipeDesktopNumberSlider.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/slider/NumberSliderMathTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopNumberParameterEditorUI.java` (editor branch)

**Interfaces:**
- Produces (consumed by the editor + the test):
  - `NumberParameterSettings.Style { FIELD, SLIDER }`, `Style style() default Style.FIELD;`
  - `JIPipeDesktopNumberSlider` public API: `getValue(): double`, `setValue(double)`, `setRange(double min, double max, double step)`, `setPrecision(int decimals)`, standard `addChangeListener`/`removeChangeListener`.
  - **Static math** (headless-testable, the source of truth the control uses):
    ```java
    public static double snap(double value, double min, double max, double step);
    public static double valueFromFraction(double frac, double min, double max, double step); // frac clamped to [0,1], then snap
    public static double fractionFromValue(double value, double min, double max);
    public static double fineStep(double step);                 // step / 10
    public static Double parseCommit(String text, double min, double max, double step);        // null if empty/invalid
    public static String format(double value, int precision);   // trim trailing zeros
    ```

- [ ] **Step 1: Add the annotation enum**

In `NumberParameterSettings`:

```java
enum Style { FIELD, SLIDER }

/**
 * The editor style for this number parameter.
 * @return FIELD (default, text field + spinners) or SLIDER (bounded range only)
 */
Style style() default Style.FIELD;
```

- [ ] **Step 2: Write the failing math test**

`NumberSliderMathTest`:

```java
import org.hkijena.jipipe.plugins.parameters.library.primitives.NumberParameterSettings;
import org.hkijena.jipipe.plugins.parameters.ui.library.JIPipeDesktopNumberParameterEditorUI;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
```

```java
class NumberSliderMathTest {
    @Test void snapRoundsToStepAndClamps() {
        assertEquals(0.37, JIPipeDesktopNumberSlider.snap(0.374, 0, 1, 0.01), 1e-9);
        assertEquals(1.0,  JIPipeDesktopNumberSlider.snap(0.999, 0, 1, 0.01), 1e-9);  // clamp high
        assertEquals(0.0,  JIPipeDesktopNumberSlider.snap(-0.5,  0, 1, 0.01), 1e-9);  // clamp low
        assertEquals(3.0,  JIPipeDesktopNumberSlider.snap(2.4,   0, 10, 1.0), 1e-9);  // snap to step 1
    }
    @Test void fractionRoundTrip() {
        double v = 0.37;
        assertEquals(v, JIPipeDesktopNumberSlider.valueFromFraction(
                JIPipeDesktopNumberSlider.fractionFromValue(v, 0, 1), 0, 1, 0.01), 1e-9);
        assertEquals(0.0, JIPipeDesktopNumberSlider.valueFromFraction(0.0, 0, 1, 0.01), 1e-9);
        assertEquals(1.0, JIPipeDesktopNumberSlider.valueFromFraction(1.0, 0, 1, 0.01), 1e-9);
    }
    @Test void fineStepIsStepOverTen() {
        assertEquals(0.001, JIPipeDesktopNumberSlider.fineStep(0.01), 1e-9);
    }
    @Test void parseCommitValidInvalidEmpty() {
        assertEquals(0.5, JIPipeDesktopNumberSlider.parseCommit("0.5", 0, 1, 0.01), 1e-9);
        assertEquals(1.0, JIPipeDesktopNumberSlider.parseCommit("5", 0, 1, 0.01), 1e-9);  // clamp
        assertNull(JIPipeDesktopNumberSlider.parseCommit("", 0, 1, 0.01));
        assertNull(JIPipeDesktopNumberSlider.parseCommit("abc", 0, 1, 0.01));
    }
    @Test void formatTrimsTrailingZeros() {
        assertEquals("0.5", JIPipeDesktopNumberSlider.format(0.5, 2));
        assertEquals("1",   JIPipeDesktopNumberSlider.format(1.0, 2));
    }
    @Test void annotationDefaultIsField() throws Exception {
        // A getter annotated with @NumberParameterSettings and no style() -> FIELD.
        // (registry/behavior guard, not UI structure)
        Method m = SliderProbe.class.getMethod("getA");
        NumberParameterSettings s = m.getAnnotation(NumberParameterSettings.class);
        assertEquals(NumberParameterSettings.Style.FIELD, s.style());
    }
    @Test void shouldUseSliderBoundedOnly() {
        NumberParameterSettings slider = new NumberParameterSettings() {
            @Override public Class<? extends Annotation> annotationType() { return NumberParameterSettings.class; }
            @Override public double min() { return 0; }
            @Override public double max() { return 1; }
            @Override public double step() { return 0.01; }
            @Override public NumberParameterSettings.Style style() { return NumberParameterSettings.Style.SLIDER; }
        };
        NumberParameterSettings field = new NumberParameterSettings() {
            @Override public Class<? extends Annotation> annotationType() { return NumberParameterSettings.class; }
        };
        // bounded + SLIDER -> slider
        assertTrue(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(slider, 0, 1));
        // unbounded (defaults) + SLIDER style -> FIELD fallback (Review Focus #2)
        assertFalse(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(slider, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        // default settings (FIELD) -> never slider
        assertFalse(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(field, 0, 1));
        // no settings at all -> never slider
        assertFalse(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(null, 0, 1));
    }
    static class SliderProbe {
        @JIPipeParameter("a") @NumberParameterSettings(min = 0, max = 1, step = 0.01)
        public double getA() { return 0; }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=NumberSliderMathTest`
Expected: FAIL — `JIPipeDesktopNumberSlider` and `NumberParameterSettings.Style` do not exist.

- [ ] **Step 4: Implement the static math**

Implement the six static methods exactly per the pinned behavior (snap = round to nearest step then clamp; `valueFromFraction` clamps frac to [0,1] before mapping; `parseCommit` uses locale-insensitive parse, returns null on empty/number-parse failure, else snap). Add the `Style` enum (Step 1).

- [ ] **Step 5: Run math test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=NumberSliderMathTest`
Expected: PASS.

- [ ] **Step 6: Implement `JIPipeDesktopNumberSlider` (VLM design loop)**

A theme-aware, custom-painted `JComponent` using the static math. Functional baseline (spec §7.2): track (~6px) with a filled portion to the value (primary color) + a thumb (~12px); press/drag anywhere sets the value (`x → value` via `valueFromFraction`, step-snapped); **Shift-drag = fine** (`fineStep`); arrow keys ±step (Left/Down −, Right/Up +, Home/End = min/max; component is focusable); double-click or F2 opens an **inline** overlaid monospace text field pre-filled with the value (Enter commits via `parseCommit`, invalid/empty reverts, Esc reverts, focus-lost reverts; thumb hidden while editing); tooltip `value (min – max, step)`; fires `ChangeListener` on user-caused changes only.

**VLM design loop (spec §7.2, user ruling):** render the control across its states (default, mid-drag with thumb, inline-editing, disabled, focused, min, max) in a small off-screen showcase, run `da_describe_image` with a critique prompt (state clarity, hit-target size, thumb affordance, theme fit, focus ring, disabled look, alignment with the other number editors), and **iterate the visual proportions/affordances until the design is approved**. Record the approved proportions. The functional behavior is already pinned by the math test; the loop only shapes the pixels.

- [ ] **Step 7: Editor branch with a pure, testable decision**

Add a **static decision method** on `JIPipeDesktopNumberParameterEditorUI` (pure — no JIPipe/Swing state, so it is unit-testable headlessly):

```java
/** True iff this parameter should render the slider (spec §7.3: bounded range + SLIDER style). */
static boolean shouldUseSlider(NumberParameterSettings settings, double min, double max) {
    return settings != null
            && settings.style() == NumberParameterSettings.Style.SLIDER
            && max > min
            && !Double.isInfinite(min) && !Double.isInfinite(max);
}
```

In `initialize()`: if `shouldUseSlider(settings, getMinimumValue(), getMaximumValue())`, build a `JIPipeDesktopNumberSlider` (range = min/max/step from the editor's existing `getMinimumValue()/getMaximumValue()/getStep()`; precision from the field class's integer-ness) **instead of** `numberField` + the up/down buttons. Otherwise the existing FIELD path (unchanged). When `style == SLIDER` but the decision is false (unbounded), the FIELD path is used and a **one-time log warning** is emitted (Review Focus #2).
- Sync: the slider's `ChangeListener` → `setCurrentValue(...)`; the editor's `reload()` → `slider.setValue(getCurrentValue())` (guard against feedback like the FIELD path does).

- [ ] **Step 8: Example adoption (if trivially safe, else none)**

If a node already annotates an opacity-style `double` with `@NumberParameterSettings(min = 0, max = 1, step = ...)` and adding `style = SLIDER` is trivially safe (e.g. `RenderOverlayAlgorithm.getOpacity`, `RoiToRGBAlgorithm`, or `RoiDrawer`), add `style = NumberParameterSettings.Style.SLIDER` to **one or two** of them as the example adoption. Otherwise add none (the capability ships; adoptions come later). Do not change any node's semantics.

- [ ] **Step 9: Compile + full test**

Run: `mvn -q -pl jipipe-core compile` then `mvn -q -pl jipipe-core test -Dtest=NumberSliderMathTest`
Expected: BUILD SUCCESS + PASS.

- [ ] **Step 10: VLM-verify**

Add a harness showcase `panel-number-slider`: a synthetic `JIPipeDynamicParameterCollection` (mirror `FontParameterShowcase`) with one `Double` field carrying a **runtime annotation instance** — the proven pattern (see `MaskDrawerPlugin2D:398`):

```java
private static class NumberSliderShowcase extends JIPipeDynamicParameterCollection {
    NumberSliderShowcase() {
        addParameter("opacity", Double.class, "Opacity", "A 0-1 opacity",
                new NumberParameterSettings() {
                    @Override public Class<? extends Annotation> annotationType() { return NumberParameterSettings.class; }
                    @Override public double min() { return 0; }
                    @Override public double max() { return 1; }
                    @Override public double step() { return 0.01; }
                    @Override public NumberParameterSettings.Style style() { return NumberParameterSettings.Style.SLIDER; }
                });
    }
}
```

(`getAnnotationOfType(NumberParameterSettings.class)` resolves via the annotation map set by `addParameter`.) Register `"panel-number-slider" -> "<number-slider>"` in `SHOWCASE` and branch in `renderNodePanels` like the `<font-parameter>` case. Render it; `da_describe_image`: the number parameter renders as a **single slider** (track + thumb at the value position), not a text field + spinners; the thumb position matches the value; no min/max labels are drawn on the control. Report the verdict (this also confirms the VLM design loop output).

- [ ] **Step 11: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/primitives/NumberParameterSettings.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/slider/JIPipeDesktopNumberSlider.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/slider/NumberSliderMathTest.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopNumberParameterEditorUI.java
git commit -m "feat(desktop): opt-in number slider style (annotation enum + JIPipeDesktopNumberSlider + editor branch) (#1339)"
```
(Add any adopted node file(s) to the `git add` list.)

---

### Task 9: S7 — HTMLText editor contract + `JIPipeDesktopHTMLEditor` widget modernization

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/markup/JIPipeDesktopHTMLEditor.java` (widget)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopHTMLTextParameterEditorUI.java` (form contract)

**Interfaces:**
- **Kept stable (public API, spec §8.2):** `Mode` enum (`Compact`, `Full`, `HTML`), `getHTML(): String`, `setText(String)`, `getDocument()`, the `WITH_DIALOG_EDITOR_BUTTON` flag, the `getHTMLByDialog` path.
- Produces: a **compact ⇄ expanded** toolbar toggle replacing the mode popup menu; a **3-state** WYSIWYG / Preview / Source view; `UIUtils.createControlBorder()` chrome on the widget.

- [ ] **Step 1: Compact mode becomes the default shape**

In `JIPipeDesktopHTMLEditor`, make the **single-row icon-only toolbar** the default (24px tall): B, I, U, foreground color, insert image, a separator `|`, the **mode/view toggle**, and (if `WITH_DIALOG_EDITOR_BUTTON`) the Edit-in-dialog button. Replace the janky "mode" `JPopupMenu` (the `compactModeItem`/`fullModeItem`/`htmlModeItem` menu) with a simple **compact ⇄ expanded** toggle button (expanded = the existing Full toolbars). Buttons are flat 24×24 with hover tooltips.

- [ ] **Step 2: 3-state view toggle (WYSIWYG / Preview / Source)**

Add a view toggle that cycles **WYSIWYG → Preview → Source**:
- **WYSIWYG** = the existing editable `JTextPane`/`JEditorPane` (current behavior).
- **Preview** = a non-editable `JEditorPane` rendering the current HTML read-only (same content).
- **Source** = a plain `JTextArea` with `text/html` (no syntax-highlight dependency — #1124 will replace this later). Edits in Source update the WYSIWYG/Preview on toggle-back.
Keep `Mode` for the existing external callers; the internal 3-state is a separate view concern (map the old `Compact`/`Full`/`HTML` modes to the toolbar density + view as needed so existing call sites — the parameter editor and the expression/function-builder contexts — keep working).

- [ ] **Step 3: Consistent chrome**

Apply `UIUtils.createControlBorder()` to the whole widget; toolbar buttons flat 24×24; **remove the redundant outer border** the parameter editor adds (the widget now carries it).

- [ ] **Step 4: Form-contract normalization (parameter editor)**

In `JIPipeDesktopHTMLTextParameterEditorUI`:
- Set `isUIInline() = false` (stacked — the widget is compound), **or** a compact inline value + **Edit** button if the compact widget proves usable (decide in implementation, verify with VLM). The #1124 seam ("editor component + Edit button") is preserved either way.
- Drop the editor's own `setBorder` (the widget carries it per Step 3); keep consistent height/border; no self-drawn name (already true).

- [ ] **Step 5: Compile + full test**

Run: `mvn -q -pl jipipe-core compile` then `mvn -q -pl jipipe-core test`
Expected: BUILD SUCCESS + all tests green (no regression to the expression/function-builder contexts that use `JIPipeDesktopHTMLEditor`).

- [ ] **Step 6: VLM-verify**

Render a node/panel that uses an HTML-text parameter (add a synthetic showcase `panel-html-text` with an `HTMLText` field if none in the set). `da_describe_image`: the widget shows a **compact single-row icon toolbar** (B/I/U, color, image, |, view toggle, edit-in-dialog), 24px tall, with a control border; the WYSIWYG / Preview / Source toggle works (verify the Preview renders the HTML read-only and Source shows the raw markup); the parameter editor does not double-border. Report the verdict.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/markup/JIPipeDesktopHTMLEditor.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/library/JIPipeDesktopHTMLTextParameterEditorUI.java
git commit -m "feat(desktop): HTML editor compact toolbar + 3-state view (WYSIWYG/Preview/Source), form-contract normalization (#1339)"
```

---

### Task 10: S8 — render-out audit (after S7, same branch, gates the MR)

**Files:**
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java` (per-editor showcase set)
- Create/modify: synthetic showcase collections as needed (mirror `FontParameterShowcase`)
- (No new tests — this is a render + VLM sweep + fix agreed nits.)

**Interfaces:**
- Consumes: the pass-1 `ScreenshotUiCommand`/`ScreenshotRenderer` harness, `JIPipeDynamicParameterCollection.addParameter(...)`.
- Produces: a per-editor showcase set + a final full-set render in the branch's verification record; any agreed in-scope nits fixed (commits `(#1339)`).

- [ ] **Step 1: Extend the showcase set (per-editor)**

In `ScreenshotUiCommand`, add a per-editor showcase: for every registered parameter type without a dedicated showcase yet, a synthetic one-parameter `JIPipeDynamicParameterCollection` (one `addParameter(key, fieldClass, name, desc)` per editor), rendered at the standard 480px width. Include at minimum (spec §9): boolean, number (field), number (slider), integer list, string, string-or-double, HTML text, enum (small/combo + large/box), dynamic enum, dynamic set, path list, parameter-collection list, matrix, margin (fixed/inner), font, color, optional-boolean, optional-color, expression, runtime-partition ref. Keep the existing node-based showcases too.

- [ ] **Step 2: Render the full set + VLM sweep**

Run the harness (full set, no `--only`) → `/tmp/opencode/1339/after`. Run `da_describe_image` per render with a fixed expectation prompt: *border present? consistent height family? value left-aligned? options/properties column present? no self-drawn name? obvious clipping/overlap/error text?* Compile a findings list.

- [ ] **Step 3: Triage with the user**

Present the findings list to the user; **with the user**, decide which are in-scope nits (fix now, this branch) vs. follow-up issues (new work items). Record the decision.

- [ ] **Step 4: Fix agreed nits + re-render**

Fix the agreed in-scope nits (each a `(#1339)` commit), re-render the full set, and confirm each nit is resolved. Include the final full-set render in the branch's verification record (note the output dir + the VLM verdicts).

- [ ] **Step 5: Gate the MR**

The branch is "done" when S1–S7 are committed + verified and S8's in-scope findings are fixed. Do **not** open the merge request until the user confirms S8 is complete (standing GitLab flow: push the branch only; no MR until the user says the feature is finished).

- [ ] **Step 6: Commit the harness changes**

```bash
git add jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/ScreenshotUiCommand.java
git commit -m "test(launcher): per-editor screenshot showcase for the S8 render-out audit (#1339)"
```
(Plus separate `(#1339)` commits for each nit fixed in Step 4.)

---

## Execution State — Handoff 2026-10-26 (work moved to another machine)

Subagent-driven execution (SDD). The per-task ledger lives in the gitignored `.superpowers/sdd/2026-10-25-parameter-ux-pass2/progress.md` (machine-local); this section is the durable record.

**DONE (committed, task-reviewed, VLM-verified):**
- **Task 1 (S1)** — `02d30ea`. Switch ON-track → primary. Review clean. VLM PASS (blue track, white knob; no regression).
- **Task 2 (S2a)** — `b24eadb`. `createHeaderControls()` extension point + list Add/count/⋯ on the name row (additive; footer still present in this intermediate state). Review clean. VLM PASS. Deviations accepted: base method is `public` (cross-package call site); header ⋯ menu uses the full pass-1 item set (ruling).
- **Task 3 (S2b)** — `f1a3aa5` + fix `3a2527a`. Shared base `JIPipeListParameterEditorUI`; inline pagination (`+k more`/`Show less`, 5/page, `withScrollBar` override); vertical grip + per-row menu (Delete/Move up/Move down, go-up/go-down icons); hover-× removed; footer removed; selection machinery excised from inline (modal-local in Task 4). `ListPaginationModelTest` 4/4 TDD; `ListReorderMoveTest` 6/6 untouched; full core suite green. Fix round 1 (Important: drag index resolution now against the caller's row list — `buildListRow(index, RowGripBehavior, ListRowCallbacks, List<EntryComponents> rows)`) re-reviewed clean. VLM PASS via controller-added `panel-list-populated`/`panel-list-empty` harness showcases (8 items → 5 rows + "+3 more"; empty → hint only).
- **Task 4 (S2c)** — `3e09f77`. `JIPipeDesktopListEditorDialog` (same package as the base) — Close-only (Close button + X + Esc, NO OK/Cancel), search box (hide-not-remove, selection preserved), 5-button toolbar (Select all/Invert/Clear selection/Remove selected/Clear list), all-rows body with modal-local grip selection (pass-1 range/toggle re-implementation), descending-order removal, live edits on the same `JIPipeListParameter`. Edit button wired into the base `createHeaderControls()` (text-only — no `edit-list.png` icon exists). Harness showcases `dialog-list-editor`/`dialog-list-collection` added. Load-bearing base fix included (reviewer-verified): row-menu Move up/down previously mutated without `setParameter` (inline never reloaded) → now routes through `moveEntryTo`. Review clean. VLM PASS (both dialogs: search, toolbar, grip rows, single Close; collection rows are nested mini-forms).

**IN FLIGHT:**
- **Task 5 (S3)** — `8c0c29c` + fix `ed8602a`. `PopupKeyModel` (headless, package-private, in `JIPipeDesktopRichSelectionBox`) implements the full spec §4 key matrix (24 tests, TDD red→green, full core suite 608 green); value left-aligned (`setHorizontalAlignment(LEADING)`). Review found 1 Important → **fixed in round 1** (`ed8602a`: dropped `searchField.selectAll()` on keyboard focus return — ruling: no silent new interactions in a UX project). **REMAINING: (a) scoped re-review of `8c0c29c..ed8602a` (expect clean — one-line deletion, tests re-run green); (b) VLM-verify left-aligned values on `panel-enum-box`, `panel-roi3d-statistics`, `panel-font`.** Then mark complete.

**NOT STARTED:** Tasks 6–10 (S4 font popout, S5 color dialog, S6 number slider, S7 HTMLText, S8 audit). S8 gates the MR.

**Deferred minors (triage at the final whole-branch review):** T2 `headerCountLabel.setForeground` re-applied per render; T4 boundary Move (index 0/last) still fires `setParameter`+rebuild, empty-selection Remove still writes, dialog `flatButton` duplicates the base's private `applyFlatHeaderButton`; T5 dead `twoPane` ctor param, multi-Enter-in-search fires handler with unchanged selection (idempotent; Enter/Esc asymmetric), missing test for single-Enter-in-search on empty filtered list.

**Rulings made (controller):** drag-index resolution via caller's row list (Task 3 fix); `shouldUseSlider` decision test belongs in the editor package (`NumberSliderStyleTest`), slider math in `NumberSliderMathTest` (Task 8 preflight); `EntryComponents` package-visible + `buildListRow` returns it (Task 3/4 seam); collection `itemDescription` = full `getTextDescriptionOf` (Task 3); row-menu `setParameter` base fix accepted inside Task 4 (Task 4); drop `selectAll()` on search-field focus return (Task 5).

**Environment notes for the resuming machine:**
- Branch `1339-parameter-ux-overhaul` — everything above is pushed. No merge request (GitLab flow: MR only after the user confirms S8).
- Screenshot harness: `/tmp/opencode/1339/` is machine-local. If missing, recreate `run.sh` (classpath = `jipipe-launcher/target/classes` + `mvn -pl jipipe-launcher dependency:build-classpath` output; `--add-opens` for `java.base/java.lang`, `java.desktop/javax.swing`, `java.desktop/sun.font`; `-Djava.awt.headless=false`; main `org.hkijena.jipipe.launcher.JIPipeLauncher`) and run `mvn -q install -DskipTests` first. The plan's Global Constraints carry the canonical command. Before-baselines (`/tmp/opencode/1339/before/`) are NOT pushed — re-render from a `master` worktree if before/after pairs are needed.
- Model tiers that work on the gateway: `asb-hki/vllm/small`, `asb-hki/vllm/frontier#medium`, `asb-hki/vllm/frontier` (bare = top). `frontier#high` / `frontier#xhigh` are REJECTED (variant/effort whitelist mismatch) — do not dispatch with those.
- VLM division of labor: implementers do code + `mvn -q -pl jipipe-core compile` + the task's unit tests + commit + report; the controller runs the harness renders + `da_describe_image` and records verdicts.
- Maven output is German-locale ("Fehler" = error, "Symbol nicht gefunden" = symbol not found). Harness log noise ("Invalid color format…") is pre-existing and harmless.

---

## Execution State — FINAL (2026-10-26) — all tasks done, final review complete

This section **supersedes** the mid-flight handoff above (accurate as of `c8845ef3ef`). Resumed on the new machine, finished Tasks 5–10 inline, ran the final whole-branch review. Full per-task detail + rulings + re-grade reasoning: gitignored ledger `.superpowers/sdd/2026-10-25-parameter-ux-pass2/progress.md`.

**ALL 10 TASKS COMPLETE** (S1–S8), committed, unit-tested, VLM-verified:
- Task 5 (S3) — `8c0c29c` + fix `ed8602a`. Scoped re-review of the fix CLEAN (fresh reviewer); VLM PASS (left-aligned values on `panel-enum-box` "Viridis" + `panel-roi3d-statistics` "Default"/"16 selected"). (`panel-font`'s value is the custom-painted font preview, not a RichSelectionBox — see the S8 finding.)
- Task 6 (S4) — `2fe86ca`. Font popout onto form-panel rows + `getPopoutContent()` seam; size spinner 80px/natural-height; B/I natural-height; `popup-font` showcase. VLM PASS.
- Task 7 (S5) — `6bbf674`. Color dialog `refresh()` re-entrancy fix (TDD `#000000→#000000` RED→GREEN) + pill tabs + palette wrap (no h-scrollbar) + 16 4-bit colors + merged untitled block + 34px right-aligned label column + rail hit-target widening + rich swatch tooltips. VLM PASS (both dialogs).
- Task 8 (S6) — `1498dd9`. `NumberParameterSettings.Style` enum + `JIPipeDesktopNumberSlider` (static math + custom control + inline edit) + editor `shouldUseSlider` branch + one-time unbounded warning. Math 5/5 + decision 2/7 TDD. VLM PASS (two-value ratio proved the thumb tracks the value). **No example adoption** (no node has a bounded annotation).
- Task 9 (S7) — `c06b349`. HTML editor compact 24px icon toolbar + WYSIWYG/Preview/Source 3-state view + density toggle (replaces the mode popup) + control border on the widget + parameter editor stacked + redundant border dropped; legacy `Mode` API stable. VLM PASS (all three views).
- Task 10 (S8) — `6cc49d4` + `bc9c800`. Per-editor showcase set (17 editors) + full 35-render VLM sweep. **One finding, user-triaged and fixed in-branch (TDD):** font preview rendered blank (white default font color on the light panel) → theme-derived checkerboard preview background (`FontPreviewCheckerTest` pins the light+dark contrast contract).

**Final whole-branch review** (fresh reviewer, `asb-hki/vllm/frontier`, `29f80d5b20..bc9c80047b`): **0 Critical.** Two Important findings **re-graded to Minor by effect** (the re-grade is the gate, not the label): (1) slider `setValue()` clamps a stored *out-of-range* value on display, but fires no listeners and writes nothing — only an already-invalid value (typed beyond bounds via the FIELD path) is shown clamped/normalized on the next explicit slider use; in-range unaffected; (2) modal `rebuildRows()` could drop uncommitted caret text of a sibling row, but this codebase's editors commit via live `DocumentListener` so a sibling's text is already in the shared parameter before any rebuild. Both **deferred**. 8 Minor items deferred to the user; 4 reviewer "declined-to-judge" lines accepted as-is. **Fix pass: empty** (no standing Critical/Important).

**Full core suite: 619 tests, 0 failures, 1 skipped (headless AWT), green.**

**Branch state:** tip `bc9c80047b` on `1339-parameter-ux-overhaul`, clean tree. **NOT yet pushed; NO merge request** — standing GitLab flow: push the branch only, open the MR only after the user confirms the feature is finished.

**User still to eyeball (project policy — no Swing-structure tests; user is the final UI judge):** the new font-preview checkerboard (esp. white sample text on the light-theme checker), the number slider's proportions, and the HTML editor's compact toolbar + view toggle, in the running app.
