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

package org.hkijena.jipipe.api.artifacts.sources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactOperationContext;

import java.nio.file.Path;

public class JIPipeLocalRemoteArtifactSource extends JIPipeRemoteArtifactSource {

    @JsonProperty("file-path")
    private Path filePath;

    public JIPipeLocalRemoteArtifactSource() {
    }

    public JIPipeLocalRemoteArtifactSource(Path filePath) {
        this.filePath = filePath;
    }

    public JIPipeLocalRemoteArtifactSource(JIPipeLocalRemoteArtifactSource other) {
        this.filePath = other.filePath;
    }

    @Override
    @JsonGetter("type")
    public JIPipeRemoteArtifactSourceType getType() {
        return JIPipeRemoteArtifactSourceType.Local;
    }

    @Override
    public JIPipeRemoteArtifactSource duplicate() {
        return new JIPipeLocalRemoteArtifactSource(this);
    }

    @Override
    public Path downloadArchive(JIPipeArtifactOperationContext context, Path tmpPath, JIPipeProgressInfo progressInfo) {
        return filePath;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
}
