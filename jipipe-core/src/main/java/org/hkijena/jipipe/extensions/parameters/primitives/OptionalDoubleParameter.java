/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameter;

/**
 * Optional {@link Double}
 */
public class OptionalDoubleParameter extends OptionalParameter<Double> {

    /**
     * Creates a new instance
     */
    public OptionalDoubleParameter() {
        super(Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalDoubleParameter(OptionalDoubleParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    public OptionalDoubleParameter(double value, boolean enabled) {
        super(Double.class);
        setContent(value);
        setEnabled(enabled);
    }

    @Override
    public Double setNewInstance() {
        setContent(0.0);
        return 0.0;
    }
}
