/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * JSON-serializable project metadata
 */
public class JIPipeMetadata implements JIPipeParameterCollection {
    private EventBus eventBus = new EventBus();
    private String name = "New project";
    private String description = "A JIPipe project";
    private JIPipeAuthorMetadata.List authors = new JIPipeAuthorMetadata.List();
    private String website = "";
    private String license = "";
    private String citation = "";
    private StringList dependencyCitations = new StringList();

    /**
     * Creates new empty instance
     */
    public JIPipeMetadata() {
    }

    /**
     * Copies metadata
     *
     * @param other The original metadata
     */
    public JIPipeMetadata(JIPipeMetadata other) {
        this.name = other.name;
        this.description = other.description;
        this.authors = other.authors;
        this.website = other.website;
        this.license = other.license;
        this.citation = other.citation;
        this.dependencyCitations = new StringList(other.dependencyCitations);
    }

    /**
     * @return Gets the name
     */
    @JIPipeDocumentation(name = "Name", description = "A name")
    @JIPipeParameter(value = "name", uiOrder = 0)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name the name
     */
    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    @JIPipeDocumentation(name = "Description", description = "A description")
    @JIPipeParameter(value = "description", uiOrder = 1)
    @StringParameterSettings(multiline = true, monospace = true)
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description the description
     */
    @JIPipeParameter("description")
    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;

    }

    /**
     * @return The authors
     */
    @JIPipeDocumentation(name = "Authors", description = "The list of authors and their affiliations")
    @JIPipeParameter(value = "authors", uiOrder = 2)
    @JsonGetter("authors")
    public JIPipeAuthorMetadata.List getAuthors() {
        return authors;
    }

    /**
     * Sets the authors
     *
     * @param authors the authors
     */
    @JIPipeParameter("authors")
    @JsonSetter("authors")
    public void setAuthors(JIPipeAuthorMetadata.List authors) {
        this.authors = authors;

    }

    /**
     * @return the website
     */
    @JIPipeDocumentation(name = "Website", description = "The website")
    @JIPipeParameter(value = "website", uiOrder = 3)
    @JsonGetter("website")
    public String getWebsite() {
        return website;
    }

    /**
     * Sets the website
     *
     * @param website the website
     */
    @JIPipeParameter("website")
    @JsonSetter("website")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/filetype-html.png")
    public void setWebsite(String website) {
        this.website = website;

    }

    /**
     * @return the license
     */
    @JIPipeDocumentation(name = "License", description = "A license name like GPL v2 or BSD 2-Clause. We recommend Open Source licenses.")
    @JIPipeParameter(value = "license", uiOrder = 6)
    @JsonGetter("license")
    public String getLicense() {
        return license;
    }

    /**
     * Sets the license
     *
     * @param license the license
     */
    @JIPipeParameter("license")
    @JsonSetter("license")
    public void setLicense(String license) {
        this.license = license;

    }

    /**
     * @return the citation
     */
    @JIPipeDocumentation(name = "Citation", description = "Reference to the work where the project is published")
    @JIPipeParameter(value = "citation", uiOrder = 4)
    @StringParameterSettings(monospace = true)
    @JsonGetter("citation")
    public String getCitation() {
        return citation;
    }

    /**
     * Sets the citation
     *
     * @param citation the citation
     */
    @JIPipeParameter("citation")
    @JsonSetter("citation")
    public void setCitation(String citation) {
        this.citation = citation;

    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Dependency citations", description = "A list of external work to cite")
    @JIPipeParameter(value = "dependency-citations", uiOrder = 5)
    @StringParameterSettings(monospace = true)
    public StringList getDependencyCitations() {
        return dependencyCitations;
    }

    @JIPipeParameter("dependency-citations")
    public void setDependencyCitations(StringList dependencyCitations) {
        this.dependencyCitations = dependencyCitations;

    }
}
