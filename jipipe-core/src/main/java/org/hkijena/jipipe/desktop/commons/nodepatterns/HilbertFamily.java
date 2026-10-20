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
import java.awt.geom.Path2D;
import java.util.Random;

/** Space-filling Hilbert curve (correct d2xy). */
public final class HilbertFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int order = 6;
        int n = 1 << order;
        double m = (Math.min(w, h) - 8.0) / n;
        double ox = (w - n * m) / 2, oy = (h - n * m) / 2;
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Double();
        for (int d = 0; d < n * n; d++) {
            int[] xy = d2xy(n, d);
            double x = ox + (xy[0] + 0.5) * m, y = oy + (xy[1] + 0.5) * m;
            if (d == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        g.draw(p);
    }

    private static int[] d2xy(int n, int d) {
        // canonical Wikipedia Hilbert d2xy: rotate steps inside-out (s=1 up)
        int x = 0, y = 0, s, t = d;
        for (s = 1; s < n; s *= 2) {
            int rx = 1 & (t / 2);
            int ry = 1 & (t ^ rx);
            if (ry == 0) {
                if (rx == 1) {
                    x = s - 1 - x;
                    y = s - 1 - y;
                }
                int tmp = x; x = y; y = tmp;
            }
            x += s * rx;
            y += s * ry;
            t /= 4;
        }
        return new int[]{x, y};
    }
}
