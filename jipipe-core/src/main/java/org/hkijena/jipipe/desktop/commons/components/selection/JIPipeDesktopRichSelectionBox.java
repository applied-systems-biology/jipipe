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

        /**
         * @param value       the caller's value (returned to the change consumer)
         * @param displayText the row text
         * @param icon        the row icon (nullable)
         * @param group       the section group (nullable; rendered as group
         *                    headers / panes)
         */
        public Option(T value, String displayText, Icon icon, String group) {
            this.value = value;
            this.displayText = displayText;
            this.icon = icon;
            this.group = group;
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
    private final Font baseFont;
    private final boolean multiSelect;
    private final boolean twoPaneGroups;
    private final Supplier<List<Option<?>>> optionsSupplier;
    private final Consumer<List<Option<?>>> selectionHandler;

    private final JPopupMenu popup = new JPopupMenu();
    private final JTextField searchField = new JTextField();
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
        this(baseLabel, multiSelect, false, optionsSupplier, selectionHandler);
    }

    /**
     * Creates a rich selection box.
     *
     * @param baseLabel        the label shown when nothing is selected
     * @param multiSelect      whether more than one option can be selected
     * @param twoPaneGroups    render the option groups as side-by-side panes
     *                          (e.g. "Input" | "Output") instead of a single
     *                          list with group headers
     * @param optionsSupplier  evaluated every time the popup opens (fresh options)
     * @param selectionHandler called with the new selection on every change
     */
    public JIPipeDesktopRichSelectionBox(String baseLabel, boolean multiSelect, boolean twoPaneGroups,
                                         Supplier<List<Option<?>>> optionsSupplier,
                                         Consumer<List<Option<?>>> selectionHandler) {
        super(baseLabel);
        this.baseLabel = baseLabel;
        this.baseFont = getFont();
        this.multiSelect = multiSelect;
        this.twoPaneGroups = twoPaneGroups;
        this.optionsSupplier = optionsSupplier;
        this.selectionHandler = selectionHandler;
        setFocusable(false);
        setToolTipText(baseLabel);
        addActionListener(e -> showPopup());
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
        panel.add(searchField, BorderLayout.NORTH);
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
                    if (isGroupHeader(option)) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    }
                }
                return label;
            }
        });
        list.addMouseListener(new MouseAdapter() {
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
        if (selectedValues.isEmpty()) {
            setText(baseLabel);
            setFont(baseFont);
            return;
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
