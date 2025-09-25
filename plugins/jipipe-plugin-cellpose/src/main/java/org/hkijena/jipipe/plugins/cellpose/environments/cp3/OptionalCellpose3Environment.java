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

package org.hkijena.jipipe.plugins.cellpose.environments.cp3;

import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;

public class OptionalCellpose3Environment extends JIPipeOptionalParameter<Cellpose3Environment> {
    public OptionalCellpose3Environment() {
        super(Cellpose3Environment.class);
    }

    public OptionalCellpose3Environment(OptionalCellpose3Environment other) {
        super(other);
    }
}
