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
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;

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
