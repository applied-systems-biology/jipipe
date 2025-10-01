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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactOperationContext;
import org.hkijena.jipipe.plugins.artifacts.oras.OrasEnvironment;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class JIPipeOrasRemoteArtifactSource extends JIPipeRemoteArtifactSource{

    @JsonProperty("oci-reference")
    private String ociReference;

    public JIPipeOrasRemoteArtifactSource() {
    }

    public JIPipeOrasRemoteArtifactSource(String ociReference) {
        this.ociReference = ociReference;
    }

    public JIPipeOrasRemoteArtifactSource(JIPipeOrasRemoteArtifactSource other) {
        this.ociReference = other.ociReference;
    }

    @Override
    @JsonGetter("type")
    public JIPipeRemoteArtifactSourceType getType() {
        return JIPipeRemoteArtifactSourceType.ORAS;
    }

    @Override
    public JIPipeRemoteArtifactSource duplicate() {
        return new  JIPipeOrasRemoteArtifactSource(this);
    }

    @Override
    public Path downloadArchive(JIPipeArtifactOperationContext context, Path tmpPath, JIPipeProgressInfo progressInfo) {
        OrasEnvironment orasEnvironment = JIPipe.getArtifacts().getOrasEnvironment(context, progressInfo.resolveAndLog("Configure ORAS"));
        orasEnvironment.runExecutable(List.of("pull", "--output", tmpPath.toString(), ociReference), Collections.emptyMap(), false, progressInfo);
        return PathUtils.findFileByExtensionRecursivelyIn(tmpPath, ".zip", ".tar.gz", ".tar.bz2", ".tar.xz");
    }

    public String getOciReference() {
        return ociReference;
    }

    public void setOciReference(String ociReference) {
        this.ociReference = ociReference;
    }
}
