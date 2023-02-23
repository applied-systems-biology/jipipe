package org.hkijena.jipipe.ui.components.ribbon;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SmallButtonAction extends Ribbon.Action {

    public SmallButtonAction(String label, String tooltip, Icon icon, Runnable action) {
        super(new JButton(), 1, new Insets(2, 2, 2, 2));

        JButton button = (JButton) getFirstComponent();
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(Ribbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.run());
    }

    public SmallButtonAction(String label, String tooltip, Icon icon, JMenuItem... menuItems) {
        super(new JButton(), 1, new Insets(2, 2, 2, 2));

        JButton button = (JButton) getFirstComponent();
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(Ribbon.DEFAULT_BORDER);

        if (menuItems.length > 0) {
            JPopupMenu menu = UIUtils.addPopupMenuToComponent(button);
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
