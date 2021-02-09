package org.hkijena.jipipe.extensions.parameters.generators;

import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameter;

public class OptionalIntegerRange extends OptionalParameter<IntegerRange> {
    public OptionalIntegerRange() {
        super(IntegerRange.class);
        setContent(new IntegerRange());
    }

    public OptionalIntegerRange(IntegerRange content, boolean enabled) {
        super(IntegerRange.class);
        setContent(content);
        setEnabled(enabled);
    }

    public OptionalIntegerRange(OptionalIntegerRange other) {
        super(other);
        setContent(new IntegerRange(other.getContent()));
    }
}
