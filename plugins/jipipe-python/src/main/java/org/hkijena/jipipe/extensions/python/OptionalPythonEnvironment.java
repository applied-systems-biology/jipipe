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

package org.hkijena.jipipe.extensions.python;

import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

/**
 * An optional {@link PythonEnvironment}
 */
public class OptionalPythonEnvironment extends OptionalParameter<PythonEnvironment> {
    public OptionalPythonEnvironment() {
        super(PythonEnvironment.class);
        setContent(new PythonEnvironment());
    }

    public OptionalPythonEnvironment(PythonEnvironment environment) {
        super(PythonEnvironment.class);
        setEnabled(true);
        setContent(environment);
    }

    public OptionalPythonEnvironment(OptionalPythonEnvironment other) {
        super(PythonEnvironment.class);
        setEnabled(other.isEnabled());
        setContent(new PythonEnvironment(other.getContent()));
    }
}
