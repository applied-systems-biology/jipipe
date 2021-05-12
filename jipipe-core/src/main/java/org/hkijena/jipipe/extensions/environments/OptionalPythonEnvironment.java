package org.hkijena.jipipe.extensions.environments;

import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameter;

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
        setContent(environment);
    }

    public OptionalPythonEnvironment(OptionalPythonEnvironment other) {
        super(PythonEnvironment.class);
        setContent(new PythonEnvironment(other.getContent()));
    }
}
