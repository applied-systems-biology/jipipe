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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.Random;

/**
 * kNN graph over a jittered grid (Vanta 'net' look); the grid overshoots the
 * frame by one row/column on each side so edges cross all four borders.
 */
public final class NetFamily implements NodePatternFamily {
    private static int[] nearest(double x, double y, double[] px, double[] py, int n, int k, double maxD) {
        double[] d = new double[n];
        for (int i = 0; i < n; i++) d[i] = (px[i] - x) * (px[i] - x) + (py[i] - y) * (py[i] - y);
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(d[a], d[b]));
        int[] out = new int[Math.min(k, n)];
        for (int i = 1; i <= Math.min(k, n); i++) {
            if (d[idx[i]] <= maxD * maxD) out[i - 1] = idx[i];
            else break;
        }
        return out;
    }

    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int side = 7;
        int n = (side + 2) * (side + 2);
        double[] px = new double[n], py = new double[n];
        // jittered grid for even coverage; the -1..side overshoot puts points
        // across all four borders so edges cross the frame (plane-filling)
        int c = 0;
        for (int i = -1; i <= side; i++)
            for (int j = -1; j <= side; j++, c++) {
                px[c] = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.8) * w / side;
                py[c] = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.8) * h / side;
            }
        double maxD = w / side * 2.4;
        g.setStroke(new BasicStroke(1.1f));
        for (int i = 0; i < n; i++) {
            int[] near = nearest(px[i], py[i], px, py, n, 3, maxD);
            for (int j : near) {
                if (j > i) g.draw(new Line2D.Double(px[i], py[i], px[j], py[j]));
            }
            g.fill(new Ellipse2D.Double(px[i] - 2.2, py[i] - 2.2, 4.4, 4.4));
        }
    }
}
