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
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactOperationContext;
import org.hkijena.jipipe.plugins.artifacts.oras.OrasEnvironment;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.oci.OciManifest;
import org.hkijena.jipipe.utils.oci.OciManifestLayer;
import org.hkijena.jipipe.utils.process.ExtendedExecutor;
import org.hkijena.jipipe.utils.process.PeriodicProcessSidecarTask;

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

        // Query the OCI manifest to get the total size
        progressInfo.log("Querying ORAS manifest " + ociReference + " ...");
        Path manifestFile = tmpPath.resolve("manifest.json");
        orasEnvironment.runExecutable(List.of("manifest", "fetch", "--output", manifestFile.toString(), ociReference),
                Collections.emptyMap(),
                false,
                Collections.emptyList(),
                progressInfo);
        OciManifest manifest = JsonUtils.readFromFile(manifestFile, OciManifest.class);
        long totalSize = 0;
        for (OciManifestLayer layer : manifest.getLayers()) {
            totalSize += layer.getSize();
        }
        progressInfo.log("Total size is " + StringUtils.formatSize(totalSize));

        // Start download with a sidecar that monitors the size of the output directory
        progressInfo.log("Downloading using ORAS from " + ociReference + " ...");
        orasEnvironment.runExecutable(List.of("pull", "--output", tmpPath.toString(), ociReference),
                Collections.emptyMap(),
                false,
                List.of(new DownloadProgressSidecarTask(tmpPath, totalSize)),
                progressInfo);
        return PathUtils.findFileByExtensionRecursivelyIn(tmpPath, ".zip", ".tar.gz", ".tar.bz2", ".tar.xz");
    }

    public String getOciReference() {
        return ociReference;
    }

    public void setOciReference(String ociReference) {
        this.ociReference = ociReference;
    }

    public static class DownloadProgressSidecarTask extends PeriodicProcessSidecarTask {
        private final Path tmpPath;
        private final long totalSize;
        private long lastSize = 0;
        private int lastPercentage = 0;

        public DownloadProgressSidecarTask(Path tmpPath, long totalSize) {
            super(1000);
            this.tmpPath = tmpPath;
            this.totalSize = totalSize;
        }

        @Override
        protected void tick(ExtendedExecutor executor) {
            try {
                long currentSize = FileUtils.sizeOfDirectory(tmpPath.toFile());
                if(currentSize > lastSize) {
                    lastSize = currentSize;
                    int percentage = Math.max(0, Math.min(100, (int)(currentSize * 100.0 / totalSize)));
                    if(percentage != lastPercentage) {
                        executor.getProgressInfo().log("O R A S ->> [" + percentage + "%] Downloaded " + StringUtils.formatSize(Math.min(currentSize, totalSize))
                                + " / " +  StringUtils.formatSize(totalSize));
                        lastPercentage = percentage;
                    }
                }
            }
            catch (Throwable ignored) {
            }
        }
    }
}
