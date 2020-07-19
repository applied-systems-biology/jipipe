package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.extensions.settings.GeneralUISettings;

import javax.swing.*;
import java.awt.*;

/**
 * A custom scroll pane that can be modernized
 */
public class CustomScrollPane extends JScrollPane {

    public CustomScrollPane(Component view) {
        super(view);
        if(GeneralUISettings.getInstance().isModernizeCrossPlatformLookAndFeel()) {
            getVerticalScrollBar().setUI(new ArrowLessScrollBarUI());
            getHorizontalScrollBar().setUI(new ArrowLessScrollBarUI());
        }
    }

    public CustomScrollPane() {
    }
}
