package org.hkijena.jipipe.ui.components.ribbon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class LargeToggleButtonAction extends Ribbon.Action {

    public LargeToggleButtonAction(String label, String tooltip, Icon icon) {
        this(label, tooltip, icon, false, (button) -> {
        });
    }

    public LargeToggleButtonAction(String label, String tooltip, Icon icon, boolean selected, Consumer<JToggleButton> action) {
        super(new JToggleButton(), Integer.MAX_VALUE, new Insets(2, 6, 2, 6));

        JToggleButton button = (JToggleButton) getFirstComponent();
        button.setToolTipText(tooltip);
        button.setSelected(selected);
        button.setText(label);
        button.setIcon(icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
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
