package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.extensions.settings.GeneralUISettings;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class ArrowLessScrollBarUI extends BasicScrollBarUI {
    @Override
    protected JButton createDecreaseButton(int orientation) {
        if (GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel())
            return createZeroButton();
        else
            return super.createDecreaseButton(orientation);
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        if (GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel())
            return createZeroButton();
        else
            return super.createIncreaseButton(orientation);
    }

    private JButton createZeroButton() {
        JButton jbutton = new JButton();
        jbutton.setPreferredSize(new Dimension(0, 0));
        jbutton.setMinimumSize(new Dimension(0, 0));
        jbutton.setMaximumSize(new Dimension(0, 0));
        return jbutton;
    }
}
