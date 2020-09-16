package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.extensions.settings.GeneralUISettings;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

public class CustomSpinner extends JSpinner {
    public CustomSpinner(SpinnerModel model) {
        super(model);
        if (GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel())
            installModernDesign();
    }

    public CustomSpinner() {
        if (GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel())
            installModernDesign();
    }

    private void installModernDesign() {
        setUI(new ModernSpinnerUI());
    }

    private static class ModernSpinnerUI extends BasicSpinnerUI {
        @Override
        protected Component createPreviousButton() {
            JButton button = new JButton(UIUtils.getIconFromResources("actions/arrow-down.png"));
            button.setBackground(Color.WHITE);
            button.setPreferredSize(new Dimension(21, 14));
            button.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            installPreviousButtonListeners(button);
            return button;
        }

        @Override
        protected Component createNextButton() {
            JButton button = new JButton(UIUtils.getIconFromResources("actions/arrow-up.png"));
            button.setBackground(Color.WHITE);
            button.setPreferredSize(new Dimension(21, 14));
            button.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            installNextButtonListeners(button);
            return button;
        }
    }
}
