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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.Objects;

public class JIPipeArtifactRepositoryReference extends AbstractJIPipeParameterCollection {
    private String url;
    private String name;
    private String repository;
    private JIPipeArtifactRepositoryType type = JIPipeArtifactRepositoryType.SonatypeNexus;

    public JIPipeArtifactRepositoryReference() {
    }

    public JIPipeArtifactRepositoryReference(JIPipeArtifactRepositoryReference other) {
        this.url = other.url;
        this.repository = other.repository;
        this.type = other.type;
        this.name = other.name;
    }

    public JIPipeArtifactRepositoryReference(String name, String url, String repository, JIPipeArtifactRepositoryType type) {
        this.url = url;
        this.repository = repository;
        this.type = type;
    }

    @SetJIPipeDocumentation(name = "Name", description = "The name of the repository")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "URL", description = "The URL of the remote repository. For Sonatype Nexus, this is the base URL.")
    @JsonGetter("url")
    @JIPipeParameter("url")
    public String getUrl() {
        return url;
    }

    @JsonSetter("url")
    @JIPipeParameter("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @SetJIPipeDocumentation(name = "Repository", description = "The repository ID. For Sonatype Nexus, this is the name of the repository where the artifacts are stored.")
    @JsonGetter("repository")
    @JIPipeParameter("repository")
    public String getRepository() {
        return repository;
    }

    @JsonSetter("repository")
    @JIPipeParameter("repository")
    public void setRepository(String repository) {
        this.repository = repository;
    }

    @SetJIPipeDocumentation(name = "Type", description = "The repository type")
    @JIPipeParameter("type")
    @JsonGetter("type")
    public JIPipeArtifactRepositoryType getType() {
        return type;
    }

    @JIPipeParameter("type")
    @JsonSetter("type")
    public void setType(JIPipeArtifactRepositoryType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeArtifactRepositoryReference that = (JIPipeArtifactRepositoryReference) o;
        return Objects.equals(url, that.url) && Objects.equals(repository, that.repository) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, repository, type);
    }
}
