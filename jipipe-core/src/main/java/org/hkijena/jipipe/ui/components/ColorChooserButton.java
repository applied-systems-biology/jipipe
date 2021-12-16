/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.ui.components.icons.ColorIcon;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.ColorUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

public class ColorChooserButton extends JButton implements ActionListener {
    private final EventBus eventBus = new EventBus();
    private ColorIcon icon = new SolidColorIcon(16, 16);
    private Color selectedColor = Color.RED;
    private String selectColorPrompt = "Select color";
    private boolean updateWithHexCode = false;

    public ColorChooserButton() {
        super();
        setIcon(icon);
        addActionListener(this);
    }

    public ColorChooserButton(String text) {
        super(text);
        setIcon(icon);
        addActionListener(this);
    }

    @Override
    public ColorIcon getIcon() {
        return icon;
    }

    public void setIcon(ColorIcon icon) {
        this.icon = icon;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Color value = JColorChooser.showDialog(this, selectColorPrompt, selectedColor);
        if (value != null) {
            setSelectedColor(value);
        }
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(Color selectedColor) {
        if (!Objects.equals(selectedColor, this.selectedColor)) {
            this.selectedColor = selectedColor;
            icon.setFillColor(selectedColor);
            repaint();
            if (updateWithHexCode) {
                setText(ColorUtils.colorToHexString(selectedColor));
            }
            eventBus.post(new ColorChosenEvent(this, selectedColor));
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public String getSelectColorPrompt() {
        return selectColorPrompt;
    }

    public void setSelectColorPrompt(String selectColorPrompt) {
        this.selectColorPrompt = selectColorPrompt;
    }

    public boolean isUpdateWithHexCode() {
        return updateWithHexCode;
    }

    public void setUpdateWithHexCode(boolean updateWithHexCode) {
        this.updateWithHexCode = updateWithHexCode;
    }

    public static class ColorChosenEvent {
        private final ColorChooserButton button;
        private final Color color;

        public ColorChosenEvent(ColorChooserButton button, Color color) {
            this.button = button;
            this.color = color;
        }

        public ColorChooserButton getButton() {
            return button;
        }

        public Color getColor() {
            return color;
        }
    }
}
