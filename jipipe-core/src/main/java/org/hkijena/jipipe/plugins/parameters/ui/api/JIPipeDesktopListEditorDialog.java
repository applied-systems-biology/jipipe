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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The list editor modal (#1339 pass 2, S2c) - opened by the list editors'
 * **Edit** button. Close-only ({@code APPLICATION_MODAL}; the Close button,
 * the window X, and Esc all dispose - no OK/Cancel, because every row edit
 * and structural operation writes to the shared parameter live).
 * <p>
 * The dialog hosts the FULL list (all rows, no pagination) and re-implements
 * the pass-1 grip selection over its own modal-local state - the base
 * {@link JIPipeListParameterEditorUI} deliberately holds no row state:
 * <ul>
 * <li><b>Rows</b> - one {@link JIPipeListParameterEditorUI.EntryComponents}
 * per item, built via
 * {@code buildListRow(i, SELECT, callbacks, rows)} so the grip drag-reorder
 * resolves indices against the dialog's own row list.</li>
 * <li><b>Selection</b> - plain click selects only this row, Ctrl toggles,
 * Shift selects the range from the last clicked row; the highlight is the
 * theme's {@code List.selectionBackground} on the row panel (the pass-1
 * style).</li>
 * <li><b>Search</b> - a text filter that HIDES (does not remove) rows whose
 * {@code itemDescription(i)} does not contain the needle,
 * case-insensitively; selection is preserved for hidden rows, and the bulk
 * selection ops act on all rows regardless of visibility.</li>
 * <li><b>Bulk ops</b> - Select all / Invert selection / Clear selection /
 * Remove selected (descending index order) / Clear list, all over the
 * dialog's own state.</li>
 * </ul>
 * Value edits in a modal row need no modal re-render (the row's own editor
 * displays the value), and the owner's inline view refreshes itself - it is a
 * displayed editor subscribed to the parameter-change emitter, and every row
 * write goes through {@code setParameter(parameter, true)}. The dialog
 * re-renders its rows explicitly after its own structural operations
 * (remove selected, clear list) and after base-routed structural changes
 * (row menu's Delete / Move up / Move down, grip drag-reorder) via the
 * {@code setAfterRowOrderChanged} hook.
 * <p>
 * Lives in the same package as the base: {@code buildListRow},
 * {@code EntryComponents} and {@code RowGripBehavior} are package-private.
 */
public final class JIPipeDesktopListEditorDialog extends JDialog {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 520;

    private final JIPipeListParameterEditorUI listEditor;
    /**
     * The dialog's own full-list rows (each view keeps its own row list).
     */
    private final List<JIPipeListParameterEditorUI.EntryComponents> rows = new ArrayList<>();
    /**
     * Modal-local selection (the pass-1 grip selection, re-implemented).
     */
    private final Set<JIPipeListParameterEditorUI.EntryComponents> selected = new HashSet<>();
    private final JIPipeDesktopFormPanel itemList = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();
    /**
     * Last clicked row, for Shift range selection.
     */
    private int lastClickedIndex = -1;

    /**
     * Creates the modal for the given list editor. The parameter is edited
     * in place - there is no copy.
     *
     * @param owner      the owner window (may be null)
     * @param listEditor the list parameter editor whose parameter to edit
     */
    public JIPipeDesktopListEditorDialog(Window owner, JIPipeListParameterEditorUI listEditor) {
        super(owner, listEditor.getParameterAccess().getName() + " - Edit", ModalityType.APPLICATION_MODAL);
        this.listEditor = listEditor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setResizable(true);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JRootPane.WHEN_IN_FOCUSED_WINDOW);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.setOpaque(false);
        content.add(buildToolbar(), BorderLayout.NORTH);

        content.add(itemList, BorderLayout.CENTER);
        content.add(buildCloseRow(), BorderLayout.SOUTH);
        setContentPane(content);

        // The dialog's own rows must reflect structural changes that go
        // through the base (row menu Delete / Move up / Move down, grip
        // drag-reorder): the parameter write reloads the *inline* view, but
        // the dialog keeps its own row list.
        listEditor.setAfterRowOrderChanged(this::rebuildRows);
        rebuildRows();

        // Center on the workbench window (the owner), not the screen (#1339).
        if (owner != null) {
            setLocationRelativeTo(owner);
        }
    }


    /**
     * Clears the post-reorder hook on the (shared) base so that, once the
     * modal is closed, inline reorder / delete operations no longer fire the
     * dead dialog's {@link #rebuildRows}, and disposes the nested row editors
     * (one per item) that the dialog built - {@code JDialog.dispose()} does not
     * dispose child {@link Disposable}s. The inline view reloads itself via
     * the parameter-change emitter, so it needs no hook.
     */
    @Override
    public void dispose() {
        listEditor.setAfterRowOrderChanged(null);
        itemList.clear();
        super.dispose();
    }

    /**
     * Builds all rows for the current list size and re-applies the search
     * filter. Called from the constructor and after every structural
     * operation of the dialog.
     */
    private void rebuildRows() {
        JIPipeListParameter<?> parameter = listEditor.getParameter();
        rows.clear();
        selected.clear();
        lastClickedIndex = -1;
        itemList.clear();
        for (int i = 0; i < parameter.size(); ++i) {
            JPanel row = listEditor.buildListRow(i,
                    JIPipeListParameterEditorUI.RowGripBehavior.SELECT,
                    this::handleSelection,
                    rows).entryPanel;
            // Top-align the row within the vertical BoxLayout (default
            // alignmentY=0.5 centers each row, floating it in the viewport -
            // #1339: rows must pack to the top).
            row.setAlignmentY(0f);
            itemList.addWideToForm(row);
        }
        itemList.addVerticalGlue();
        updateSearchFilter();
    }

    /**
     * The grip's left click (SELECT behavior) - the pass-1 range/toggle
     * selection logic over the dialog's own state.
     */
    private void handleSelection(int entryIndex, boolean shiftPressed, boolean ctrlPressed) {
        JIPipeListParameterEditorUI.EntryComponents row = rows.get(entryIndex);
        if (ctrlPressed || (shiftPressed && lastClickedIndex < 0)) {
            if (selected.contains(row)) {
                selected.remove(row);
            } else {
                selected.add(row);
            }
        } else if (shiftPressed) {
            int i0;
            int i1;
            if (lastClickedIndex < entryIndex) {
                i0 = lastClickedIndex;
                i1 = entryIndex;
            } else {
                i0 = entryIndex;
                i1 = lastClickedIndex;
            }
            for (int i = i0; i <= i1; ++i) {
                selected.add(rows.get(i));
            }
        } else {
            selected.clear();
            selected.add(row);
        }
        lastClickedIndex = entryIndex;
        updateSelectionVisualizations();
    }

    /**
     * Paints the row background: the theme's selection color for selected
     * rows, the panel background otherwise (the pass-1 highlight style).
     */
    private void updateSelectionVisualizations() {
        for (JIPipeListParameterEditorUI.EntryComponents row : rows) {
            boolean isSelected = selected.contains(row);
            row.selected = isSelected;
            row.entryPanel.setBackground(isSelected
                    ? UIManager.getColor("List.selectionBackground")
                    : UIManager.getColor("Panel.background"));
        }
        repaint();
    }

    /**
     * The search filter: hides (does not remove) rows whose
     * {@code itemDescription(i)} does not contain the needle,
     * case-insensitively. Selection is preserved - hidden-but-selected rows
     * stay selected.
     */
    private void updateSearchFilter() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        for (int i = 0; i < rows.size(); ++i) {
            boolean visible = needle.isEmpty()
                    || listEditor.itemDescription(i).toLowerCase().contains(needle);
            rows.get(i).entryPanel.setVisible(visible);
        }
    }

    /**
     * Removes the selected items in descending index order (so earlier
     * indices stay valid), then writes the parameter (notifying the owner
     * inline view) and re-renders the dialog's rows.
     */
    private void removeSelectedEntries() {
        JIPipeListParameter<?> parameter = listEditor.getParameter();
        for (int i = rows.size() - 1; i >= 0; i--) {
            if (selected.contains(rows.get(i))) {
                parameter.remove(i);
            }
        }
        listEditor.setParameter(parameter, true);
        rebuildRows();
    }

    /**
     * Top bar: the search field above the icon toolbar (Add + bulk ops).
     */
    private JComponent buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        searchField.getTextField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateSearchFilter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateSearchFilter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateSearchFilter();
            }
        });

        toolbar.add(UIUtils.createButton("Add", JIPipe.RESOURCES.getIcon16("actions/list-add.png"), () -> {
            listEditor.addNewEntry();
            rebuildRows();
        }));
        toolbar.add(UIUtils.createButton("Remove", JIPipe.RESOURCES.getIcon16("actions/edit-delete.png"), this::removeSelectedEntries));

        toolbar.addSeparator();

        toolbar.add(UIUtils.createIconOnlyButton("Select all", JIPipe.RESOURCES.getIcon16("actions/edit-select-all.png"), () -> {
                    selected.clear();
                    selected.addAll(rows);
                    lastClickedIndex = -1;
                    updateSelectionVisualizations();
                }));
        toolbar.add(UIUtils.createIconOnlyButton("Invert selection", JIPipe.RESOURCES.getIcon16("actions/edit-select-invert.png"), () -> {
                    Set<JIPipeListParameterEditorUI.EntryComponents> toSelect = new HashSet<>();
                    for (JIPipeListParameterEditorUI.EntryComponents row : rows) {
                        if (!selected.contains(row)) {
                            toSelect.add(row);
                        }
                    }
                    selected.clear();
                    selected.addAll(toSelect);
                    updateSelectionVisualizations();
                }));
        toolbar.add(UIUtils.createIconOnlyButton("Clear selection", JIPipe.RESOURCES.getIcon16("actions/edit-select-none.png"), () -> {
                    selected.clear();
                    lastClickedIndex = -1;
                    updateSelectionVisualizations();
                }));

        toolbar.add(searchField, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 4, 0), 0, 0));

        toolbar.add(UIUtils.createButton("Clear", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), () -> {
                    listEditor.clearList();
                    rebuildRows();
                }));

        return toolbar;
    }

    /**
     * The single Close button, bottom-right (Close-only - no OK/Cancel).
     */
    private JComponent buildCloseRow() {
        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeRow.setOpaque(false);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        closeRow.add(closeButton);
        return closeRow;
    }
}
