package org.hkijena.jipipe.extensions.parameters.library.quantities;

import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

public class OptionalQuantity extends OptionalParameter<Quantity> {
    public OptionalQuantity() {
        super(Quantity.class);
        setContent(new Quantity());
    }

    public OptionalQuantity(Quantity value, boolean enabled) {
        super(Quantity.class);
        setContent(value);
        setEnabled(enabled);
    }

    public OptionalQuantity(OptionalQuantity other) {
        super(Quantity.class);
        setEnabled(other.isEnabled());
        setContent(new Quantity(other.getContent()));
    }
}
