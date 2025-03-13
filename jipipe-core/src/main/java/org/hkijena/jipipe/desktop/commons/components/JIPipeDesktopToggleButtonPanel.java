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

package org.hkijena.jipipe.desktop.commons.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JIPipeDesktopToggleButtonPanel<T> extends JPanel {
    private final ButtonGroup buttonGroup;
    private final Map<ButtonModel, T> valueMap; // Maps ButtonModel to model value
    private final Map<T, JToggleButton> buttonMap; // Maps model value to JToggleButton
    private final Border defaultBorder = UIManager.getBorder("ToggleButton.border"); // Default border
    private final Border selectedBorder = UIUtils.createButtonBorder(UIUtils.COLOR_SUCCESS);
    private final Set<ActionListener> actionListeners = new LinkedHashSet<>();

    public JIPipeDesktopToggleButtonPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); // Vertical layout
        buttonGroup = new ButtonGroup();
        valueMap = new HashMap<>();
        buttonMap = new HashMap<>();
    }

    public void addToggleButton(String text, Icon icon, T value) {
        addToggleButton(new JToggleButton(text, icon), value);
    }

    public void addToggleButton(String text, String subText, Icon icon, T value) {
        addToggleButton(new JToggleButton("<html>" + HtmlEscapers.htmlEscaper().escape(text) + "<br/>" +
                "<span style=\"font-size: 10pt;\">" + subText + "</span>" +
                "</html>", icon), value);
    }

    public void addToggleButton(JToggleButton button, T value) {
        buttonGroup.add(button);
        valueMap.put(button.getModel(), value);
        buttonMap.put(value, button);

        // Add action listener to update borders when selection changes
        button.addActionListener(this::actionPerformed);

        add(button);
        revalidate();
        repaint();
    }

    private void updateButtonBorders() {
        for (JToggleButton button : buttonMap.values()) {
            if (button.isSelected()) {
                button.setBorder(selectedBorder);
            } else {
                button.setBorder(defaultBorder);
            }
        }
        repaint();
    }

    /**
     * Gets the model value of the selected button.
     *
     * @return The selected model value or null if no selection.
     */
    public T getSelectedValue() {
        ButtonModel selectedModel = buttonGroup.getSelection();
        return selectedModel != null ? valueMap.get(selectedModel) : null;
    }

    /**
     * Sets the selected toggle button based on the provided model value.
     *
     * @param value The model value to select.
     */
    public void setSelectedValue(T value) {
        JToggleButton button = buttonMap.get(value);
        if (button != null) {
            button.setSelected(true);
        }
        actionPerformed(null);
    }

    public void addActionListener(ActionListener actionListener) {
        actionListeners.add(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionListeners.remove(actionListener);
    }

    private void actionPerformed(ActionEvent e) {
        updateButtonBorders();
        for (ActionListener actionListener : actionListeners) {
            actionListener.actionPerformed(e);
        }
    }
}

