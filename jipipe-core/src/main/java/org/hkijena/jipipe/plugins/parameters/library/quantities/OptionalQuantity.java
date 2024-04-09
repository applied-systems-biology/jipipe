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

package org.hkijena.jipipe.plugins.parameters.library.quantities;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

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
