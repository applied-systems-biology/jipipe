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

import java.util.HashMap;
import java.util.Map;

/**
 * An index that follows the JIPipe Artifact Index v1 standard
 */
public class JIPipeArtifactIndexV1 {
    @JsonProperty("base")
    private String base;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("repo")
    private String repo;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("version")
    private int version = 1;

    @JsonProperty("packages")
    private Map<String, JIPipeArtifactIndexV1Package> packages = new HashMap<>();

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, JIPipeArtifactIndexV1Package> getPackages() {
        return packages;
    }

    public void setPackages(Map<String, JIPipeArtifactIndexV1Package> packages) {
        this.packages = packages;
    }
}
