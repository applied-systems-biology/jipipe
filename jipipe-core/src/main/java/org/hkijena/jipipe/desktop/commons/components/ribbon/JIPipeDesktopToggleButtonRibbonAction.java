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
import java.util.List;
import java.util.function.Consumer;

public abstract class JIPipeDesktopToggleButtonRibbonAction extends JIPipeDesktopRibbon.Action {

    public JIPipeDesktopToggleButtonRibbonAction(Component component, int height, Insets insets) {
        super(component, height, insets);
    }

    public JIPipeDesktopToggleButtonRibbonAction(List<Component> components, int height, Insets insets) {
        super(components, height, insets);
    }

    public abstract JToggleButton getButton();

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
