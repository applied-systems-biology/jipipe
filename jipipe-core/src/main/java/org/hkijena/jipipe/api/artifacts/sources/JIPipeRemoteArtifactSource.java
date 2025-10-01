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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactOperationContext;

import java.nio.file.Path;

/**
 * Encapsulates a remote artifact source together with helper methods
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JIPipeHttpRemoteArtifactSource.class, name = "HTTP"),
        @JsonSubTypes.Type(value = JIPipeOrasRemoteArtifactSource.class, name = "ORAS"),
        @JsonSubTypes.Type(value = JIPipeLocalRemoteArtifactSource.class, name = "Local")
})
public abstract class JIPipeRemoteArtifactSource {

    @JsonGetter("type")
    public abstract JIPipeRemoteArtifactSourceType getType();

    public abstract JIPipeRemoteArtifactSource duplicate();

    /**
     * Downloads the source as archive and returns the path to the archive file
     *
     * @param context
     * @param tmpPath      a temporary directory where downloaded files can be placed
     * @param progressInfo the progress info
     * @return the downloaded archive
     */
    public abstract Path downloadArchive(JIPipeArtifactOperationContext context, Path tmpPath, JIPipeProgressInfo progressInfo);
}
