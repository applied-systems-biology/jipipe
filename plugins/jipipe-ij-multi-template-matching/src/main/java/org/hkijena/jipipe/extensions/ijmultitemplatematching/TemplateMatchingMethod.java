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

package org.hkijena.jipipe.extensions.ijmultitemplatematching;

public enum TemplateMatchingMethod {
    SquareDifference("Square difference", 0),
    NormalizedSquareDifference("Normalised Square Difference", 1),
    CrossCorrelation("Cross-Correlation", 2),
    NormalizedCrossCorrelation("Normalised cross-correlation", 3),
    ZeroMeanCrossCorrelation("0-mean cross-correlation", 4),
    NormalizedZeroMeanCrossCorrelation("Normalised 0-mean cross-correlation", 5);

    private final String label;
    private final int index;

    TemplateMatchingMethod(String label, int index) {
        this.label = label;
        this.index = index;
    }

    @Override
    public String toString() {
        return label;
    }

    public int getIndex() {
        return index;
    }
}
