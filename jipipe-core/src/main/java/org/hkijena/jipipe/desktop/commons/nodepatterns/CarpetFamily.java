/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.nodepatterns;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/** Sierpinski carpet (plane-filling fractal). */
public final class CarpetFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        g.setStroke(new BasicStroke(1.1f));
        carpet(g, 0, 0, w, h, 3);
    }

    private static void carpet(Graphics2D g, double x, double y, double w, double h, int depth) {
        if (depth == 0) {
            g.draw(new Rectangle2D.Double(x, y, w, h));
            return;
        }
        double cw = w / 3, ch = h / 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) continue;
                carpet(g, x + i * cw, y + j * ch, cw, ch, depth - 1);
            }
    }
}
