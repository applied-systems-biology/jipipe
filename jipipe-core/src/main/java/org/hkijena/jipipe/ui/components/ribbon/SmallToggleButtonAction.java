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

package org.hkijena.jipipe.ui.components.ribbon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class SmallToggleButtonAction extends Ribbon.Action {
    public SmallToggleButtonAction(String label, String tooltip, Icon icon) {
        this(label, tooltip, icon, false, (button) -> {
        });
    }

    public SmallToggleButtonAction(String label, String tooltip, Icon icon, boolean selected, Consumer<JToggleButton> action) {
        super(new JToggleButton(), 1, new Insets(2, 2, 2, 2));

        JToggleButton button = (JToggleButton) getFirstComponent();
        button.setToolTipText(tooltip);
        button.setSelected(selected);
        button.setText(label);
        button.setIcon(icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(Ribbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.accept(button));
    }

    public JToggleButton getButton() {
        return (JToggleButton) getFirstComponent();
    }

    public boolean getState() {
        return getButton().isSelected();
    }

    public void setState(boolean state) {
        getButton().setSelected(state);
    }

    public boolean isSelected() {
        return getButton().isSelected();
    }

    public void setSelected(boolean state) {
        getButton().setSelected(state);
    }

    public void addActionListener(Runnable runnable) {
        getButton().addActionListener(e -> runnable.run());
    }

    public void addActionListener(ActionListener listener) {
        getButton().addActionListener(listener);
    }
}
