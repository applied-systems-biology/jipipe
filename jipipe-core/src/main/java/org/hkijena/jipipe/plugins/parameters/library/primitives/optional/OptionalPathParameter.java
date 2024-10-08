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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Optional {@link Path}
 */
public class OptionalPathParameter extends OptionalParameter<Path> {

    /**
     * Creates a new instance
     */
    public OptionalPathParameter() {
        super(Path.class);
    }

    public OptionalPathParameter(Path value, boolean enabled) {
        super(Path.class);
        setContent(value);
        setEnabled(enabled);
    }


    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalPathParameter(OptionalPathParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Path setNewInstance() {
        setContent(Paths.get(""));
        return getContent();
    }
}
