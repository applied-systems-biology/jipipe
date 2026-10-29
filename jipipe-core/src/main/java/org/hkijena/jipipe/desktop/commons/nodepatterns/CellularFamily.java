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
 * Organic voronoi — each cell as one smooth wobbly closed membrane
 * (KDE 'Cluster' / Vanta 'cells' look). Border sites just outside the frame
 * make the cells clip at all four edges.
 */
public final class CellularFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int side = 4;
        int n = side * side;
        // interior sites
        double[] sx = new double[n], sy = new double[n];
        for (int i = 0; i < side; i++)
            for (int j = 0; j < side; j++) {
                sx[i * side + j] = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.7) * w / side;
                sy[i * side + j] = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.7) * h / side;
            }
        // border sites (just outside the frame) -> cells clip at all four edges
        int extra = side * 4;
        double[] ex = new double[n + extra], ey = new double[n + extra];
        System.arraycopy(sx, 0, ex, 0, n);
        System.arraycopy(sy, 0, ey, 0, n);
        int c = n;
        for (int i = 0; i < side; i++) {
            double t = (i + 0.5) / side;
            ex[c] = -w * 0.06;
            ey[c] = t * h;
            c++;
            ex[c] = w * 1.08;
            ey[c] = t * h;
            c++; // 1.08 (was 1.06 in the prototype): keeps the right-edge membranes inside the 6px border band at all contract-tested sizes (ruling 2026-17)
            ex[c] = t * w;
            ey[c] = -h * 0.06;
            c++;
            ex[c] = t * w;
            ey[c] = h * 1.06;
            c++;
        }
        int total = n + extra;
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int segs = 48;
        for (int s = 0; s < n; s++) {
            Path2D p = new Path2D.Double();
            for (int k = 0; k <= segs; k++) {
                double a = k / (double) segs * 2 * Math.PI;
                double dxr = Math.cos(a), dyr = Math.sin(a);
                double maxR = Double.POSITIVE_INFINITY;
                for (int t = 0; t < total; t++) {
                    if (t == s) continue;
                    double ox = ex[t] - ex[s], oy = ey[t] - ey[s];
                    double dot = ox * dxr + oy * dyr;
                    if (dot <= 1e-9) continue; // bisector behind or parallel
                    double r = (ox * ox + oy * oy) / (2 * dot);
                    if (r < maxR) maxR = r;
                }
                if (Double.isInfinite(maxR)) maxR = 1e9;
                // organic wobble of the membrane
                double wob = maxR * (0.07 * Math.sin(3 * a + s * 1.7) + 0.05 * Math.cos(5 * a + s * 0.9));
                double bx = ex[s] + (maxR + wob) * dxr;
                double by = ey[s] + (maxR + wob) * dyr;
                if (k == 0) p.moveTo(bx, by);
                else p.lineTo(bx, by);
            }
            p.closePath();
            g.draw(p);
        }
    }
}
