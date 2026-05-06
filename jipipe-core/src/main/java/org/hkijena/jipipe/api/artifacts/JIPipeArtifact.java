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
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.acceleration.JIPipeHardwareAccelerationMode;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JIPipeArtifact extends AbstractJIPipeParameterCollection implements Comparable<JIPipeArtifact>, JIPipeValidatable {
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;

    public JIPipeArtifact() {
    }

    public JIPipeArtifact(JIPipeArtifact other) {
        this.groupId = other.groupId;
        this.artifactId = other.artifactId;
        this.version = other.version;
        this.classifier = other.classifier;
    }

    public JIPipeArtifact(String fullArtifactId) {
        String[] component = org.apache.commons.lang3.StringUtils.split(fullArtifactId, ":");
        if (component.length != 2) {
            throw new IllegalArgumentException("Invalid artifact ID: " + fullArtifactId);
        }
        String[] versionClassifierItems = component[1].split("-");
        String[] packagePathItems = component[0].split("\\.");
        this.version = versionClassifierItems[0];
        this.classifier = versionClassifierItems[1];
        this.artifactId = packagePathItems[packagePathItems.length - 1];
        this.groupId = Arrays.stream(packagePathItems, 0, packagePathItems.length - 1).collect(Collectors.joining("."));
    }

    @SetJIPipeDocumentation(name = "Group ID", description = "The group ID (e.g., org.hkijena")
    @JIPipeParameter("group-id")
    @JsonGetter("group-id")
    public String getGroupId() {
        return groupId;
    }

    @JIPipeParameter("group-id")
    @JsonSetter("group-id")
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @SetJIPipeDocumentation(name = "Artifact ID", description = "The artifact ID (e.g., jipipe)")
    @JsonGetter("artifact-id")
    @JIPipeParameter("artifact-id")
    public String getArtifactId() {
        return artifactId;
    }

    @JIPipeParameter("artifact-id")
    @JsonSetter("artifact-id")
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @SetJIPipeDocumentation(name = "Version", description = "The version (e.g., 1.0.0")
    @JIPipeParameter("version")
    @JsonGetter("version")
    public String getVersion() {
        return version;
    }

    @JIPipeParameter("version")
    @JsonSetter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @SetJIPipeDocumentation(name = "Classifier", description = "Additional information about the artifact. Tags are separated by _. " +
            "Generally recognized tags are: linux, windows, macos, amd64, arm64, gpu, cu112, cu(CUDA version), wine, any")
    @JIPipeParameter("classifier")
    @JsonGetter("classifier")
    public String getClassifier() {
        return classifier;
    }

    @JIPipeParameter("classifier")
    @JsonSetter("classifier")
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * Returns a string GroupId.ArtifactId:Version-Classifier
     *
     * @return the full identifier string for this artifact
     */
    public String getFullId() {
        return getGroupId() + "." + getArtifactId() + ":" + getVersion() + "-" + getClassifier();
    }

    /**
     * Fills values from a full ID
     *
     * @param fullId the ID
     */
    public void setFullId(String fullId) {
        JIPipeArtifact artifact = new JIPipeArtifact(fullId);
        this.setArtifactId(artifact.getArtifactId());
        this.setVersion(artifact.getVersion());
        this.setClassifier(artifact.getClassifier());
        this.setGroupId(artifact.getGroupId());
    }

    /**
     * Returns a string GroupId.ArtifactId:Version-*
     *
     * @return the version-specific identifier string for this artifact
     */
    public String getFullId(ResolutionStatus status) {
        return switch (status) {
            case Full -> getFullId();
            case GroupNameVersion -> getGroupId() + "." + getArtifactId() + ":" + getVersion() + "-*";
            case GroupName -> getGroupId() + "." + getArtifactId() + ":*";
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }

    /**
     * Returns true if this artifact is fully resolved
     *
     * @return if the artifact is fully resolved
     */
    public boolean isFullyResolved() {
        return getResolutionStatus() == ResolutionStatus.Full;
    }

    /**
     * Returns the resolution status (in order)
     * Cannot handle intermediate globs
     *
     * @return the resolution status
     */
    public ResolutionStatus getResolutionStatus() {
        if (!"*".equals(getClassifier())) {
            return ResolutionStatus.Full;
        } else if (!"*".equals(getVersion())) {
            return ResolutionStatus.GroupNameVersion;
        } else {
            return ResolutionStatus.GroupName;
        }
    }

    /**
     * Returns true if this artifact is compatible with the current system
     * Includes non-native compatibility (e.g., x86 is compatible to amd64)
     *
     * @return if the artifact is compatible
     */
    public boolean isCompatible() {
        if("*".equals(getClassifier())) {
            return true;
        }

        String[] split = StringUtils.nullToEmpty(getClassifier()).split("_");
        for (String s : split) {
            if("any".equals(s)) {
                return true;
            }
        }

        if ("*".equals(getClassifier()) || "any".equalsIgnoreCase(getClassifier())) {
            return true;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (!getClassifier().contains("windows") && !getClassifier().contains("win32") && !getClassifier().contains("win64")) {
                return false;
            }
            if (SystemUtils.OS_ARCH == null) {
                return true;
            } else if (SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64")) {
                return getClassifier().contains("amd64") || getClassifier().contains("x86");
            } else {
                return getClassifier().contains("x86");
            }
        } else if (SystemUtils.IS_OS_LINUX) {
            if (!getClassifier().contains("linux")) {
                return false;
            }
            if (SystemUtils.OS_ARCH == null) {
                return true;
            } else if (SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64")) {
                return getClassifier().contains("amd64") || getClassifier().contains("x86");
            } else {
                return getClassifier().contains("x86");
            }
        } else if (SystemUtils.IS_OS_MAC) {
            if (!getClassifier().contains("macos")) {
                return false;
            }
            if (SystemUtils.OS_ARCH == null) {
                return true;
            } else if (SystemUtils.OS_ARCH.equals("arm64") || SystemUtils.OS_ARCH.equals("aarch64")) {
                return getClassifier().contains("arm64") || getClassifier().contains("amd64") || getClassifier().contains("x86") || getClassifier().contains("aarch64");
            } else if (SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64")) {
                return getClassifier().contains("amd64") || getClassifier().contains("x86");
            } else {
                return getClassifier().contains("x86");
            }
        }
        return false;
    }

    /**
     * Returns true if the artifact is perfectly compatible with the current system (excludes translation layers like x86 to amd64)
     *
     * @return if the artifact is perfectly compatible
     */
    public boolean isNative() {
        if ("any".equalsIgnoreCase(getClassifier())) {
            return true;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (!getClassifier().contains("windows")) {
                return false;
            }
            if (SystemUtils.OS_ARCH == null) {
                return true;
            } else if (SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64")) {
                return getClassifier().contains("amd64");
            } else {
                return getClassifier().contains("x86");
            }
        } else if (SystemUtils.IS_OS_LINUX) {
            if (!getClassifier().contains("linux")) {
                return false;
            }
            if (SystemUtils.OS_ARCH == null) {
                return true;
            } else if (SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64")) {
                return getClassifier().contains("amd64");
            } else {
                return getClassifier().contains("x86");
            }
        } else if (SystemUtils.IS_OS_MAC) {
            if (!getClassifier().contains("macos")) {
                return false;
            }
            if (SystemUtils.OS_ARCH == null) {
                return true;
            } else if (SystemUtils.OS_ARCH.equals("arm64")) {
                return getClassifier().contains("arm64");
            } else if (SystemUtils.OS_ARCH.equals("amd64") || SystemUtils.OS_ARCH.equals("x86_64")) {
                return getClassifier().contains("amd64");
            } else {
                return getClassifier().contains("x86");
            }
        }
        return false;
    }

    public boolean isRequireGPU() {
        return getClassifier().contains("gpu");
    }

    @Override
    public String toString() {
        return "Artifact " + getFullId();
    }

    @Override
    public int compareTo(@NotNull JIPipeArtifact o) {
        int compareName = getArtifactId().compareTo(o.getArtifactId());
        if (compareName == 0) {
            return -VersionUtils.compareVersions(getVersion(), o.getVersion()); // Never versions at the top
        } else {
            return compareName;
        }
    }

    public Path getDefaultInstallationPath(Path localRepositoryPath) {
        return localRepositoryPath.resolve(Paths.get(getGroupId().replace('.', '/'))).resolve(getArtifactId()).resolve(getVersion() + "-" + getClassifier());
    }

    public int getGPUVersion(String prefix) {
        for (String entry : getClassifier().split("_")) {
            if (entry.matches(prefix + "\\d\\d\\d")) {
                return Integer.parseInt(entry.substring(prefix.length()));
            }
        }
        // Handle legacy case
        if (isRequireGPU() && "cu".equals(prefix)) {
            return 102; // old artifacts were running on CUDA 10.2
        }
        return -1;
    }

    public boolean isGPUCompatible(JIPipeHardwareAccelerationMode accelerationPreference, Vector2iParameter accelerationPreferenceVersions) {
        int min = accelerationPreferenceVersions.getX();
        int max = accelerationPreferenceVersions.getY();
        if (accelerationPreference == JIPipeHardwareAccelerationMode.CPU) {
            return true;
        } else {
            int gpuVersion = getGPUVersion(accelerationPreference.getPrefix());
            if (gpuVersion == 0) {
                return false;
            }
            if (min > 0 && gpuVersion < min) {
                return false;
            }
            if (max > 0 && gpuVersion > max) {
                return false;
            }
            return true;
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        if (StringUtils.isNullOrEmpty(artifactId)) {
            reportContext.error()
                    .title("Invalid artifact ID")
                    .explanation("The artifact ID cannot be empty!")
                    .report(report);
        } else {
            if (!artifactId.matches("[a-z]+[a-z0-9_]*")) {
                reportContext.error()
                        .title("Invalid artifact ID")
                        .explanation("The artifact ID must be lowercase and can only contain alphanumeric characters and underscores!")
                        .report(report);
            }
        }
        if (StringUtils.isNullOrEmpty(version)) {
            reportContext.error()
                    .title("Invalid version")
                    .explanation("The version cannot be empty!")
                    .report(report);
        } else {
            if (!StringUtils.isValidVersion(version)) {
                reportContext.error()
                        .title("Invalid version")
                        .explanation("The version is not valid!")
                        .report(report);
            }
        }
    }

    public String getVersionWithoutRevision() {
        if (StringUtils.isValidVersion(version)) {
            int[] items = VersionUtils.getVersionComponents(version);
            int lastItem = items[items.length - 1];

            // JIPipe-style revisions start at 1000
            if (lastItem >= 1000) {
                int[] s = new int[items.length - 1];
                System.arraycopy(items, 0, s, 0, items.length - 1);
                return Arrays.stream(s).boxed().map(Object::toString).collect(Collectors.joining("."));
            } else {
                // No revision part
                return version;
            }
        }
        return version;
    }

    public int getVersionRevision() {
        if (StringUtils.isValidVersion(version)) {
            int[] items = VersionUtils.getVersionComponents(version);
            int lastItem = items[items.length - 1];

            // JIPipe-style revisions start at 1000
            if (lastItem >= 1000) {
                return lastItem;
            }
        }
        return 0;
    }

    /**
     * Returns true if a query matches the artifact. Only works for fully resolved artifacts.
     * @param query the query
     * @return if it matches
     * @throws IllegalStateException if the artifact's ID is not fully resolved
     */
    public boolean matchesQuery(String query) {
        if(getResolutionStatus() != ResolutionStatus.Full) {
            throw new IllegalStateException("Cannot match query for non-fully resolved artifacts!");
        }
        String regex = StringUtils.convertGlobToRegex(query);
        return getFullId().matches(regex);
    }

    /**
     * The current resolution status of this artifact
     */
    public enum ResolutionStatus {
        GroupName,
        GroupNameVersion,
        Full
    }

}
