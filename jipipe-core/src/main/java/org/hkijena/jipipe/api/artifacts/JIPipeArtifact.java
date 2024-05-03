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

    public boolean isCompatible() {
        if ("any".equalsIgnoreCase(getClassifier())) {
            return true;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            return getClassifier().startsWith("win");
        } else if (SystemUtils.IS_OS_LINUX) {
            return getClassifier().startsWith("linux");
        } else if (SystemUtils.IS_OS_MAC) {
            return getClassifier().startsWith("mac");
        } else {
            return false;
        }
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
}
