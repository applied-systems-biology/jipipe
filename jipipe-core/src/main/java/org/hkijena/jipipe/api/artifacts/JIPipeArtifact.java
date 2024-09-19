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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactAccelerationPreference;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JIPipeArtifact implements Comparable<JIPipeArtifact> {
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
        String[] component = StringUtils.split(fullArtifactId, ":.");
        if (component.length < 3) {
            throw new IllegalArgumentException("Invalid artifact ID: " + fullArtifactId);
        }
        String[] versionClassifierItems = component[component.length - 1].split("-");
        this.version = versionClassifierItems[0];
        this.classifier = versionClassifierItems[1];
        this.artifactId = component[component.length - 2];
    }

    @JsonGetter("group-id")
    public String getGroupId() {
        return groupId;
    }

    @JsonSetter("group-id")
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @JsonGetter("artifact-id")
    public String getArtifactId() {
        return artifactId;
    }

    @JsonSetter("artifact-id")
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @JsonGetter("version")
    public String getVersion() {
        return version;
    }

    @JsonSetter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonGetter("classifier")
    public String getClassifier() {
        return classifier;
    }

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
     * Returns true if this artifact is compatible with the current system
     * Includes non-native compatibility (e.g., x86 is compatible to amd64)
     *
     * @return if the artifact is compatible
     */
    public boolean isCompatible() {
        if ("any".equalsIgnoreCase(getClassifier())) {
            return true;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (!getClassifier().contains("windows")) {
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
            } else if (SystemUtils.OS_ARCH.equals("arm64")) {
                return getClassifier().contains("arm64") || getClassifier().contains("amd64") || getClassifier().contains("x86");
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
            if(entry.matches(prefix + "\\d\\d\\d")) {
                return Integer.parseInt(entry.substring(prefix.length()));
            }
        }
        return 0;
    }

    public boolean isGPUCompatible(JIPipeArtifactAccelerationPreference accelerationPreference, Vector2iParameter accelerationPreferenceVersions) {
        int min = accelerationPreferenceVersions.getX();
        int max = accelerationPreferenceVersions.getY();
        if(accelerationPreference == JIPipeArtifactAccelerationPreference.CPU) {
            return true;
        }
        else {
            int gpuVersion = getGPUVersion(accelerationPreference.getPrefix());
            if(gpuVersion == 0) {
                return false;
            }
            if(min > 0 && gpuVersion < min) {
                return false;
            }
            if(max > 0 && gpuVersion > max) {
                return false;
            }
            return true;
        }
    }
}
