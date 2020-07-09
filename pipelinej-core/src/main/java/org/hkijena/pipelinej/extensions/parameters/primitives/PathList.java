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

package org.hkijena.pipelinej.extensions.parameters.primitives;

import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;

import java.nio.file.Path;

/**
 * Collection of paths. Used as parameter type.
 */
public class PathList extends ListParameter<Path> {
    /**
     * Creates a new instance
     */
    public PathList() {
        super(Path.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PathList(PathList other) {
        super(Path.class);
        addAll(other);
    }
}
