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

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorChooserButton extends JButton implements ActionListener {
    private ColorIcon icon = new ColorIcon(16,16);
    private Color selectedColor = Color.RED;
    private String selectColorPrompt = "Select color";

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
        this.selectedColor = selectedColor;
        icon.setFillColor(selectedColor);
        repaint();
    }

    public String getSelectColorPrompt() {
        return selectColorPrompt;
    }

    public void setSelectColorPrompt(String selectColorPrompt) {
        this.selectColorPrompt = selectColorPrompt;
    }
}
