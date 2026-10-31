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
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Isocurves of a periodic smooth field (marching squares).
 */
public final class WavesFamily implements NodePatternFamily {
    private static double f(double x, double y, int[] fx, int[] fy, double[] amp, double[] ph, int w, int h) {
        double v = 0;
        for (int i = 0; i < fx.length; i++)
            v += amp[i] * Math.sin(2 * Math.PI * fx[i] * x / w + ph[i]) * Math.cos(2 * Math.PI * fy[i] * y / h + ph[i] * 1.7);
        return v;
    }

    private static double[][] crossSegs(int gx, int gy, int step, double v00, double v10, double v11, double v01, double lv) {
        double[][] corners = {{gx, gy}, {gx + step, gy}, {gx + step, gy + step}, {gx, gy + step}};
        double[] vals = {v00, v10, v11, v01};
        List<double[]> pts = new ArrayList<>();
        for (int e = 0; e < 4; e++) {
            double v1 = vals[e], v2 = vals[(e + 1) % 4];
            if ((v1 - lv) * (v2 - lv) <= 0 && v1 != v2) {
                double t = (lv - v1) / (v2 - v1);
                double[] c1 = corners[e], c2 = corners[(e + 1) % 4];
                pts.add(new double[]{c1[0] + t * (c2[0] - c1[0]), c1[1] + t * (c2[1] - c1[1])});
            }
        }
        if (pts.isEmpty() || pts.size() > 4) return null;
        return pts.toArray(new double[0][]);
    }

    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 3;
        int[] fx = new int[n], fyy = new int[n];
        double[] amp = new double[n], ph = new double[n];
        for (int i = 0; i < n; i++) {
            fx[i] = 1 + rng.nextInt(3);
            fyy[i] = 1 + rng.nextInt(3);
            amp[i] = 0.5 + rng.nextDouble() * 0.7;
            ph[i] = rng.nextDouble() * Math.PI * 2;
        }
        int step = 4;
        for (double lv : new double[]{-0.5, 0.0, 0.5}) {
            BasicStroke bs = new BasicStroke(1.6f);
            g.setStroke(bs);
            for (int gy = 0; gy < h - step; gy += step)
                for (int gx = 0; gx < w - step; gx += step) {
                    double v00 = f(gx, gy, fx, fyy, amp, ph, w, h);
                    double v10 = f(gx + step, gy, fx, fyy, amp, ph, w, h);
                    double v11 = f(gx + step, gy + step, fx, fyy, amp, ph, w, h);
                    double v01 = f(gx, gy + step, fx, fyy, amp, ph, w, h);
                    double[][] segs = crossSegs(gx, gy, step, v00, v10, v11, v01, lv);
                    if (segs != null && segs.length == 2) {
                        g.draw(new Line2D.Double(segs[0][0], segs[0][1], segs[1][0], segs[1][1]));
                    } else if (segs != null && segs.length == 4) {
                        g.draw(new Line2D.Double(segs[0][0], segs[0][1], segs[1][0], segs[1][1]));
                        g.draw(new Line2D.Double(segs[2][0], segs[2][1], segs[3][0], segs[3][1]));
                    }
                }
        }
    }
}
