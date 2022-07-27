package org.hkijena.jipipe.ui.components.ribbon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SmallButtonAction extends Ribbon.Action {

    public SmallButtonAction(String label, String tooltip, Icon icon, Runnable action) {
        super(new JButton(), 1, new Insets(2, 2, 2, 2));

        JButton button = (JButton) getComponent();
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setIcon(icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(Ribbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.run());
    }

    public JButton getButton() {
        return (JButton) getComponent();
    }

    public void addActionListener(Runnable runnable) {
        getButton().addActionListener(e -> runnable.run());
    }

    public void addActionListener(ActionListener listener) {
        getButton().addActionListener(listener);
    }
}
