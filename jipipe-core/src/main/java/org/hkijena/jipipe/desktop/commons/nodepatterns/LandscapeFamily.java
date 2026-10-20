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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Stacked wavy strata (KDE 'Nuvole'-like bands); each band is the region
 * between two consecutive curves, so no alpha stacking; the top band starts
 * at y=0 and the bottom at y=h.
 */
public final class LandscapeFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int layers = 5 + rng.nextInt(2);
        double[] amp = new double[layers];
        double[] k = new double[layers];
        double[] ph = new double[layers];
        double[] yb = new double[layers];
        for (int L = 0; L < layers; L++) {
            yb[L] = -h * 0.04 + h * (L + 0.15) / layers;
            amp[L] = h * (0.025 + rng.nextDouble() * 0.045);
            k[L] = 1 + rng.nextInt(2);
            ph[L] = rng.nextDouble() * Math.PI * 2;
        }
        // top cap: from the frame top down to curve 0 (dark band)
        {
            Path2D p = new Path2D.Double();
            p.moveTo(0, -2);
            p.lineTo(w, -2);
            for (int x = w; x >= 0; x -= 4) p.lineTo(x, curveY(x, yb[0], amp[0], k[0], ph[0], w));
            p.closePath();
            g.setColor(new Color(255, 255, 255, 52));
            g.fill(p);
            g.setColor(Color.WHITE);
        }
        for (int L = 0; L < layers; L++) {
            // band between curve L and curve L+1 (or the bottom edge)
            Path2D p = new Path2D.Double();
            p.moveTo(0, curveY(0, yb[L], amp[L], k[L], ph[L], w));
            for (int x = 0; x <= w; x += 4) p.lineTo(x, curveY(x, yb[L], amp[L], k[L], ph[L], w));
            if (L + 1 < layers) {
                for (int x = w; x >= 0; x -= 4) p.lineTo(x, curveY(x, yb[L + 1], amp[L + 1], k[L + 1], ph[L + 1], w));
            } else {
                p.lineTo(w, h);
                p.lineTo(0, h);
            }
            p.closePath();
            int alpha = 210 - (layers - 1 - L) * (170 / layers);
            g.setColor(new Color(255, 255, 255, alpha));
            g.fill(p);
            g.setColor(Color.WHITE);
        }
    }

    private static double curveY(double x, double yb, double amp, double kk, double ph, int ww) {
        return yb + amp * Math.sin(x / ww * Math.PI * 2 * kk + ph) + amp * 0.5 * Math.sin(x / ww * Math.PI * 4 + ph * 2);
    }
}
