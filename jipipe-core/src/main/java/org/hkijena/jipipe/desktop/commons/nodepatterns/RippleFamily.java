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
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Radial fan + concentric rings from an interior point (Vanta 'ripple' / 'trunk'); the full 360° crosses all four borders.
 */
public final class RippleFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        double cx = (0.25 + rng.nextDouble() * 0.5) * w;
        double cy = (0.25 + rng.nextDouble() * 0.5) * h;
        g.setStroke(new BasicStroke(1.0f));
        int lines = 48;
        double len = Math.max(Math.hypot(cx, cy), Math.hypot(w - cx, h - cy)) * 1.6;
        double startDeg = rng.nextDouble() * 360;
        for (int i = 0; i < lines; i++) {
            double a = Math.toRadians(startDeg + i * 360.0 / lines);
            g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * len, cy + Math.sin(a) * len));
        }
        for (double r = Math.min(w, h) * 0.10; r < len; r *= 1.30) {
            Path2D p = new Path2D.Double();
            p.append(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), false);
            g.draw(p);
        }
    }
}
