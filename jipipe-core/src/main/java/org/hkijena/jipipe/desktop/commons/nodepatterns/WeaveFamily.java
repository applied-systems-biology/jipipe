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

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * Over/under bands.
 */
public final class WeaveFamily implements NodePatternFamily {
    private static void fillBandH(Graphics2D g, int w, int h, double y, double bw) {
        g.fill(new RoundRectangle2D.Double(-bw, y, w + 2 * bw, bw, bw * 0.6, bw * 0.6));
    }

    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 4 + rng.nextInt(2);
        double bw = Math.min(w, h) / n * (0.5 + rng.nextDouble() * 0.3);
        double pitch = Math.max(w, h) / n;
        // horizontal bands
        for (int i = 0; i < n + 1; i++) {
            double y = i * pitch - pitch / 2;
            fillBandH(g, w, h, y, bw);
        }
        // vertical bands (over where (i+j) even)
        for (int j = 0; j < n + 1; j++) {
            double x = j * pitch - pitch / 2;
            for (int i = 0; i < n + 1; i++) {
                if ((i + j) % 2 == 0) {
                    double y = i * pitch - pitch / 2;
                    double gap = bw * 0.28;
                    g.fill(new RoundRectangle2D.Double(x, y - pitch / 2 + gap, bw, pitch - 2 * gap, bw * 0.6, bw * 0.6));
                }
            }
        }
    }
}
