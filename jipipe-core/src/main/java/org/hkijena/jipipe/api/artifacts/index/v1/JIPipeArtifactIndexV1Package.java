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

package org.hkijena.jipipe.api.artifacts.index.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JIPipeArtifactIndexV1Package {
    @JsonProperty("query")
    private String query;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("license")
    private String license;

    @JsonProperty("version")
    private String version;

    @JsonProperty("homepage")
    private String homepage;

    @JsonProperty("sources")
    private List<JIPipeArtifactIndexV1PackageSource> sources = new ArrayList<>();

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @JsonProperty("maintainers")
    private List<JIPipeArtifactIndexV1PackageMaintainer> maintainers = new ArrayList<>();

    @JsonProperty("updated")
    private String updated;

    public JIPipeArtifactIndexV1Package() {
    }

    public List<JIPipeArtifactIndexV1PackageMaintainer> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<JIPipeArtifactIndexV1PackageMaintainer> maintainers) {
        this.maintainers = maintainers;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public List<JIPipeArtifactIndexV1PackageSource> getSources() {
        return sources;
    }

    public void setSources(List<JIPipeArtifactIndexV1PackageSource> sources) {
        this.sources = sources;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    /**
     * Returns a basic artifact from this package by parsing the query
     * @return the artifact
     */
    public JIPipeArtifact toArtifact() {
       return new JIPipeArtifact(getQuery());
    }
}

