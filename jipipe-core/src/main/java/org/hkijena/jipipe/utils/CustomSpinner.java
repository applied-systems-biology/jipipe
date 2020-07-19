package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.extensions.settings.GeneralUISettings;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;

public class CustomSpinner extends JSpinner {
    public CustomSpinner(SpinnerModel model) {
        super(model);
        if(GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel())
            installModernDesign();
    }

    public CustomSpinner() {
        if(GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel())
            installModernDesign();
    }

    private void installModernDesign() {
        setUI(new ModernSpinnerUI());
    }

    private static class ModernSpinnerUI extends BasicSpinnerUI {
        @Override
        protected Component createPreviousButton() {
            JButton button = new JButton(UIUtils.getIconFromResources("triangle-down.png"));
            button.setBackground(Color.WHITE);
            button.setPreferredSize(new Dimension(21,14));
            button.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
            installPreviousButtonListeners(button);
            return button;
        }

        @Override
        protected Component createNextButton() {
            JButton button = new JButton(UIUtils.getIconFromResources("triangle-up.png"));
            button.setBackground(Color.WHITE);
            button.setPreferredSize(new Dimension(21,14));
            button.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
            installNextButtonListeners(button);
            return button;
        }
    }
}
