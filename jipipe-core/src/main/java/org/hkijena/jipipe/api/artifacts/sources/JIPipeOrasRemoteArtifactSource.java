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
import java.util.Locale;

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
        private boolean firstTick = true;
        private boolean downloadComplete = false;
        
        // Time tracking fields
        private long startTime = System.currentTimeMillis();
        private long currentSpeed = 0;
        private long finalSpeed = 0;
        
        // Animation fields
        private static final String[] ANIMATION_PATTERNS = {
            ">----", "->---", "-->--", "--->-", "---->"
        };
        private int animationIndex = 0;

        public DownloadProgressSidecarTask(Path tmpPath, long totalSize) {
            super(1000);
            this.tmpPath = tmpPath;
            this.totalSize = totalSize;
        }

        @Override
        protected void tick(ExtendedExecutor executor) {
            try {
                long currentSize = FileUtils.sizeOfDirectory(tmpPath.toFile());
                long currentTime = System.currentTimeMillis();
                
                // Check if download is complete
                if (currentSize >= totalSize && !downloadComplete) {
                    downloadComplete = true;
                    onDownloadComplete(executor, currentSize, currentTime);
                    return;
                }
                
                // Show initial information on first tick
                if (firstTick) {
                    showInitialInfo(executor);
                    firstTick = false;
                }
                
                if(currentSize > lastSize) {
                    lastSize = currentSize;
                    
                    // Calculate speed (bytes per second)
                    long timeDiff = currentTime - startTime;
                    if (timeDiff > 0) {
                        currentSpeed = (currentSize * 1000) / timeDiff;
                    }
                    
                    int percentage = Math.max(0, Math.min(100, (int)(currentSize * 100.0 / totalSize)));
                    if(percentage != lastPercentage) {
                        // Calculate elapsed time
                        long elapsedMillis = currentTime - startTime;
                        String elapsedDuration = StringUtils.formatDuration(elapsedMillis);
                        
                        // Calculate estimated remaining time with better edge case handling
                        String estimatedDuration = "N/A";
                        if (currentSpeed > 0) {
                            if (currentSize < totalSize) {
                                long remainingBytes = totalSize - currentSize;
                                // Handle edge case: if remaining bytes is very small, show minimal time
                                if (remainingBytes < 1024) { // Less than 1KB
                                    estimatedDuration = "< 1s";
                                } else {
                                    long estimatedMillis = (remainingBytes * 1000) / currentSpeed;
                                    estimatedDuration = StringUtils.formatDuration(estimatedMillis);
                                }
                            } else {
                                estimatedDuration = "Complete";
                            }
                        } else if (currentSize < totalSize) {
                            estimatedDuration = "Calculating...";
                        }
                        
                        // Get animated arrow pattern
                        String animatedArrow = ANIMATION_PATTERNS[animationIndex];
                        animationIndex = (animationIndex + 1) % ANIMATION_PATTERNS.length;
                        
                        // Format speed in MB/s
                        String speedText;
                        if (currentSpeed > 0) {
                            double speedMBs = currentSpeed / (1024.0 * 1024.0);
                            speedText = String.format(Locale.US, "%.2f MB/s", speedMBs);
                        } else {
                            speedText = "? MB/s";
                        }
                        
                        // Format progress message
                        String progressMessage = String.format("O R A S [%s] [%d%%] Elapsed: %s | Estimated: %s | Downloaded: %s / %s | Speed: %s",
                                animatedArrow,
                                percentage,
                                elapsedDuration,
                                estimatedDuration,
                                StringUtils.formatSize(Math.min(currentSize, totalSize)),
                                StringUtils.formatSize(totalSize),
                                speedText);
                        
                        executor.getProgressInfo().log(progressMessage);
                        lastPercentage = percentage;
                    }
                }
            }
            catch (Throwable ignored) {
            }
        }
        
        private void showInitialInfo(ExtendedExecutor executor) {
            String initialMessage = String.format("O R A S [>----] [0%%] Elapsed: 0s | Estimated: Calculating... | Downloaded: %s / %s | Speed: ? MB/s",
                    StringUtils.formatSize(0),
                    StringUtils.formatSize(totalSize));
            executor.getProgressInfo().log(initialMessage);
        }
        
        private void onDownloadComplete(ExtendedExecutor executor, long finalSize, long completionTime) {
            // Calculate final speed
            long totalTimeDiff = completionTime - startTime;
            if (totalTimeDiff > 0) {
                finalSpeed = (finalSize * 1000) / totalTimeDiff;
            }
            
            // Format speed in MB/s
            String speedText;
            if (finalSpeed > 0) {
                double speedMBs = finalSpeed / (1024.0 * 1024.0);
                speedText = String.format("%.2f MB/s", speedMBs);
            } else {
                speedText = "? MB/s";
            }
            
            // Calculate final statistics
            long elapsedMillis = completionTime - startTime;
            String elapsedDuration = StringUtils.formatDuration(elapsedMillis);
            
            // Get final animation state
            String animatedArrow = ANIMATION_PATTERNS[animationIndex];
            
            // Format completion message
            String completionMessage = String.format("O R A S [%s] [100%%] Elapsed: %s | Downloaded: %s / %s | Speed: %s",
                    animatedArrow,
                    elapsedDuration,
                    StringUtils.formatSize(finalSize),
                    StringUtils.formatSize(totalSize),
                    speedText);
            
            executor.getProgressInfo().log(completionMessage);
        }
    }
}
