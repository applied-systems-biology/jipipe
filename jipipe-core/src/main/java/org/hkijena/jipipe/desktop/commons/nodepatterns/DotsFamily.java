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
import java.awt.geom.Ellipse2D;
import java.util.Random;

/** Modulated dot grid (Vanta 'dots' look); centers on the border grid so dots clip at the frame edges and the field continues beyond the tile. */
public final class DotsFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 9;
        double cell = w / n;
        double kx = 1 + rng.nextInt(2), ky = 1 + rng.nextInt(2);
        double ph = rng.nextDouble() * Math.PI * 2;
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++) {
                // centers on the border grid (i*cell) so dots clip at the
                // frame edges -> the field continues beyond the tile
                double cx = i * cell + (rng.nextDouble() - 0.5) * 0.3 * cell;
                double cy = j * cell + (rng.nextDouble() - 0.5) * 0.3 * cell;
                double v = 0.5 + 0.5 * Math.sin(cx * kx * 2 * Math.PI / w + ph) * Math.cos(cy * ky * 2 * Math.PI / h);
                double r = cell * (0.12 + v * v * 0.34);
                g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            }
    }
}
