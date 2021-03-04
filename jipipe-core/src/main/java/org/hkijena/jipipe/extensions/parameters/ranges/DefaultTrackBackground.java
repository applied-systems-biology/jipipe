package org.hkijena.jipipe.extensions.parameters.ranges;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class DefaultTrackBackground implements Supplier<Paint> {
    @Override
    public Paint get() {
        return UIManager.getColor("Panel.background");
    }
}
