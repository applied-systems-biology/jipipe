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

package org.hkijena.jipipe.plugins.omero;

import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

public class OptionalOMEROCredentialsEnvironment extends OptionalParameter<OMEROCredentialsEnvironment> {
    public OptionalOMEROCredentialsEnvironment() {
        super(OMEROCredentialsEnvironment.class);
    }

    public OptionalOMEROCredentialsEnvironment(OptionalParameter<OMEROCredentialsEnvironment> other) {
        super(other);
        this.setContent(new OMEROCredentialsEnvironment(other.getContent()));
    }

    @Override
    public OMEROCredentialsEnvironment getContentOrDefault(OMEROCredentialsEnvironment defaultValue) {
        if (getContent() != null && getContent().generateValidityReport(new UnspecifiedValidationReportContext()).isValid()) {
            return getContent();
        } else {
            return defaultValue;
        }
    }
}
