package org.hkijena.jipipe.extensions.parameters.ranges;

import java.awt.*;

public interface PaintGenerator {
    Paint generate(int x, int y, int width, int height);
}
