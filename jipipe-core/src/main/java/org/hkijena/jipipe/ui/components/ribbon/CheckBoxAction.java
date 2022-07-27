package org.hkijena.jipipe.ui.components.ribbon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class CheckBoxAction extends Ribbon.Action {
    public CheckBoxAction(String label, String tooltip) {
        this(label, tooltip, false, (button) -> {
        });
    }

    public CheckBoxAction(String label, String tooltip, boolean selected, Consumer<JCheckBox> action) {
        super(new JToggleButton(), 1, new Insets(2, 2, 2, 2));

        JCheckBox button = (JCheckBox) getComponent();
        button.setSelected(selected);
        button.setToolTipText(tooltip);
        button.setText(label);
        button.setBorder(Ribbon.DEFAULT_BORDER);
        button.addActionListener(e -> action.accept(button));
    }

    public JCheckBox getCheckBox() {
        return (JCheckBox) getComponent();
    }

    public boolean getState() {
        return getCheckBox().isSelected();
    }

    public boolean isSelected() {
        return getCheckBox().isSelected();
    }

    public void setState(boolean state) {
        getCheckBox().setSelected(state);
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
