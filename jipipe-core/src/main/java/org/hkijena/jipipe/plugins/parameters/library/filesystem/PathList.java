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

package org.hkijena.jipipe.plugins.parameters.library.filesystem;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

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
    public PathList(Collection<Path> other) {
        super(Path.class);
        addAll(other);
    }

    @Override
    public Path addNewInstance() {
        Path path = Paths.get("");
        add(path);
        return path;
    }
}
