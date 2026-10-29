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
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Wobbly hexagonal tiling (KDE 'Canopee' / 'Flow' look); the grid overshoots the frame so hexagons clip at all four edges.
 */
public final class HoneycombFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        double r = 13 + rng.nextDouble() * 8; // hex radius
        double hexW = Math.sqrt(3) * r;
        double hexH = 1.5 * r;
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cols = (int) (w / hexW) + 3, rows = (int) (h / hexH) + 3;
        for (int i = -1; i < cols; i++) {
            for (int j = -1; j < rows; j++) {
                double cx = i * hexW + (j % 2 == 0 ? 0 : hexW / 2);
                double cy = j * hexH;
                Path2D p = new Path2D.Double();
                for (int k = 0; k < 6; k++) {
                    double a = Math.toRadians(60 * k - 30 + (rng.nextDouble() - 0.5) * 14);
                    double rr = r * (0.94 + rng.nextDouble() * 0.09);
                    double x = cx + rr * Math.cos(a), y = cy + rr * Math.sin(a);
                    if (k == 0) p.moveTo(x, y);
                    else p.lineTo(x, y);
                }
                p.closePath();
                g.draw(p);
                if (rng.nextDouble() < 0.18) {
                    // some cells carry a small centered dot (KDE 'Flow' accent)
                    g.fill(new Ellipse2D.Double(cx - 2, cy - 2, 4, 4));
                }
            }
        }
    }
}
