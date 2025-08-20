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

package org.hkijena.jipipe.desktop.commons.components.window;

import org.hkijena.jipipe.JIPipe;

import javax.swing.*;

public class JIPipeDesktopAlwaysOnTopToggle extends JToggleButton {
    public JIPipeDesktopAlwaysOnTopToggle(JWindow window) {
        super(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
        setToolTipText("Make window always on top");
        setSelected(window.isAlwaysOnTop());
        addActionListener(e -> window.setAlwaysOnTop(isSelected()));
    }

    public JIPipeDesktopAlwaysOnTopToggle(JFrame window) {
        super(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
        setToolTipText("Make window always on top");
        setSelected(window.isAlwaysOnTop());
        addActionListener(e -> window.setAlwaysOnTop(isSelected()));
    }

    public JIPipeDesktopAlwaysOnTopToggle(JDialog window) {
        super(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
        setToolTipText("Make window always on top");
        setSelected(window.isAlwaysOnTop());
        addActionListener(e -> window.setAlwaysOnTop(isSelected()));
    }
}
