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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A <em>rich selection box</em>: a button that opens a popup with a search
 * bar and a fast, icon-bearing, optionally grouped list of options (single-
 * or multi-select). The successor of plain comboboxes and checkbox popups
 * for option sets that are too large or too visual for a {@link JComboBox}.
 * <p>
 * Intended use cases: the node browser's tag filter (hundreds of string
 * options), the input/output data-type filter (grouped, icon-bearing, with
 * the two-pane "Input | Output" layout), and - planned - parameter editors
 * for enum-like and selection-type parameters (e.g.
 * {@code JIPipeDynamicSetParameter}), which currently have clunky UIs.
 * <p>
 * The component is deliberately model-light: the caller supplies the
 * options (via a supplier evaluated every time the popup opens) and a
 * selection-change consumer that is invoked <em>immediately</em> on every
 * toggle (live filtering; there is no explicit "OK" button). The search
 * field filters the options by case-insensitive substring match on the
 * display text.
 * <p>
 * Not thread-safe (Swing UI).
 */
public class JIPipeDesktopRichSelectionBox extends JButton {

    /**
     * Marker prefix of a group-header row value (single-pane list mode).
     */
    private static final String GROUP_HEADER_MARKER = "__group__:";
    private static final int POPUP_WIDTH = 300;
    private static final int POPUP_HEIGHT = 380;
    /**
     * Fast wheel scrolling (Swing's default unit increment is glacial).
     */
    private static final int SCROLL_UNIT_INCREMENT = 48;
    private final String baseLabel;
    /**
     * The compact label of a non-empty selection (null = legacy label, see the constructor).
     */
    private final String summaryLabel;
    private final Font baseFont;
    private final boolean multiSelect;
    private final boolean twoPaneGroups;
    private final Supplier<List<Option<?>>> optionsSupplier;
    private final Consumer<List<Option<?>>> selectionHandler;
    private final JPopupMenu popup = new JPopupMenu();
    private final JTextField searchField = new JTextField();
    /**
     * Resets the selection to empty ("show all"); sits right of the search bar.
     */
    private final JButton clearButton = new JButton("Clear");
    /**
     * (Optional) selects all current options; sits left of the Clear button. Enabled only when {@link #setHasSelectAll(boolean)}.
     */
    private final JButton selectAllButton = new JButton("Select all");
    /**
     * One per distinct group (two-pane mode) or a single list (list mode).
     */
    private final List<JList<Option<?>>> paneLists = new ArrayList<>();
    private final List<DefaultListModel<Option<?>>> paneModels = new ArrayList<>();
    private final Set<Object> selectedValues = new LinkedHashSet<>();
    private final Map<Object, Option<?>> optionsByValue = new LinkedHashMap<>();
    private final DocumentListener searchFilterListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            applyFilter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            applyFilter();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            applyFilter();
        }
    };
    /**
     * Routes the popup's navigation keys to the key model. Shared by the
     * search field and every pane list: {@link #dispatchPopupKey(KeyEvent)}
     * stamps the model's focus from the event's source (ground truth), so the
     * model is never out of sync even after a mouse click moved focus/selection.
     * Only the keys the model owns are handled (it returns NOOP otherwise);
     * {@code e.consume()} stops the component's default so e.g. Tab doesn't
     * also start a traversal. Text input, Ctrl-shortcuts, and Backspace are
     * untouched (the DocumentListener keeps the type-to-filter behavior).
     */
    private final KeyListener popupKeyListener = new KeyListener() {
        @Override
        public void keyPressed(KeyEvent e) {
            dispatchPopupKey(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    };
    /**
     * The keyboard decision matrix of the currently open popup (null when the
     * popup is closed, see {@link #showPopup()}/{@link #buildKeyModel()}).
     */
    private PopupKeyModel keyModel = null;
    /**
     * Whether the popup header shows the "Select all" button (see {@link #setHasSelectAll(boolean)}).
     */
    private boolean hasSelectAll = false;
    /**
     * When true, the button shows a compact count ("n selected") instead of the
     * selected option texts / summary label. Used by editors that already show
     * the parameter name in a separate label column, so the button must not
     * repeat it (#1339).
     */
    private boolean countOnly = false;
    /**
     * The options of the last (or current) popup open.
     */
    private List<Option<?>> currentOptions = List.of();

    /**
     * Creates a rich selection box (single-pane list mode).
     *
     * @param baseLabel        the label shown when nothing is selected
     * @param multiSelect      whether more than one option can be selected
     * @param optionsSupplier  evaluated every time the popup opens (fresh options)
     * @param selectionHandler called with the new selection on every change
     */
    public JIPipeDesktopRichSelectionBox(String baseLabel, boolean multiSelect,
                                         Supplier<List<Option<?>>> optionsSupplier,
                                         Consumer<List<Option<?>>> selectionHandler) {
        this(baseLabel, null, multiSelect, false, optionsSupplier, selectionHandler);
    }

    /**
     * Creates a rich selection box with a compact summary label (see the
     * full constructor for the meaning of the parameters; single-pane list
     * mode).
     */
    public JIPipeDesktopRichSelectionBox(String baseLabel, String summaryLabel, boolean multiSelect,
                                         Supplier<List<Option<?>>> optionsSupplier,
                                         Consumer<List<Option<?>>> selectionHandler) {
        this(baseLabel, summaryLabel, multiSelect, false, optionsSupplier, selectionHandler);
    }

    /**
     * Creates a rich selection box.
     *
     * @param baseLabel        the label shown when nothing is selected (e.g.
     *                         "All data")
     * @param summaryLabel     the compact label used while options are
     *                         selected, rendered as
     *                         {@code "<summaryLabel> (<count>)"} (e.g.
     *                         "Data" → "Data (3)"), so the button width
     *                         stays bounded no matter how many options are
     *                         picked; {@code null} keeps the legacy label
     *                         (the first three selected option texts)
     * @param multiSelect      whether more than one option can be selected
     * @param twoPaneGroups    render the option groups as side-by-side panes
     *                         (e.g. "Input" | "Output") instead of a single
     *                         list with group headers
     * @param optionsSupplier  evaluated every time the popup opens (fresh options)
     * @param selectionHandler called with the new selection on every change
     */
    public JIPipeDesktopRichSelectionBox(String baseLabel, String summaryLabel, boolean multiSelect, boolean twoPaneGroups,
                                         Supplier<List<Option<?>>> optionsSupplier,
                                         Consumer<List<Option<?>>> selectionHandler) {
        super(baseLabel);
        this.baseLabel = baseLabel;
        this.summaryLabel = summaryLabel;
        this.baseFont = getFont();
        this.multiSelect = multiSelect;
        this.twoPaneGroups = twoPaneGroups;
        this.optionsSupplier = optionsSupplier;
        this.selectionHandler = selectionHandler;
        setFocusable(false);
        setToolTipText(baseLabel);
        // Left-align the value text to match the other parameter boxes
        // (a JButton defaults to centering its label).
        setHorizontalAlignment(SwingConstants.LEADING);
        addActionListener(e -> showPopup());

        // The popup's keyboard model (arrow / Enter / Tab / Esc) on the search
        // field. The search field is a REUSED instance (the popup panel is
        // rebuilt on every show), so its listener is wired here - not in
        // showPopup, which would accumulate one listener per open.
        searchField.addKeyListener(popupKeyListener);

        // The popup's "Clear" button (resets the selection to empty =
        // "show all"). Wired here - not in showPopup, which runs on every
        // open and would accumulate listeners on the reused button - so the
        // clear goes through the same path as a user toggle (handler +
        // updateText) and callers persist/refresh exactly as for any other
        // selection change.
        clearButton.setToolTipText("Clear the selection (show all)");
        clearButton.setFocusable(false);
        clearButton.addActionListener(e -> {
            if (selectedValues.isEmpty()) {
                return;
            }
            selectedValues.clear();
            fireSelectionChanged();
            refreshListSelection();
        });

        // The popup's "Select all" button (selects every current option). Only
        // shown when {@link #setHasSelectAll(boolean)} is true. Wired here -
        // not in showPopup, which runs on every open and would accumulate
        // listeners on the reused button.
        selectAllButton.setToolTipText("Select all options");
        selectAllButton.setFocusable(false);
        selectAllButton.addActionListener(e -> {
            for (Option<?> option : currentOptions) {
                if (!isGroupHeader(option)) {
                    selectedValues.add(option.getValue());
                }
            }
            fireSelectionChanged();
            refreshListSelection();
        });
    }

    private static JScrollPane makeScroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        return scroll;
    }

    private static boolean isGroupHeader(Option<?> option) {
        return option.getValue() instanceof String s && s.startsWith(GROUP_HEADER_MARKER);
    }

    private static Option<String> groupHeader(String group) {
        return new Option<>(GROUP_HEADER_MARKER + group, group, null, null);
    }

    private static List<String> distinctGroups(List<Option<?>> options) {
        List<String> groups = new ArrayList<>();
        for (Option<?> option : options) {
            if (option.getGroup() != null && !groups.contains(option.getGroup())) {
                groups.add(option.getGroup());
            }
        }
        return groups;
    }

    /**
     * Stamps the model's focus from the event source and asks it what to do
     * with the key; applies the returned action to the components.
     */
    private void dispatchPopupKey(KeyEvent e) {
        if (keyModel == null) {
            return;
        }
        int code = e.getKeyCode();
        if (code != KeyEvent.VK_DOWN && code != KeyEvent.VK_UP && code != KeyEvent.VK_ENTER
                && code != KeyEvent.VK_ESCAPE && code != KeyEvent.VK_TAB) {
            return;
        }
        Component source = e.getComponent();
        if (source == searchField) {
            // A single-select Enter-in-search confirms the highlighted match
            // row if one is currently selected (refreshListSelection keeps the
            // row of the current selection highlighted while the user filters),
            // else the first filtered option.
            int pane = -1;
            int row = -1;
            for (int p = 0; p < paneLists.size(); p++) {
                int idx = paneLists.get(p).getSelectedIndex();
                if (idx >= 0 && idx < paneModels.get(p).size()
                        && !isGroupHeader(paneModels.get(p).get(idx))) {
                    pane = p;
                    row = idx;
                    break;
                }
            }
            keyModel.setFocus(PopupKeyModel.Focus.SEARCH, pane, row);
        } else {
            int pane = paneLists.indexOf(source);
            if (pane < 0) {
                return;
            }
            JList<?> list = (JList<?>) source;
            // The list's current selection is the cursor row (-1 if it was
            // cleared, e.g. the highlighted row was filtered out); the model
            // treats -1 as "no row", so Enter is a no-op and the arrows re-land
            // on the first/previous option.
            keyModel.setFocus(PopupKeyModel.Focus.LIST, pane, list.getSelectedIndex());
        }
        PopupKeyModel.KeyActionResult result = keyModel.onKey(code, e.isShiftDown());
        if (result.type() != PopupKeyModel.KeyAction.NOOP) {
            e.consume();
            applyPopupKeyAction(result);
        }
    }

    /**
     * Shows a "Select all" button next to the popup's "Clear" button when
     * {@code true} (it selects every current option through the normal
     * selection-change path). Default {@code false}.
     */
    public void setHasSelectAll(boolean hasSelectAll) {
        this.hasSelectAll = hasSelectAll;
    }

    /**
     * Makes the button show a compact count ("n selected" when non-empty,
     * {@code baseLabel} when empty) instead of the selected option texts. Use
     * this when the surrounding form already provides the parameter's name, so
     * the button does not repeat it (#1339).
     */
    public void setCountOnly(boolean countOnly) {
        this.countOnly = countOnly;
        updateText();
    }

    /**
     * @return the currently selected values (unmodifiable)
     */
    public Set<Object> getSelectedValues() {
        return Collections.unmodifiableSet(selectedValues);
    }

    // ------------------------------------------------------------------
    // Popup
    // ------------------------------------------------------------------

    /**
     * Sets the selection programmatically (no change event is fired).
     *
     * @param values the selected values (may be empty)
     */
    public void setSelectedValues(Collection<?> values) {
        selectedValues.clear();
        if (values != null) {
            selectedValues.addAll(values);
        }
        updateText();
    }

    /**
     * Evaluates the options supplier and updates the button's label and icon
     * to reflect the current selection, <em>without</em> showing the popup.
     * Call this after construction (and whenever the underlying value changes)
     * so the button shows the selected option's display text and icon instead
     * of the raw value - the option index is only built on the first popup
     * open otherwise.
     */
    public void refreshDisplay() {
        currentOptions = optionsSupplier.get();
        optionsByValue.clear();
        for (Option<?> option : currentOptions) {
            optionsByValue.put(option.getValue(), option);
        }
        updateText();
    }

    private void showPopup() {
        currentOptions = optionsSupplier.get();
        optionsByValue.clear();
        for (Option<?> option : currentOptions) {
            optionsByValue.put(option.getValue(), option);
        }

        searchField.setText("");
        searchField.getDocument().removeDocumentListener(searchFilterListener);
        searchField.getDocument().addDocumentListener(searchFilterListener);

        // Panes: one per distinct group, side by side (two-pane mode), or a
        // single list with interleaved group headers (list mode)
        paneLists.clear();
        paneModels.clear();
        JPanel panesPanel = new JPanel(new GridLayout(1, 0, 4, 0));
        List<String> groups = distinctGroups(currentOptions);
        if (twoPaneGroups && !groups.isEmpty()) {
            for (String group : groups) {
                JList<Option<?>> list = createPaneList();
                paneLists.add(list);
                paneModels.add((DefaultListModel<Option<?>>) list.getModel());
                JPanel pane = new JPanel(new BorderLayout(0, 2));
                JLabel header = new JLabel(group);
                header.setFont(header.getFont().deriveFont(Font.BOLD));
                pane.add(header, BorderLayout.NORTH);
                pane.add(makeScroll(list), BorderLayout.CENTER);
                panesPanel.add(pane);
            }
        } else {
            JList<Option<?>> list = createPaneList();
            paneLists.add(list);
            paneModels.add((DefaultListModel<Option<?>>) list.getModel());
            panesPanel.add(makeScroll(list));
        }

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        boolean wide = twoPaneGroups && paneLists.size() > 1;
        panel.setPreferredSize(new Dimension(wide ? POPUP_WIDTH * 2 : POPUP_WIDTH, POPUP_HEIGHT));

        // Search bar + a "Clear" button next to it (resets the selection to
        // empty = "show all"). The clear is fired through the same path as a
        // user toggle (handler + updateText), so callers persist/refresh
        // exactly as they do for any other selection change. The button is a
        // REUSED instance (the popup panel is rebuilt on every show), so its
        // listener is wired up exactly once in the constructor - adding it
        // again here would accumulate one listener per popup open.
        JPanel header = new JPanel(new BorderLayout(4, 0));
        header.add(searchField, BorderLayout.CENTER);
        if (hasSelectAll) {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttons.setOpaque(false);
            buttons.add(selectAllButton);
            buttons.add(clearButton);
            header.add(buttons, BorderLayout.EAST);
        } else {
            header.add(clearButton, BorderLayout.EAST);
        }
        panel.add(header, BorderLayout.NORTH);
        panel.add(panesPanel, BorderLayout.CENTER);

        popup.removeAll();
        popup.add(panel);
        rebuildListModels("");
        refreshListSelection();
        buildKeyModel();
        popup.show(this, 0, getHeight());
        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }

    /**
     * (Re)builds the popup key model from the current pane models and selection
     * (called on every popup open, after {@link #rebuildListModels} and
     * {@link #refreshListSelection}). The model starts focused on the search
     * field with no pending match row.
     */
    private void buildKeyModel() {
        KeyModelSnapshot snapshot = snapshotPaneModels();
        keyModel = new PopupKeyModel(multiSelect, twoPaneGroups, snapshot.paneViews(), snapshot.headerRows(), selectedValues);
    }

    /**
     * Re-syncs the key model after the filter changed (the pane models were
     * rebuilt). Keeps the model alive; resets navigation to the search field.
     */
    private void syncKeyModelAfterFilter() {
        if (keyModel == null) {
            return;
        }
        KeyModelSnapshot snapshot = snapshotPaneModels();
        keyModel.modelsRebuilt(snapshot.paneViews(), snapshot.headerRows());
    }

    private KeyModelSnapshot snapshotPaneModels() {
        List<List<Option<?>>> paneViews = new ArrayList<>();
        List<Boolean> headers = new ArrayList<>();
        for (int p = 0; p < paneModels.size(); p++) {
            DefaultListModel<Option<?>> m = paneModels.get(p);
            List<Option<?>> view = new ArrayList<>(m.size());
            for (int i = 0; i < m.size(); i++) {
                view.add(m.get(i));
                if (p == 0) {
                    headers.add(isGroupHeader(m.get(i)));
                }
            }
            paneViews.add(view);
        }
        return new KeyModelSnapshot(paneViews, List.copyOf(headers));
    }

    /**
     * Applies a key-model action to the components. The model has already
     * updated its own selection for confirm/toggle actions; this only moves
     * focus, scrolls, closes, and fires the handler where required.
     */
    private void applyPopupKeyAction(PopupKeyModel.KeyActionResult result) {
        PopupKeyModel.KeyAction type = result.type();
        switch (type) {
            case FOCUS_SEARCH -> focusSearchField();
            case FOCUS_LIST -> focusPaneList(result.pane(), result.row());
            case MOVE_ROW -> {
                JList<Option<?>> list = paneLists.get(result.pane());
                list.setSelectedIndex(result.row());
                list.ensureIndexIsVisible(result.row());
            }
            case SELECT_ROW_AND_CLOSE, SELECT_FIRST_AND_CLOSE -> {
                // The model's selection is authoritative; refresh the lists and
                // fire the handler exactly like a mouse click, then close.
                syncModelSelectionToBox();
                refreshListSelection();
                fireSelectionChanged();
                closePopup();
            }
            case TOGGLE_ROW -> {
                syncModelSelectionToBox();
                refreshListSelection();
                fireSelectionChanged();
            }
            case CLOSE_KEEP -> {
                if (keyModel.fireHandlerOnClose()) {
                    // multi-Enter in search: "close and keep the current
                    // toggles" → fire the handler with the current selection.
                    fireSelectionChanged();
                }
                closePopup();
            }
            default -> {
                // NOOP
            }
        }
    }

    /**
     * Copies the key model's selection values into the box's selectedValues.
     */
    private void syncModelSelectionToBox() {
        if (keyModel == null) {
            return;
        }
        selectedValues.clear();
        selectedValues.addAll(keyModel.getSelectedValues());
    }

    private void focusSearchField() {
        keyModel.focusSearch();
        searchField.requestFocusInWindow();
    }

    private void focusPaneList(int pane, int row) {
        JList<Option<?>> list = paneLists.get(pane);
        keyModel.focusList(pane);
        if (row >= 0) {
            list.setSelectedIndex(row);
            list.ensureIndexIsVisible(row);
            keyModel.setFocusRow(row);
        }
        list.requestFocusInWindow();
    }

    private void closePopup() {
        keyModel = null;
        popup.setVisible(false);
    }

    private JList<Option<?>> createPaneList() {
        DefaultListModel<Option<?>> model = new DefaultListModel<>();
        JList<Option<?>> list = new JList<>(model);
        // The popup's keyboard model handles the list's arrow / Enter / Tab /
        // Esc keys (consuming them so the default JList bindings don't also
        // move the selection); the mouse handling below is unchanged.
        list.addKeyListener(popupKeyListener);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> view, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(view, value, index, isSelected, cellHasFocus);
                if (value instanceof Option<?> option) {
                    label.setText(option.getDisplayText());
                    label.setIcon(option.getIcon());
                    label.setToolTipText(option.getTooltip());
                    if (isGroupHeader(option)) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    }
                }
                return label;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // The list UI's own (earlier-registered) selection handler
                // collapses a multi-selection to the pressed row on mouse
                // press. Restoring the SAVED selection in the same event
                // dispatch makes the list paint once, in its final state -
                // without this, the collapsed ("all off") state was painted
                // for the few frames between press and click (the flicker).
                if (multiSelect) {
                    refreshListSelection();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Same guard for the release (a selection change made here
                // must not linger until the click handler runs).
                if (multiSelect) {
                    refreshListSelection();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int index = list.locationToIndex(e.getPoint());
                DefaultListModel<Option<?>> model = (DefaultListModel<Option<?>>) list.getModel();
                if (index < 0 || index >= model.size()) {
                    return;
                }
                Option<?> option = model.get(index);
                if (isGroupHeader(option)) {
                    return; // a group header row is not selectable
                }
                if (multiSelect) {
                    if (!selectedValues.add(option.getValue())) {
                        selectedValues.remove(option.getValue()); // toggle off
                    }
                    refreshListSelection();
                    fireSelectionChanged();
                } else {
                    selectedValues.clear();
                    selectedValues.add(option.getValue());
                    fireSelectionChanged();
                    closePopup();
                }
            }
        });
        return list;
    }

    private void rebuildListModels(String filter) {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        for (DefaultListModel<Option<?>> model : paneModels) {
            model.clear();
        }
        if (paneModels.size() == 1) {
            // List mode: options in supplier order, with a bold header row
            // before the first option of each group
            boolean hasGroups = currentOptions.stream().anyMatch(o -> o.getGroup() != null);
            String previousGroup = null;
            for (Option<?> option : currentOptions) {
                if (!needle.isEmpty() && !option.getSearchText().contains(needle)) {
                    continue;
                }
                if (hasGroups && option.getGroup() != null && !option.getGroup().equals(previousGroup)) {
                    paneModels.get(0).addElement(groupHeader(option.getGroup()));
                    previousGroup = option.getGroup();
                }
                paneModels.get(0).addElement(option);
            }
        } else {
            // Two-pane mode: pane i carries the options of group i
            List<String> groups = distinctGroups(currentOptions);
            for (Option<?> option : currentOptions) {
                if (!needle.isEmpty() && !option.getSearchText().contains(needle)) {
                    continue;
                }
                int pane = option.getGroup() != null ? groups.indexOf(option.getGroup()) : -1;
                if (pane >= 0 && pane < paneModels.size()) {
                    paneModels.get(pane).addElement(option);
                }
            }
        }
    }

    private void applyFilter() {
        rebuildListModels(searchField.getText());
        refreshListSelection();
        syncKeyModelAfterFilter();
    }

    private void refreshListSelection() {
        for (int p = 0; p < paneLists.size(); p++) {
            JList<Option<?>> list = paneLists.get(p);
            DefaultListModel<Option<?>> model = paneModels.get(p);
            list.clearSelection();
            for (int i = 0; i < model.size(); i++) {
                if (selectedValues.contains(model.get(i).getValue())) {
                    list.addSelectionInterval(i, i);
                }
            }
        }
    }

    private void fireSelectionChanged() {
        // Every box-side selection change (mouse toggle, Clear, Select-all)
        // funnels through here; keep the key model's copy of the selection in
        // lockstep so a later keyboard confirm/toggle starts from the right set.
        if (keyModel != null) {
            keyModel.selectionChanged(selectedValues);
        }
        updateText();
        List<Option<?>> selection = new ArrayList<>();
        for (Option<?> option : currentOptions) {
            if (selectedValues.contains(option.getValue())) {
                selection.add(option);
            }
        }
        selectionHandler.accept(selection);
    }

    private void updateText() {
        if (countOnly) {
            if (selectedValues.isEmpty()) {
                setText(baseLabel);
                setFont(baseFont);
            } else {
                setText(selectedValues.size() + " selected");
                setFont(baseFont.deriveFont(Font.BOLD));
            }
            setIcon(null);
            return;
        }
        if (selectedValues.isEmpty()) {
            setText(baseLabel);
            setFont(baseFont);
            setIcon(null);
            return;
        }
        if (summaryLabel != null) {
            // Compact: "Data (3)" / "Tags (7)" - the button width stays
            // bounded no matter how many options are selected.
            setText(summaryLabel + " (" + selectedValues.size() + ")");
            setFont(baseFont.deriveFont(Font.BOLD));
            setIcon(null);
            return;
        }
        if (selectedValues.size() == 1) {
            // Single select (e.g. an enum): show the selected option's label
            // and icon on the button, not the raw value.
            Option<?> option = optionsByValue.get(selectedValues.iterator().next());
            if (option != null) {
                setText(option.getDisplayText());
                setIcon(option.getIcon());
                setToolTipText(option.getTooltip() != null ? option.getTooltip() : option.getDisplayText());
                setFont(baseFont);
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Object value : selectedValues) {
            if (shown >= 3) {
                sb.append(", …");
                break;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            Option<?> option = optionsByValue.get(value);
            sb.append(option != null ? option.getDisplayText() : String.valueOf(value));
            shown++;
        }
        setText(sb.toString());
        setIcon(null);
        setFont(baseFont.deriveFont(Font.BOLD));
    }

    // ------------------------------------------------------------------
    // Group headers
    // ------------------------------------------------------------------

    /**
     * One selectable option of the box.
     *
     * @param <T> the caller's value type carried by the option
     */
    public static class Option<T> {
        private final T value;
        private final String displayText;
        private final String searchText;
        private final Icon icon;
        private final String group;
        private final String tooltip;

        /**
         * @param value       the caller's value (returned to the change consumer)
         * @param displayText the row text
         * @param icon        the row icon (nullable)
         * @param group       the section group (nullable; rendered as group
         *                    headers / panes)
         */
        public Option(T value, String displayText, Icon icon, String group) {
            this(value, displayText, icon, group, null);
        }

        /**
         * @param value       the caller's value (returned to the change consumer)
         * @param displayText the row text
         * @param icon        the row icon (nullable)
         * @param group       the section group (nullable; rendered as group
         *                    headers / panes)
         * @param tooltip     the row tooltip (nullable; shown on hover)
         */
        public Option(T value, String displayText, Icon icon, String group, String tooltip) {
            this.value = value;
            this.displayText = displayText;
            this.icon = icon;
            this.group = group;
            this.tooltip = tooltip;
            this.searchText = displayText.toLowerCase(Locale.ROOT);
        }

        public T getValue() {
            return value;
        }

        public String getDisplayText() {
            return displayText;
        }

        public Icon getIcon() {
            return icon;
        }

        public String getGroup() {
            return group;
        }

        /**
         * The row tooltip, or null.
         */
        public String getTooltip() {
            return tooltip;
        }

        /**
         * The lower-cased display text, for case-insensitive substring search.
         */
        String getSearchText() {
            return searchText;
        }
    }

    /**
     * The keyboard decision matrix of the popup (see #1339): a headless,
     * Swing-free state machine that the search field and the pane lists
     * delegate to. The box constructs one per popup open and syncs it on
     * every focus move, filter change, and list selection change;
     * {@link #onKey(int, boolean)} returns the {@link KeyAction} to apply.
     * <p>
     * The model knows the pane models as {@link Option} lists - including the
     * non-option group-header rows of the single-pane list mode, which are
     * marked via {@code headerRows} (one boolean per row of pane 0). Header
     * rows are skipped by arrow navigation and Enter on one is a no-op.
     * <p>
     * Two-pane mode has no header rows (each pane IS one group).
     */
    static final class PopupKeyModel {

        private final boolean multiSelect;
        private final Set<Object> selectedValues = new LinkedHashSet<>();
        private List<List<Option<?>>> paneOptions;
        private List<Boolean> headerRows = List.of();
        private Focus focus = Focus.SEARCH;
        private int focusPane = 0;
        private int focusRow = -1;
        /**
         * The option row a filtered match highlights while the user types in
         * the search field (or -1 if none): single-select Enter in search
         * confirms this option instead of the first visible one.
         */
        private int pendingConfirmPane = -1;
        private int pendingConfirmRow = -1;
        /**
         * Whether the closing (multi-Enter in search) fired the selection handler.
         */
        private boolean fireHandlerOnClose = false;
        /**
         * @param multiSelect whether the box allows more than one selected option
         * @param twoPane     whether the popup renders two side-by-side panes
         * @param paneOptions the visible options of each pane, in display order
         *                    (one list for single-pane, two for two-pane);
         *                    single-pane list mode may contain header rows
         * @param headerRows  per-row (pane 0 only) whether the row is a
         *                    non-option group header (empty for two-pane / ungrouped)
         * @param selected    the currently selected values
         */
        public PopupKeyModel(boolean multiSelect, boolean twoPane,
                             List<List<Option<?>>> paneOptions, List<Boolean> headerRows,
                             Set<?> selected) {
            this.multiSelect = multiSelect;
            this.paneOptions = paneOptions;
            this.headerRows = headerRows == null ? List.of() : headerRows;
            this.selectedValues.addAll(selected);
        }

        /**
         * The filter changed and the box rebuilt the pane models: re-point
         * the model's row views at the new contents and reset navigation to
         * the search field.
         */
        public void modelsRebuilt(List<List<Option<?>>> newPaneOptions, List<Boolean> newHeaderRows) {
            this.paneOptions = newPaneOptions;
            this.headerRows = newHeaderRows == null ? List.of() : newHeaderRows;
            focus = Focus.SEARCH;
            focusRow = -1;
            // A rebuild invalidates any previously highlighted match row (the
            // lists may still show a stale selection that no longer belongs to
            // the filtered contents); Enter-in-search falls back to the first
            // visible option until the user moves the cursor again.
            pendingConfirmPane = -1;
            pendingConfirmRow = -1;
        }

        /**
         * Sets the focus region (the box calls this on focus moves, and the
         * headless test calls it to position the cursor):
         * <ul>
         *   <li>{@code LIST} at the given pane + row (the list's selected row);</li>
         *   <li>{@code SEARCH} - the search field has focus. If {@code row >= 0},
         *       that (pane, row) is the match row a single-select Enter should
         *       confirm (the box reports the first filtered match here); if
         *       {@code row < 0}, no specific row is pending and Enter confirms
         *       the first visible option.</li>
         * </ul>
         */
        public void setFocus(Focus region, int pane, int row) {
            focus = region;
            if (region == Focus.LIST) {
                focusPane = pane;
                focusRow = row;
            } else {
                focusPane = 0;
                focusRow = -1;
                if (row >= 0) {
                    pendingConfirmPane = pane;
                    pendingConfirmRow = row;
                } else {
                    pendingConfirmPane = -1;
                    pendingConfirmRow = -1;
                }
            }
        }

        /**
         * The box moved keyboard focus to the search field (no pending match row).
         */
        public void focusSearch() {
            setFocus(Focus.SEARCH, -1, -1);
        }

        // -- state sync (called by the box) ------------------------------

        /**
         * The box moved keyboard focus to the list at the given pane.
         */
        public void focusList(int pane) {
            focus = Focus.LIST;
            focusPane = pane;
        }

        /**
         * The focused list's selection interval is now at this row (of its pane).
         */
        public void setFocusRow(int row) {
            focusRow = row;
        }

        /**
         * The box (re)applied its selection state to the model.
         */
        public void selectionChanged(Set<?> selected) {
            selectedValues.clear();
            selectedValues.addAll(selected);
        }

        public Focus focus() {
            return focus;
        }

        public int focusPane() {
            return focusPane;
        }

        public int focusRow() {
            return focusRow;
        }

        // -- queries -------------------------------------------------------

        /**
         * An unmodifiable view of the current selection values.
         */
        public Set<Object> getSelectedValues() {
            return Collections.unmodifiableSet(selectedValues);
        }

        /**
         * Whether the just-processed close action (multi-Enter in search)
         * must fire the selection handler with the current selection.
         * Esc never sets this.
         */
        public boolean fireHandlerOnClose() {
            return fireHandlerOnClose;
        }

        /**
         * Handles a key event on the search field or one of the pane lists.
         *
         * @param keyCode the key's {@code KeyEvent.VK_*} code
         * @param shift   whether Shift was held (for Tab direction)
         * @return the action to apply (and its target pane/row where applicable)
         */
        public KeyActionResult onKey(int keyCode, boolean shift) {
            return onKey(keyCode, shift, false);
        }

        public KeyActionResult onKey(int keyCode, boolean shift, boolean ctrl) {
            fireHandlerOnClose = false;
            switch (keyCode) {
                case KeyEvent.VK_ESCAPE:
                    return new KeyActionResult(KeyAction.CLOSE_KEEP, -1, -1);
                case KeyEvent.VK_TAB:
                    return tab(shift);
                case KeyEvent.VK_ENTER:
                    return enter();
                case KeyEvent.VK_DOWN:
                    return down();
                case KeyEvent.VK_UP:
                    return up();
                default:
                    return new KeyActionResult(KeyAction.NOOP, -1, -1);
            }
        }

        private KeyActionResult enter() {
            if (focus == Focus.SEARCH) {
                if (multiSelect) {
                    // Close, keeping the current toggles, and fire the
                    // handler with the current selection (Clear-button path).
                    fireHandlerOnClose = true;
                    return new KeyActionResult(KeyAction.CLOSE_KEEP, -1, -1);
                }
                // Confirm the pending match row if one was reported, else the
                // first visible option.
                int[] target = pendingConfirmRow >= 0 && pendingConfirmValue() != null
                        ? new int[]{pendingConfirmPane, pendingConfirmRow}
                        : firstOption();
                if (target == null) {
                    return new KeyActionResult(KeyAction.NOOP, -1, -1);
                }
                selectedValues.clear();
                selectedValues.add(paneOptions.get(target[0]).get(target[1]).getValue());
                return new KeyActionResult(KeyAction.SELECT_FIRST_AND_CLOSE, target[0], target[1]);
            }
            // focus == LIST
            int row = focusRow;
            List<Option<?>> pane = paneOptions.get(focusPane);
            if (row < 0 || row >= pane.size() || isHeader(row)) {
                return new KeyActionResult(KeyAction.NOOP, -1, -1);
            }
            Object value = pane.get(row).getValue();
            if (multiSelect) {
                // Toggle; popup stays open.
                if (!selectedValues.add(value)) {
                    selectedValues.remove(value);
                }
                return new KeyActionResult(KeyAction.TOGGLE_ROW, focusPane, row);
            }
            selectedValues.clear();
            selectedValues.add(value);
            return new KeyActionResult(KeyAction.SELECT_ROW_AND_CLOSE, focusPane, row);
        }

        // -- the decision matrix ------------------------------------------

        private KeyActionResult down() {
            if (focus == Focus.SEARCH) {
                int row = firstOptionRow(0);
                if (row < 0) {
                    return new KeyActionResult(KeyAction.NOOP, -1, -1);
                }
                focus = Focus.LIST;
                focusPane = 0;
                focusRow = row;
                return new KeyActionResult(KeyAction.FOCUS_LIST, 0, row);
            }
            // focus == LIST
            int row = nextOptionRow(focusRow + 1);
            if (row < 0) {
                return new KeyActionResult(KeyAction.NOOP, -1, -1);
            }
            focusRow = row;
            return new KeyActionResult(KeyAction.MOVE_ROW, focusPane, row);
        }

        private KeyActionResult up() {
            if (focus == Focus.LIST) {
                int row = prevOptionRow(focusRow - 1);
                if (row >= 0) {
                    focusRow = row;
                    return new KeyActionResult(KeyAction.MOVE_ROW, focusPane, row);
                }
                // No option above: return to the search field.
                focus = Focus.SEARCH;
                return new KeyActionResult(KeyAction.FOCUS_SEARCH, -1, -1);
            }
            return new KeyActionResult(KeyAction.NOOP, -1, -1);
        }

        private KeyActionResult tab(boolean shift) {
            int panes = paneOptions.size();
            int next;
            if (focus == Focus.SEARCH) {
                next = shift ? panes - 1 : 0;
            } else if (shift) {
                next = focusPane - 1;
            } else {
                next = focusPane + 1;
            }
            // The focus cycle is SEARCH -> pane 0 -> pane 1 -> … -> SEARCH.
            // Stepping forward past the last pane, or backward past the first,
            // wraps to the search field.
            if (next >= panes || next < 0) {
                focus = Focus.SEARCH;
                return new KeyActionResult(KeyAction.FOCUS_SEARCH, -1, -1);
            }
            int row = firstOptionRow(next);
            focus = Focus.LIST;
            focusPane = next;
            focusRow = row;
            return new KeyActionResult(KeyAction.FOCUS_LIST, next, row);
        }

        private boolean isHeader(int row) {
            return !headerRows.isEmpty() && row < headerRows.size() && headerRows.get(row);
        }

        private Object pendingConfirmValue() {
            if (pendingConfirmPane < 0 || pendingConfirmPane >= paneOptions.size()) {
                return null;
            }
            List<Option<?>> pane = paneOptions.get(pendingConfirmPane);
            if (pendingConfirmRow < 0 || pendingConfirmRow >= pane.size() || isHeader(pendingConfirmRow)) {
                return null;
            }
            return pane.get(pendingConfirmRow).getValue();
        }

        /**
         * The first visible option as {pane, row}, or null if there is none.
         */
        private int[] firstOption() {
            for (int p = 0; p < paneOptions.size(); p++) {
                for (int i = 0; i < paneOptions.get(p).size(); i++) {
                    if (!isHeader(i)) {
                        return new int[]{p, i};
                    }
                }
            }
            return null;
        }

        // -- option/row helpers -------------------------------------------

        private int firstOptionRow(int pane) {
            List<Option<?>> p = paneOptions.get(pane);
            for (int i = 0; i < p.size(); i++) {
                if (!isHeader(i)) {
                    return i;
                }
            }
            return -1;
        }

        private int nextOptionRow(int from) {
            List<Option<?>> p = paneOptions.get(focusPane);
            for (int i = Math.max(from, 0); i < p.size(); i++) {
                if (!isHeader(i)) {
                    return i;
                }
            }
            return -1;
        }

        private int prevOptionRow(int from) {
            List<Option<?>> p = paneOptions.get(focusPane);
            for (int i = Math.min(from, p.size() - 1); i >= 0; i--) {
                if (!isHeader(i)) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Where the keyboard focus is inside the popup.
         */
        public enum Focus {SEARCH, LIST}

        /**
         * What the box must do in response to a key.
         */
        public enum KeyAction {
            /**
             * Nothing (e.g. Enter on a header row, arrow at the edge).
             */
            NOOP,
            /**
             * Move keyboard focus to the list at ({@code pane}, {@code row}).
             */
            FOCUS_LIST,
            /**
             * Move keyboard focus to the search field.
             */
            FOCUS_SEARCH,
            /**
             * Move the list's selection within its pane to {@code row} (popup stays open).
             */
            MOVE_ROW,
            /**
             * Single-select: confirm the first visible option (or the highlighted
             * match row if one is pending) and close the popup.
             */
            SELECT_FIRST_AND_CLOSE,
            /**
             * Single-select: confirm the option at the focused row and close.
             */
            SELECT_ROW_AND_CLOSE,
            /**
             * Multi-select: toggle the option at the focused row; popup stays open.
             */
            TOGGLE_ROW,
            /**
             * Close the popup, keeping the selection as-is (Esc; multi-Enter in search).
             */
            CLOSE_KEEP
        }

        /**
         * A {@link KeyAction} with its target pane and row (both -1 when not applicable).
         */
        public record KeyActionResult(KeyAction type, int pane, int row) {
        }
    }

    /**
     * A plain-data snapshot of the current pane models: each pane's visible
     * options in display order, plus per-row (pane 0) header flags - the input
     * the key model needs (it must not hold the live {@link DefaultListModel}s).
     */
    private record KeyModelSnapshot(List<List<Option<?>>> paneViews, List<Boolean> headerRows) {
    }
}
