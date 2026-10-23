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

package org.hkijena.jipipe.api.project.io;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.project.JIPipeProjectMetadata;

import java.util.Set;

/**
 * An immutable snapshot of a project file's metadata and dependencies,
 * extracted by {@link JIPipeProjectIOs#peek} without loading the project.
 */
public final class JIPipeProjectPeek {

    private final Set<JIPipeDependency> dependencies;
    private final JIPipeProjectMetadata metadata;

    /**
     * Creates a new peek.
     *
     * @param dependencies the project dependencies
     * @param metadata     the project metadata
     */
    public JIPipeProjectPeek(Set<JIPipeDependency> dependencies, JIPipeProjectMetadata metadata) {
        this.dependencies = dependencies;
        this.metadata = metadata;
    }

    /**
     * Gets the project dependencies declared in the file.
     *
     * @return the dependencies
     */
    public Set<JIPipeDependency> getDependencies() {
        return dependencies;
    }

    /**
     * Gets the project metadata declared in the file.
     *
     * @return the metadata
     */
    public JIPipeProjectMetadata getMetadata() {
        return metadata;
    }
}
