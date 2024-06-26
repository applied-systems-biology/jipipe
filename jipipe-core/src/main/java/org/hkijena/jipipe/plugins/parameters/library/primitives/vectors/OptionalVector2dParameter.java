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

public class OptionalVector2dParameter extends OptionalParameter<Vector2dParameter> {
    public OptionalVector2dParameter() {
        super(Vector2dParameter.class);
    }

    public OptionalVector2dParameter(Vector2dParameter value, boolean enabled) {
        super(Vector2dParameter.class);
        setContent(value);
        setEnabled(enabled);
    }

    public OptionalVector2dParameter(OptionalVector2dParameter other) {
        super(other);
        setContent(new Vector2dParameter(other.getContent()));
    }
}
