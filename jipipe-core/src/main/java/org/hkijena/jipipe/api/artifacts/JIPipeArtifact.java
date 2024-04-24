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

public class JIPipeArtifact {
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

    public String getFullId() {
        return  getGroupId() + "." + getArtifactId() + ":" + getVersion() + "-" + getClassifier();
    }

    @Override
    public String toString() {
        return "Artifact " + getFullId();
    }
}
