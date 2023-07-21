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
