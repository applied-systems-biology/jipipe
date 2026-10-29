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
 * Flow-field lines (Vanta 'topology' look); lines start at random points inside the frame and wander beyond it.
 */
public final class FlowFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        double a1 = (1 + rng.nextInt(2)) * 2 * Math.PI / w;
        double a2 = (1 + rng.nextInt(2)) * 2 * Math.PI / h;
        double p1 = rng.nextDouble() * Math.PI * 2, p2 = rng.nextDouble() * Math.PI * 2;
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 240; i++) {
            double x = rng.nextDouble() * w, y = rng.nextDouble() * h;
            Path2D p = new Path2D.Double();
            p.moveTo(x, y);
            for (int s = 0; s < 22; s++) {
                double th = Math.sin(x * a1 + p1) + Math.cos(y * a2 + p2);
                double ang = th * 2.0;
                x += Math.cos(ang) * 3.2;
                y += Math.sin(ang) * 3.2;
                p.lineTo(x, y);
            }
            g.draw(p);
        }
    }
}
