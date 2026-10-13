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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A combo-box-like <em>multiselect</em> filter button for <b>large,
 * grouped</b> option sets (the node browser's "Data" filter: input and
 * output data types, dozens of options each). Like
 * {@link MultiSelectFilterButton}, the button shows the facet's base label
 * when nothing is selected and the joined selection summary (bold)
 * otherwise; clicking opens a popup of checkboxes.
 * <p>
 * Two differences for large option sets:
 * <ul>
 * <li>the popup is scrollable (a fixed-size panel inside the popup menu),
 * so hundreds of options do not produce a screen-filling popup;</li>
 * <li>options may be grouped: a {@code "header:"} value is rendered as a
 * non-selectable section header row instead of a checkbox (the caller
 * passes header entries interleaved in the desired order).</li>
 * </ul>
 */
public class GroupedMultiSelectFilterButton extends JButton {

    private static final int POPUP_WIDTH = 260;
    private static final int POPUP_HEIGHT = 320;
    private static final String HEADER_PREFIX = "header:";

    private final String baseLabel;
    private final Font baseFont;
    /** value -> display; a display starting with {@link #HEADER_PREFIX} is a section header (not selectable). */
    private final Map<String, String> valueToDisplay = new LinkedHashMap<>();
    private final Set<String> selected = new LinkedHashSet<>();
    private final JPopupMenu popup = new JPopupMenu();
    private final Consumer<Set<String>> changeHandler;

    /**
     * @param baseLabel     the label shown when nothing is selected (also used
     *                      for the tooltip)
     * @param changeHandler fired with a copy of the currently selected values
     *                      whenever the selection changes
     */
    public GroupedMultiSelectFilterButton(String baseLabel, Consumer<Set<String>> changeHandler) {
        super(baseLabel);
        this.baseLabel = baseLabel;
        this.baseFont = getFont();
        this.changeHandler = changeHandler;
        setFocusable(false);
        setToolTipText("Filter by " + baseLabel.toLowerCase() + " (select one or more)");
        addActionListener(e -> showPopup());
    }

    /**
     * Registers a selectable option.
     *
     * @param value   a stable value key (used in the change-handler set)
     * @param display the checkbox text
     */
    public void addOption(String value, String display) {
        valueToDisplay.put(value, display);
    }

    /**
     * Registers a non-selectable section header, rendered between the
     * options added before and after it.
     *
     * @param display the header text
     */
    public void addSectionHeader(String display) {
        valueToDisplay.put(HEADER_PREFIX + display, HEADER_PREFIX + display);
    }

    /**
     * Sets the selection programmatically and refreshes the label without
     * firing the change handler (used to mirror an externally cleared filter).
     *
     * @param values the selected values (may be empty)
     */
    public void setSelection(Set<String> values) {
        selected.clear();
        if (values != null) {
            selected.addAll(values);
        }
        updateText();
    }

    /** @return a copy of the currently selected values */
    public Set<String> getSelection() {
        return new LinkedHashSet<>(selected);
    }

    private void showPopup() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        for (Map.Entry<String, String> option : valueToDisplay.entrySet()) {
            final String value = option.getKey();
            final String display = option.getValue();
            if (display.startsWith(HEADER_PREFIX)) {
                JLabel header = new JLabel(display.substring(HEADER_PREFIX.length()));
                header.setFont(header.getFont().deriveFont(Font.BOLD));
                panel.add(header);
                continue;
            }
            JCheckBox box = new JCheckBox(display, selected.contains(value));
            box.addActionListener(e -> {
                if (box.isSelected()) {
                    selected.add(value);
                } else {
                    selected.remove(value);
                }
                updateText();
                changeHandler.accept(new LinkedHashSet<>(selected));
            });
            panel.add(box);
        }
        JScrollPane scroll = new JScrollPane(panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(POPUP_WIDTH, POPUP_HEIGHT));
        scroll.setMaximumSize(new Dimension(POPUP_WIDTH, POPUP_HEIGHT));
        popup.removeAll();
        popup.add(scroll);
        popup.show(this, 0, getHeight());
    }

    private void updateText() {
        if (selected.isEmpty()) {
            setText(baseLabel);
            setFont(baseFont);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String value : selected) {
            if (shown >= 3) {
                sb.append(", …");
                break;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String display = valueToDisplay.get(value);
            sb.append(display != null ? display : value);
            shown++;
        }
        setText(sb.toString());
        setFont(baseFont.deriveFont(Font.BOLD));
    }
}
