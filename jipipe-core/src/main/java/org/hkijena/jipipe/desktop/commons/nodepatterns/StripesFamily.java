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

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Wavy vertical bands (filled strips); the two frame-adjacent strips are
 * straight so the left/right borders always carry ink.
 */
public final class StripesFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 5 + rng.nextInt(3);
        double bw = w / (double) n;
        double stripW = bw * 0.55;
        for (int i = 0; i <= n; i++) {
            boolean frame = (i == 0) || (i == n);
            double x0 = i * bw - stripW / 2;
            double amp = frame ? 0 : stripW * (0.35 + rng.nextDouble() * 0.5);
            double k = 1 + rng.nextInt(2);
            double ph = rng.nextDouble() * Math.PI * 2;
            Path2D p = new Path2D.Double();
            for (int y = -2; y <= h + 2; y += 2) {
                double x = x0 + amp * Math.sin(y / h * Math.PI * 2 * k + ph);
                if (y == -2) p.moveTo(x, y); else p.lineTo(x, y);
            }
            for (int y = h + 2; y >= -2; y -= 2) {
                double x = x0 + stripW + amp * Math.sin(y / h * Math.PI * 2 * k + ph + 0.6);
                p.lineTo(x, y);
            }
            p.closePath();
            g.fill(p);
        }
    }
}
