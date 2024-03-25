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

package org.hkijena.jipipe.plugins.r;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

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
        setEnabled(other.isEnabled());
        setContent(new REnvironment(other.getContent()));
    }
}
