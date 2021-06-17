package org.hkijena.jipipe.extensions.parameters.ranges;

import javax.swing.*;
import java.awt.Paint;

public class DefaultTrackBackground implements PaintGenerator {
    @Override
    public Paint generate(int x, int y, int width, int height) {
        return UIManager.getColor("Panel.background");
    }
}
