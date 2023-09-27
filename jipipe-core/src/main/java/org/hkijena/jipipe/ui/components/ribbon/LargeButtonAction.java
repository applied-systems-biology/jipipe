package org.hkijena.jipipe.ui.components.ribbon;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class LargeButtonAction extends Ribbon.Action {


    public LargeButtonAction(String label, String tooltip, Icon icon, Runnable action) {
        super(new JButton(), Integer.MAX_VALUE, new Insets(2, 6, 2, 6));

        // Create a new button
        JButton button = (JButton) getFirstComponent();
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorder(Ribbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.run());
    }

    public LargeButtonAction(String label, String tooltip, Icon icon, JMenuItem... menuItems) {
        super(new JButton(), Integer.MAX_VALUE, new Insets(2, 6, 2, 6));

        // Create a new button
        JButton button = (JButton) getFirstComponent();
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorder(Ribbon.DEFAULT_BORDER);

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
