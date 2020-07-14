/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.roi;

/**
 * An anchor
 */
public enum Anchor {
    TopLeft(Margin.PARAM_LEFT | Margin.PARAM_TOP | Margin.PARAM_WIDTH | Margin.PARAM_HEIGHT),
    TopCenter(Margin.PARAM_LEFT | Margin.PARAM_TOP | Margin.PARAM_RIGHT | Margin.PARAM_HEIGHT),
    TopRight(Margin.PARAM_RIGHT | Margin.PARAM_TOP | Margin.PARAM_WIDTH | Margin.PARAM_HEIGHT),
    BottomLeft(Margin.PARAM_LEFT | Margin.PARAM_BOTTOM | Margin.PARAM_HEIGHT | Margin.PARAM_WIDTH),
    BottomCenter(Margin.PARAM_LEFT | Margin.PARAM_BOTTOM | Margin.PARAM_HEIGHT | Margin.PARAM_RIGHT),
    BottomRight(Margin.PARAM_WIDTH | Margin.PARAM_BOTTOM | Margin.PARAM_HEIGHT | Margin.PARAM_RIGHT),
    CenterLeft(Margin.PARAM_TOP | Margin.PARAM_BOTTOM | Margin.PARAM_WIDTH | Margin.PARAM_LEFT),
    CenterRight(Margin.PARAM_TOP | Margin.PARAM_BOTTOM | Margin.PARAM_WIDTH | Margin.PARAM_RIGHT),
    CenterCenter(Margin.PARAM_LEFT | Margin.PARAM_TOP | Margin.PARAM_RIGHT | Margin.PARAM_BOTTOM);

    private final int relevantParameters;

    Anchor(int relevantParameters) {
        this.relevantParameters = relevantParameters;
    }

    public int getRelevantParameters() {
        return relevantParameters;
    }
}
