/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components.selection;

import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox.PopupKeyModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static java.awt.event.KeyEvent.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The keyboard decision matrix of the rich selection box's popup
 * ({@link PopupKeyModel} - a headless, Swing-free state machine, see #1339):
 * Enter/Esc/arrow/Tab behavior in single- vs multi-select and single- vs
 * two-pane mode, including the single-pane grouped header rows, which are
 * non-option rows that arrow navigation must skip and Enter must treat as a
 * no-op. The model is driven purely through {@code onKey}; no popup, focus,
 * or Swing component is involved.
 */
class RichSelectionBoxKeyModelTest {

    private static final int ENTER = VK_ENTER;
    private static final int ESC = VK_ESCAPE;
    private static final int TAB = VK_TAB;
    private static final int UP = VK_UP;
    private static final int DOWN = VK_DOWN;

    private static JIPipeDesktopRichSelectionBox.Option<String> opt(String v) {
        return new JIPipeDesktopRichSelectionBox.Option<>(v, v, null, null);
    }

    /**
     * A non-option group header row of the single-pane list model.
     */
    private static JIPipeDesktopRichSelectionBox.Option<String> hdr(String g) {
        return new JIPipeDesktopRichSelectionBox.Option<>("__group__:" + g, g, null, null);
    }

    /**
     * Two panes: pane 0 = [A, B], pane 1 = [C, D] (two-pane mode, no header rows).
     */
    private static List<List<JIPipeDesktopRichSelectionBox.Option<?>>> panes() {
        return List.of(single("A", "B"), single("C", "D"));
    }

    private static List<JIPipeDesktopRichSelectionBox.Option<?>> single(String... rows) {
        List<JIPipeDesktopRichSelectionBox.Option<?>> pane = new java.util.ArrayList<>();
        for (String row : rows) {
            pane.add(opt(row));
        }
        return pane;
    }

    // ------------------------------------------------------------------
    // Enter
    // ------------------------------------------------------------------

    @Test
    void enterInSearchSingleSelectsFirstFilteredAndCloses() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        assertEquals(PopupKeyModel.KeyAction.SELECT_FIRST_AND_CLOSE, model.onKey(ENTER, false, false).type());
        // confirms the FIRST filtered option (A)
        assertTrue(model.getSelectedValues().contains("A"));
    }

    @Test
    void enterInSearchMultiClosesKeepingToggles() {
        PopupKeyModel model = new PopupKeyModel(
                true, false, List.of(single("A", "B", "C")), List.of(), Set.of("B"));
        assertEquals(PopupKeyModel.KeyAction.CLOSE_KEEP, model.onKey(ENTER, false, false).type());
        // the toggles are kept unchanged (the handler firing on close is
        // covered by multiEnterInSearchFiresHandlerOnClose)
        assertEquals(Set.of("B"), model.getSelectedValues());
    }

    @Test
    void enterInListSingleSelectsRowAndCloses() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        assertEquals(PopupKeyModel.KeyAction.SELECT_ROW_AND_CLOSE, model.onKey(ENTER, false, false).type());
        assertTrue(model.getSelectedValues().contains("A"));
    }

    @Test
    void enterInListMultiTogglesRowStaysOpen() {
        PopupKeyModel model = new PopupKeyModel(
                true, false, List.of(single("A", "B", "C")), List.of(), Set.of("A"));
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 1);
        assertEquals(PopupKeyModel.KeyAction.TOGGLE_ROW, model.onKey(ENTER, false, false).type());
        // row B toggled ON, A kept
        assertEquals(Set.of("A", "B"), model.getSelectedValues());
    }

    @Test
    void enterInListMultiTogglesRowOff() {
        PopupKeyModel model = new PopupKeyModel(
                true, false, List.of(single("A", "B", "C")), List.of(), Set.of("A", "B"));
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 1);
        assertEquals(PopupKeyModel.KeyAction.TOGGLE_ROW, model.onKey(ENTER, false, false).type());
        assertEquals(Set.of("A"), model.getSelectedValues());
    }

    // ------------------------------------------------------------------
    // Arrows
    // ------------------------------------------------------------------

    @Test
    void downFromSearchFocusesListRow0() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        PopupKeyModel.KeyActionResult action = model.onKey(DOWN, false, false);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, action.type());
        assertEquals(0, action.pane());
        assertEquals(0, action.row());
        assertEquals(PopupKeyModel.Focus.LIST, model.focus());
    }

    @Test
    void upFromListRow0ReturnsToSearch() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_SEARCH, model.onKey(UP, false, false).type());
        assertEquals(PopupKeyModel.Focus.SEARCH, model.focus());
    }

    @Test
    void downInListMovesSelection() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        PopupKeyModel.KeyActionResult action = model.onKey(DOWN, false, false);
        assertEquals(PopupKeyModel.KeyAction.MOVE_ROW, action.type());
        assertEquals(1, action.row());
    }

    @Test
    void upInListMovesSelection() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 2);
        PopupKeyModel.KeyActionResult action = model.onKey(UP, false, false);
        assertEquals(PopupKeyModel.KeyAction.MOVE_ROW, action.type());
        assertEquals(1, action.row());
    }

    @Test
    void downOnLastRowIsNoop() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 2);
        assertEquals(PopupKeyModel.KeyAction.NOOP, model.onKey(DOWN, false, false).type());
    }

    @Test
    void downFromSearchWithNoOptionsIsNoop() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single()), List.of(), Set.of());
        assertEquals(PopupKeyModel.KeyAction.NOOP, model.onKey(DOWN, false, false).type());
    }

    // ------------------------------------------------------------------
    // Esc
    // ------------------------------------------------------------------

    @Test
    void escFromSearchClosesWithoutChange() {
        PopupKeyModel model = new PopupKeyModel(
                true, false, List.of(single("A", "B", "C")), List.of(), Set.of("B"));
        assertEquals(PopupKeyModel.KeyAction.CLOSE_KEEP, model.onKey(ESC, false, false).type());
        assertEquals(Set.of("B"), model.getSelectedValues());
        // Esc must NOT fire the selection handler (unlike multi-Enter in search)
        assertFalse(model.fireHandlerOnClose());
    }

    @Test
    void escFromListClosesWithoutChange() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B", "C")), List.of(), Set.of("A"));
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 1);
        assertEquals(PopupKeyModel.KeyAction.CLOSE_KEEP, model.onKey(ESC, false, false).type());
        assertEquals(Set.of("A"), model.getSelectedValues());
        assertFalse(model.fireHandlerOnClose());
    }

    // ------------------------------------------------------------------
    // Tab
    // ------------------------------------------------------------------

    @Test
    void tabCyclesSearchToPanes() {
        PopupKeyModel model = new PopupKeyModel(
                false, true, panes(), List.of(), Set.of());

        // SEARCH -> pane 0
        PopupKeyModel.KeyActionResult a1 = model.onKey(TAB, false, false);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, a1.type());
        assertEquals(0, a1.pane());

        // pane 0 -> pane 1
        PopupKeyModel.KeyActionResult a2 = model.onKey(TAB, false, false);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, a2.type());
        assertEquals(1, a2.pane());

        // pane 1 wraps back to SEARCH
        assertEquals(PopupKeyModel.KeyAction.FOCUS_SEARCH, model.onKey(TAB, false, false).type());
    }

    @Test
    void shiftTabCyclesBackwards() {
        PopupKeyModel model = new PopupKeyModel(
                false, true, panes(), List.of(), Set.of());

        // SEARCH with Shift-Tab wraps to the last pane
        PopupKeyModel.KeyActionResult a1 = model.onKey(TAB, true, false);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, a1.type());
        assertEquals(1, a1.pane());

        // pane 1 with Shift-Tab -> pane 0
        PopupKeyModel.KeyActionResult a2 = model.onKey(TAB, true, false);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, a2.type());
        assertEquals(0, a2.pane());

        // pane 0 with Shift-Tab -> SEARCH
        assertEquals(PopupKeyModel.KeyAction.FOCUS_SEARCH, model.onKey(TAB, true, false).type());
    }

    @Test
    void tabInSinglePaneCyclesSearchToListToSearch() {
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(single("A", "B")), List.of(), Set.of());

        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, model.onKey(TAB, false, false).type());
        assertEquals(PopupKeyModel.KeyAction.FOCUS_SEARCH, model.onKey(TAB, false, false).type());
    }

    // ------------------------------------------------------------------
    // Single-pane grouped header rows
    // ------------------------------------------------------------------

    @Test
    void headerRowsAreSkippedDuringArrowNavigation() {
        // model rows: [header "G1", A, header "G2", B]
        List<JIPipeDesktopRichSelectionBox.Option<?>> rows = new java.util.ArrayList<>();
        rows.add(hdr("G1"));
        rows.add(opt("A"));
        rows.add(hdr("G2"));
        rows.add(opt("B"));
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(rows), List.of(true, false, true, false), Set.of());

        // focus starts on a header row (row 0): DOWN must land on the first
        // OPTION (row 1), not the header
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        PopupKeyModel.KeyActionResult action = model.onKey(DOWN, false, false);
        assertEquals(PopupKeyModel.KeyAction.MOVE_ROW, action.type());
        assertEquals(1, action.row());

        // from option A (row 1): DOWN must skip header row 2 and land on B
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 1);
        PopupKeyModel.KeyActionResult action2 = model.onKey(DOWN, false, false);
        assertEquals(PopupKeyModel.KeyAction.MOVE_ROW, action2.type());
        assertEquals(3, action2.row());
    }

    @Test
    void enterOnHeaderRowIsNoop() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> rows = new java.util.ArrayList<>();
        rows.add(hdr("G1"));
        rows.add(opt("A"));
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(rows), List.of(true, false), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        assertEquals(PopupKeyModel.KeyAction.NOOP, model.onKey(ENTER, false, false).type());
        // the selection is untouched
        assertTrue(model.getSelectedValues().isEmpty());
    }

    @Test
    void upFromHeaderRowReturnsToSearch() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> rows = new java.util.ArrayList<>();
        rows.add(hdr("G1"));
        rows.add(opt("A"));
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(rows), List.of(true, false), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_SEARCH, model.onKey(UP, false, false).type());
    }

    @Test
    void downFromSearchSkipsLeadingHeaderRows() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> rows = new java.util.ArrayList<>();
        rows.add(hdr("G1"));
        rows.add(opt("A"));
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(rows), List.of(true, false), Set.of());
        PopupKeyModel.KeyActionResult action = model.onKey(DOWN, false, false);
        assertEquals(PopupKeyModel.KeyAction.FOCUS_LIST, action.type());
        assertEquals(1, action.row()); // the first option, not the header row 0
    }

    // ------------------------------------------------------------------
    // Row resolution (auto-selected-while-typing)
    // ------------------------------------------------------------------

    @Test
    void singleEnterInSearchConfirmsAutoSelectedRow() {
        // The box highlights a matching row while the user types (the
        // pending confirm row, reported via setFilter); if the user then
        // presses Enter while the search field still has focus, that
        // highlighted row (not the first option) is confirmed.
        List<JIPipeDesktopRichSelectionBox.Option<?>> rows = new java.util.ArrayList<>();
        rows.add(hdr("G1"));
        rows.add(opt("A"));
        rows.add(opt("B"));
        PopupKeyModel model = new PopupKeyModel(
                false, false, List.of(rows), List.of(true, false, false), Set.of());
        model.setFocus(PopupKeyModel.Focus.SEARCH, 0, 2); // auto-selected row B
        assertEquals(PopupKeyModel.KeyAction.SELECT_FIRST_AND_CLOSE, model.onKey(ENTER, false, false).type());
        assertEquals(Set.of("B"), model.getSelectedValues());
    }

    @Test
    void multiEnterInSearchFiresHandlerOnClose() {
        PopupKeyModel model = new PopupKeyModel(
                true, false, List.of(single("A", "B", "C")), List.of(), Set.of("B"));
        assertEquals(PopupKeyModel.KeyAction.CLOSE_KEEP, model.onKey(ENTER, false, false).type());
        // "close and keep the current toggles" fires the handler with the
        // current selection (matching the Clear-button semantics)
        assertTrue(model.fireHandlerOnClose());
    }

    // ------------------------------------------------------------------
    // Two-pane option resolution
    // ------------------------------------------------------------------

    @Test
    void enterInListOfPaneOneSelectsThatPane() {
        PopupKeyModel model = new PopupKeyModel(
                false, true, panes(), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 1, 0);
        assertEquals(PopupKeyModel.KeyAction.SELECT_ROW_AND_CLOSE, model.onKey(ENTER, false, false).type());
        // pane 1 row 0 is option C
        assertTrue(model.getSelectedValues().contains("C"));
    }

    @Test
    void moveRowStaysWithinItsPane() {
        PopupKeyModel model = new PopupKeyModel(
                false, true, panes(), List.of(), Set.of());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 1); // pane 0, last row (B)
        assertEquals(PopupKeyModel.KeyAction.NOOP, model.onKey(DOWN, false, false).type());
        model.setFocus(PopupKeyModel.Focus.LIST, 0, 0);
        assertEquals(PopupKeyModel.KeyAction.MOVE_ROW, model.onKey(DOWN, false, false).type());
        assertEquals(PopupKeyModel.Focus.LIST, model.focus());
        // still pane 0, row 1
        assertEquals(0, model.focusPane());
        assertEquals(1, model.focusRow());
    }
}
