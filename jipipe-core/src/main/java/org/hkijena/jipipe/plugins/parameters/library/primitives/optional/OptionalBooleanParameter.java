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
 * Optional {@link Boolean}
 */
public class OptionalBooleanParameter extends OptionalParameter<Boolean> {

    /**
     * Creates a new instance
     */
    public OptionalBooleanParameter() {
        super(Boolean.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalBooleanParameter(OptionalBooleanParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Boolean setNewInstance() {
        setContent(false);
        return false;
    }
}
