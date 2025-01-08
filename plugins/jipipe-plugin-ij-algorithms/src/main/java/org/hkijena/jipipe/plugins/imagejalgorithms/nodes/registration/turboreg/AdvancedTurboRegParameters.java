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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class AdvancedTurboRegParameters extends AbstractJIPipeParameterCollection {
    private int minSize = 12;

    public AdvancedTurboRegParameters() {
    }

    public AdvancedTurboRegParameters(AdvancedTurboRegParameters other) {
        this.minSize = other.minSize;
    }

    @SetJIPipeDocumentation(name = "Minimum image size", description = "Minimal linear dimension of an image in the multi-resolution pyramid.")
    @JIPipeParameter("min-size")
    public int getMinSize() {
        return minSize;
    }

    @JIPipeParameter("min-size")
    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }
}
