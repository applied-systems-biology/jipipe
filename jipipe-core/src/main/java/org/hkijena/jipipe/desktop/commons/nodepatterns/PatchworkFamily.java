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
import java.awt.geom.Rectangle2D;
import java.util.Random;

/** Recursive subdivision, alternating fills. */
public final class PatchworkFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        subdivide(g, rng, 0, 0, w, h, 4, true);
    }

    private static void subdivide(Graphics2D g, Random rng, double x, double y, double w, double h, int depth, boolean fill) {
        if (depth == 0 || (w < 24 && h < 24)) { // 24 (was 26 in the prototype): at 64px the frame-adjacent leaf column can land on an empty fill-parity, leaving one border ink-free (plane-filling contract deviation, controller ruling)
            if (fill) g.fill(new Rectangle2D.Double(x + 1.5, y + 1.5, Math.max(1, w - 3), Math.max(1, h - 3)));
            return;
        }
        if (w > h * 1.3 || (w > h && rng.nextDouble() < 0.65)) {
            double split = x + w * (0.3 + rng.nextDouble() * 0.4);
            subdivide(g, rng, x, y, split - x, h, depth - 1, !fill);
            subdivide(g, rng, split, y, x + w - split, h, depth - 1, !fill);
        } else {
            double split = y + h * (0.3 + rng.nextDouble() * 0.4);
            subdivide(g, rng, x, y, w, split - y, depth - 1, !fill);
            subdivide(g, rng, x, split, w, y + h - split, depth - 1, !fill);
        }
    }
}
