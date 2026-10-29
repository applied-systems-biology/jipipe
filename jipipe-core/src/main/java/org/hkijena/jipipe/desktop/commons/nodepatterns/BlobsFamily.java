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
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Scattered smooth filled blobs (superellipse + noise); centers may overshoot the frame so blobs clip at the borders.
 */
public final class BlobsFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            // centers may overshoot the frame -> blobs clip at the borders
            double cx = (rng.nextDouble() * 1.3 - 0.15) * w;
            double cy = (rng.nextDouble() * 1.3 - 0.15) * h;
            double r = (w / 5) * (0.5 + rng.nextDouble() * 0.8);
            double[] rr = new double[12];
            for (int k = 0; k < 12; k++) rr[k] = r * (0.72 + rng.nextDouble() * 0.5);
            double rot = rng.nextDouble() * Math.PI * 2;
            double[][] pts = new double[12][2];
            for (int k = 0; k < 12; k++) {
                double a = rot + k * 2 * Math.PI / 12;
                pts[k][0] = cx + rr[k] * Math.cos(a);
                pts[k][1] = cy + rr[k] * Math.sin(a);
            }
            Path2D p = new Path2D.Double();
            double[] mx = new double[12], my = new double[12];
            for (int k = 0; k < 12; k++) {
                mx[k] = (pts[k][0] + pts[(k + 1) % 12][0]) / 2;
                my[k] = (pts[k][1] + pts[(k + 1) % 12][1]) / 2;
            }
            p.moveTo(mx[0], my[0]);
            for (int k = 1; k <= 12; k++) {
                int k2 = k % 12;
                p.quadTo(pts[k2][0], pts[k2][1], mx[k2], my[k2]);
            }
            p.closePath();
            g.fill(p);
        }
    }
}
