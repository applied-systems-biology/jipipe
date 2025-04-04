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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions;

public enum HyperstackDimension {
    Depth("Depth (Z)"),
    Channel("Channel (C)"),
    Frame("Frame (T)");

    private final String label;

    HyperstackDimension(String label) {

        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
