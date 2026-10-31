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
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Scattered open loop motifs on a jittered grid.
 */
public final class LoopsFamily implements NodePatternFamily {
    private static double toDeg(double rad) {
        return Math.toDegrees(rad);
    }

    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 6;
        double cell = w / n;
        g.setStroke(new BasicStroke((float) Math.max(1.4, cell * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -1; i <= n; i++)
            for (int j = -1; j <= n; j++) {
                double cx = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                double cy = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                double r = cell * (0.28 + rng.nextDouble() * 0.2);
                double a0 = rng.nextDouble() * Math.PI * 2;
                double sweep = Math.PI * (1.2 + rng.nextDouble() * 1.2);
                Path2D p = new Path2D.Double();
                p.append(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, toDeg(a0), toDeg(sweep), Arc2D.OPEN), false);
                g.draw(p);
            }
    }
}
