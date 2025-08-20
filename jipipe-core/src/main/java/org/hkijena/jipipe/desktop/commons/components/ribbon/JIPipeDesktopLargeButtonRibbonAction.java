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

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class JIPipeDesktopLargeButtonRibbonAction extends JIPipeDesktopRibbon.Action {


    public JIPipeDesktopLargeButtonRibbonAction(String label, String tooltip, Icon icon, Runnable action) {
        super(new JButton(), Integer.MAX_VALUE, new Insets(2, 6, 2, 6));

        // Create a new button
        JButton button = (JButton) getFirstComponent();
        button.setOpaque(false);
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorder(JIPipeDesktopRibbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.run());
    }

    public JIPipeDesktopLargeButtonRibbonAction(String label, String tooltip, Icon icon, JMenuItem... menuItems) {
        super(new JButton(), Integer.MAX_VALUE, new Insets(2, 6, 2, 6));

        // Create a new button
        JButton button = (JButton) getFirstComponent();
        button.setOpaque(false);
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorder(JIPipeDesktopRibbon.DEFAULT_BORDER);

        if (menuItems.length > 0) {
            JPopupMenu menu = UIUtils.addPopupMenuToButton(button);
            for (JMenuItem item : menuItems) {
                if (item == null) {
                    menu.addSeparator();
                } else {
                    menu.add(item);
                }
            }
        }
    }

    public JButton getButton() {
        return (JButton) getFirstComponent();
    }

    public void addActionListener(Runnable runnable) {
        getButton().addActionListener(e -> runnable.run());
    }

    public void addActionListener(ActionListener listener) {
        getButton().addActionListener(listener);
    }
}
