package org.hkijena.jipipe.extensions.parameters.library.ranges;

import java.awt.Paint;

public interface PaintGenerator {
    Paint generate(int x, int y, int width, int height);
}
