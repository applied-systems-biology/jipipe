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
import java.util.Random;

/**
 * One framework pattern style: draws white ink on a transparent canvas,
 * antialiased, filling the whole frame edge-to-edge.
 * <p>
 * Contract (enforced by NodePatternEngineTest):
 * <ul>
 *   <li>never selects its own COLOR — the white RGB channels are mandatory so
 *       the mask can be tinted at draw time; per-shape alpha is allowed;
 *       the renderer supplies a white pen/brush;</li>
 *   <li>reaches all four borders (overshoot the frame on grid-based geometry
 *       so the motif is clipped by the node body like an infinite texture);</li>
 *   <li>is a pure function of {@code (w, h, rng)} — no Math.random, no clock.</li>
 * </ul>
 * The renderer sets {@code KEY_ANTIALIASING=ON} and {@code KEY_STROKE_CONTROL=PURE}
 * before calling.
 */
public interface NodePatternFamily {
    void paint(Graphics2D g, int w, int h, Random rng);
}
