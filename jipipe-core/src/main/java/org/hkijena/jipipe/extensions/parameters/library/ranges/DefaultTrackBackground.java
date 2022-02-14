package org.hkijena.jipipe.extensions.parameters.library.ranges;

import javax.swing.*;
import java.awt.*;

public class DefaultTrackBackground implements PaintGenerator {
    @Override
    public Paint generate(int x, int y, int width, int height) {
        return UIManager.getColor("Panel.background");
    }
}
