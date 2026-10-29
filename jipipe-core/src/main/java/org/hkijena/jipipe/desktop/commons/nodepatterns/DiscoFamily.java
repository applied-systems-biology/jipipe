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
 * Confetti polygons on a jittered grid; shapes may clip at the frame, so the pattern continues beyond the tile.
 */
public final class DiscoFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 7;
        double cell = w / n;
        for (int i = -1; i <= n; i++)
            for (int j = -1; j <= n; j++) {
                // shapes may clip at the frame -> the pattern continues beyond the tile
                double cx = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                double cy = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                double r = cell * (0.30 + rng.nextDouble() * 0.30);
                int sides = 3 + rng.nextInt(3);
                double rot = rng.nextDouble() * Math.PI;
                Path2D p = new Path2D.Double();
                for (int s = 0; s < sides; s++) {
                    double a = rot + s * 2 * Math.PI / sides;
                    double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
                    if (s == 0) p.moveTo(x, y);
                    else p.lineTo(x, y);
                }
                p.closePath();
                if (rng.nextDouble() < 0.5) g.fill(p);
                else g.draw(p);
            }
    }
}
