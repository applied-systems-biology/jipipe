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

package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

public class OptionalVector2iParameter extends OptionalParameter<Vector2iParameter> {
    public OptionalVector2iParameter() {
        super(Vector2iParameter.class);
    }

    public OptionalVector2iParameter(Vector2iParameter value, boolean enabled) {
        super(Vector2iParameter.class);
        setContent(value);
        setEnabled(enabled);
    }

    public OptionalVector2iParameter(OptionalVector2iParameter other) {
        super(other);
        setContent(new Vector2iParameter(other.getContent()));
    }
}
