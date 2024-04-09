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

package org.hkijena.jipipe.desktop.commons.components.ribbon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class JIPipeDesktopCheckBoxRibbonAction extends JIPipeDesktopRibbon.Action {
    public JIPipeDesktopCheckBoxRibbonAction(String label, String tooltip) {
        this(label, tooltip, false, (button) -> {
        });
    }

    public JIPipeDesktopCheckBoxRibbonAction(String label, String tooltip, boolean selected, Consumer<JCheckBox> action) {
        super(new JToggleButton(), 1, new Insets(2, 2, 2, 2));

        JCheckBox button = (JCheckBox) getFirstComponent();
        button.setSelected(selected);
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setBorder(JIPipeDesktopRibbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.accept(button));
    }

    public JCheckBox getCheckBox() {
        return (JCheckBox) getFirstComponent();
    }

    public boolean getState() {
        return getCheckBox().isSelected();
    }

    public void setState(boolean state) {
        getCheckBox().setSelected(state);
    }

    public boolean isSelected() {
        return getCheckBox().isSelected();
    }

    public void setSelected(boolean state) {
        getCheckBox().setSelected(state);
    }

    public void addActionListener(Runnable runnable) {
        getCheckBox().addActionListener(e -> runnable.run());
    }

    public void addActionListener(ActionListener listener) {
        getCheckBox().addActionListener(listener);
    }
}
