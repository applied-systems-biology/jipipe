package org.hkijena.jipipe.extensions.environments;

import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameter;

/**
 * An optional {@link REnvironment}
 */
public class OptionalREnvironment extends OptionalParameter<REnvironment> {
    public OptionalREnvironment() {
        super(REnvironment.class);
        setContent(new REnvironment());
    }

    public OptionalREnvironment(REnvironment environment) {
        super(REnvironment.class);
        setContent(environment);
    }
    
    public OptionalREnvironment(OptionalREnvironment other) {
        super(REnvironment.class);
        setContent(new REnvironment(other.getContent()));
    }
}
