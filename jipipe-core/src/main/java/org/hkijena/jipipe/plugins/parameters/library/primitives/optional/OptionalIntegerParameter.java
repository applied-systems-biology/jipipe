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

package org.hkijena.jipipe.plugins.parameters.library.primitives.optional;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

/**
 * Optional {@link Integer}
 */
public class OptionalIntegerParameter extends OptionalParameter<Integer> {

    /**
     * Creates a new instance
     */
    public OptionalIntegerParameter() {
        super(Integer.class);
    }

    public OptionalIntegerParameter(boolean enabled, int value) {
        super(Integer.class);
        setContent(value);
        setEnabled(enabled);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalIntegerParameter(OptionalIntegerParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Integer setNewInstance() {
        setContent(0);
        return 0;
    }

    public int orElse(int defaultValue) {
        return isEnabled() ? getContent() : defaultValue;
    }
}
