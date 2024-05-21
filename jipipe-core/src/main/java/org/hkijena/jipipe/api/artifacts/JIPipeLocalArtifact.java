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

package org.hkijena.jipipe.api.artifacts;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.nio.file.Path;

public class JIPipeLocalArtifact extends JIPipeArtifact {
    private Path localPath;

    public JIPipeLocalArtifact() {
    }

    public JIPipeLocalArtifact(JIPipeLocalArtifact other) {
        super(other);
        this.localPath = other.localPath;
    }

    @JsonGetter("local-path")
    public Path getLocalPath() {
        return localPath;
    }

    @JsonSetter("local-path")
    public void setLocalPath(Path localPath) {
        this.localPath = localPath;
    }
}
