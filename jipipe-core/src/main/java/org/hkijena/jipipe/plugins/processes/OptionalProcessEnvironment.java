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

package org.hkijena.jipipe.plugins.processes;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

/**
 * An optional {@link ProcessEnvironment}
 */
public class OptionalProcessEnvironment extends OptionalParameter<ProcessEnvironment> {
    public OptionalProcessEnvironment() {
        super(ProcessEnvironment.class);
        setContent(new ProcessEnvironment());
    }

    public OptionalProcessEnvironment(ProcessEnvironment environment) {
        super(ProcessEnvironment.class);
        setEnabled(true);
        setContent(environment);
    }

    public OptionalProcessEnvironment(OptionalProcessEnvironment other) {
        super(ProcessEnvironment.class);
        setEnabled(other.isEnabled());
        setContent(new ProcessEnvironment(other.getContent()));
    }
}
