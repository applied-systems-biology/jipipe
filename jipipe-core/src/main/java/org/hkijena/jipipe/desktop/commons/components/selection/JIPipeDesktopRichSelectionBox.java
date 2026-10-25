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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
 * the two-pane "Input | Output" layout), and — planned — parameter editors
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

        /** The row tooltip, or null. */
        public String getTooltip() {
            return tooltip;
        }

        /** The lower-cased display text, for case-insensitive substring search. */
        String getSearchText() {
            return searchText;
        }
    }

    /** Marker prefix of a group-header row value (single-pane list mode). */
    private static final String GROUP_HEADER_MARKER = "__group__:";

    private static final int POPUP_WIDTH = 300;
    private static final int POPUP_HEIGHT = 380;
    /** Fast wheel scrolling (Swing's default unit increment is glacial). */
    private static final int SCROLL_UNIT_INCREMENT = 48;

    private final String baseLabel;
    /** The compact label of a non-empty selection (null = legacy label, see the constructor). */
    private final String summaryLabel;
    private final Font baseFont;
    private final boolean multiSelect;
    private final boolean twoPaneGroups;
    private final Supplier<List<Option<?>>> optionsSupplier;
    private final Consumer<List<Option<?>>> selectionHandler;

    private final JPopupMenu popup = new JPopupMenu();
    private final JTextField searchField = new JTextField();
    /** Resets the selection to empty ("show all"); sits right of the search bar. */
    private final JButton clearButton = new JButton("Clear");
    /** (Optional) selects all current options; sits left of the Clear button. Enabled only when {@link #setHasSelectAll(boolean)}. */
    private final JButton selectAllButton = new JButton("Select all");
    /** Whether the popup header shows the "Select all" button (see {@link #setHasSelectAll(boolean)}). */
    private boolean hasSelectAll = false;
    /**
     * When true, the button shows a compact count ("n selected") instead of the
     * selected option texts / summary label. Used by editors that already show
     * the parameter name in a separate label column, so the button must not
     * repeat it (#1339).
     */
    private boolean countOnly = false;
    /** One per distinct group (two-pane mode) or a single list (list mode). */
    private final List<JList<Option<?>>> paneLists = new ArrayList<>();
    private final List<DefaultListModel<Option<?>>> paneModels = new ArrayList<>();
    private final Set<Object> selectedValues = new LinkedHashSet<>();
    private final Map<Object, Option<?>> optionsByValue = new LinkedHashMap<>();
    /** The options of the last (or current) popup open. */
    private List<Option<?>> currentOptions = List.of();

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
     *                          "All data")
     * @param summaryLabel     the compact label used while options are
     *                          selected, rendered as
     *                          {@code "<summaryLabel> (<count>)"} (e.g.
     *                          "Data" → "Data (3)"), so the button width
     *                          stays bounded no matter how many options are
     *                          picked; {@code null} keeps the legacy label
     *                          (the first three selected option texts)
     * @param multiSelect      whether more than one option can be selected
     * @param twoPaneGroups    render the option groups as side-by-side panes
     *                          (e.g. "Input" | "Output") instead of a single
     *                          list with group headers
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
        addActionListener(e -> showPopup());

        // The popup's "Clear" button (resets the selection to empty =
        // "show all"). Wired here — not in showPopup, which runs on every
        // open and would accumulate listeners on the reused button — so the
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
        // shown when {@link #setHasSelectAll(boolean)} is true. Wired here —
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

    /** @return the currently selected values (unmodifiable) */
    public Set<Object> getSelectedValues() {
        return Collections.unmodifiableSet(selectedValues);
    }

    /**
     * Evaluates the options supplier and updates the button's label and icon
     * to reflect the current selection, <em>without</em> showing the popup.
     * Call this after construction (and whenever the underlying value changes)
     * so the button shows the selected option's display text and icon instead
     * of the raw value — the option index is only built on the first popup
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

    // ------------------------------------------------------------------
    // Popup
    // ------------------------------------------------------------------

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
        // listener is wired up exactly once in the constructor — adding it
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
        popup.show(this, 0, getHeight());
        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
    }

    private static JScrollPane makeScroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        return scroll;
    }

    private JList<Option<?>> createPaneList() {
        DefaultListModel<Option<?>> model = new DefaultListModel<>();
        JList<Option<?>> list = new JList<>(model);
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
                // dispatch makes the list paint once, in its final state —
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
                    popup.setVisible(false);
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
            // Compact: "Data (3)" / "Tags (7)" — the button width stays
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
}
