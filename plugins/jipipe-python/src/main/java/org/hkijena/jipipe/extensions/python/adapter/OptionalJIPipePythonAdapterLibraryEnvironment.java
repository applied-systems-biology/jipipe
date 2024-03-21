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

package org.hkijena.jipipe.extensions.python.adapter;

import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

/**
 * An optional {@link JIPipePythonAdapterLibraryEnvironment}
 */
public class OptionalJIPipePythonAdapterLibraryEnvironment extends OptionalParameter<JIPipePythonAdapterLibraryEnvironment> {
    public OptionalJIPipePythonAdapterLibraryEnvironment() {
        super(JIPipePythonAdapterLibraryEnvironment.class);
        setContent(new JIPipePythonAdapterLibraryEnvironment());
    }

    public OptionalJIPipePythonAdapterLibraryEnvironment(JIPipePythonAdapterLibraryEnvironment environment) {
        super(JIPipePythonAdapterLibraryEnvironment.class);
        setEnabled(true);
        setContent(environment);
    }

    public OptionalJIPipePythonAdapterLibraryEnvironment(OptionalJIPipePythonAdapterLibraryEnvironment other) {
        super(JIPipePythonAdapterLibraryEnvironment.class);
        setEnabled(other.isEnabled());
        setContent(new JIPipePythonAdapterLibraryEnvironment(other.getContent()));
    }
}
