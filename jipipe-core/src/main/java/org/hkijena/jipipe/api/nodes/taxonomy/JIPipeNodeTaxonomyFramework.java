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

package org.hkijena.jipipe.api.nodes.taxonomy;

import java.awt.*;

/**
 * A framework in the node taxonomy: the origin of an algorithm node,
 * i.e. the external software or research project it implements
 * (e.g. Cellpose, TrackMate, JFreeChart).
 * <p>
 * Frameworks are flat: unlike {@link JIPipeNodeTaxonomyFunction}s there is
 * no hierarchy. Each framework has a short {@link #getAbbreviation()
 * abbreviation} (at most 4 characters) used for compact node labels and a
 * node pattern style id rendered on canvas nodes.
 */
public interface JIPipeNodeTaxonomyFramework {

    /**
     * The fixed neutral 12-color palette used for the default
     * {@link #getColor()}. Indexed by {@code Math.floorMod(id.hashCode(), 12)}
     * so the color is a pure function of the id.
     */
    Color[] PALETTE = {
            new Color(0x4a86c8), new Color(0xd08a3e), new Color(0x5aa05a), new Color(0x8a63c0),
            new Color(0xb3542e), new Color(0x2e6db3), new Color(0x5a7d2a), new Color(0xc05a8a),
            new Color(0x7a5ac0), new Color(0x2ea0a0), new Color(0xa0a02e), new Color(0x666e7a)
    };

    /**
     * @return the stable, unique identifier of this framework
     */
    String getId();

    /**
     * @return the display name of this framework
     */
    String getName();

    /**
     * @return the short abbreviation of this framework, at most 4 characters
     */
    String getAbbreviation();

    /**
     * @return the node pattern style id rendered on canvas nodes for this
     * framework's nodes, or {@code null} if the framework carries no pattern
     */
    String getPatternStyleId();

    /**
     * The color of this framework: the fixed {@link #PALETTE} indexed by
     * {@code Math.floorMod(getId().hashCode(), 12)}.
     * <p>
     * The result is a pure function of the id, so it is stable across
     * registry instances and JVM runs. Implementations may override this
     * to define an explicit color.
     *
     * @return the framework color
     */
    default Color getColor() {
        return PALETTE[Math.floorMod(getId().hashCode(), PALETTE.length)];
    }
}
