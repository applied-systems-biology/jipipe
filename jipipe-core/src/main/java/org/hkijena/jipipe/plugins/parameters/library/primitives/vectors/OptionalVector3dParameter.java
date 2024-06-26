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

public class OptionalVector3dParameter extends OptionalParameter<Vector3dParameter> {
    public OptionalVector3dParameter() {
        super(Vector3dParameter.class);
    }

    public OptionalVector3dParameter(Vector3dParameter value, boolean enabled) {
        super(Vector3dParameter.class);
        setContent(value);
        setEnabled(enabled);
    }

    public OptionalVector3dParameter(OptionalVector3dParameter other) {
        super(other);
        setContent(new Vector3dParameter(other.getContent()));
    }
}
