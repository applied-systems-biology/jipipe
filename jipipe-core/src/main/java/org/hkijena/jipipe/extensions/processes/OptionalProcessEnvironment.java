package org.hkijena.jipipe.extensions.processes;

import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

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
