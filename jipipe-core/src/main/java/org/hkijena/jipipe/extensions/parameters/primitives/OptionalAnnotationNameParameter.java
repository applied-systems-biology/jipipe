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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * Parameter used for creating annotations
 */
@StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
public class OptionalAnnotationNameParameter extends OptionalStringParameter implements JIPipeValidatable {
    public OptionalAnnotationNameParameter() {
    }

    public OptionalAnnotationNameParameter(OptionalStringParameter other) {
        super(other);
    }

    public OptionalAnnotationNameParameter(String value, boolean enabled) {
        super(value, enabled);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if(isEnabled()) {
            report.forCategory("Value").checkNonEmpty(getContent(), this);
        }
    }
}
