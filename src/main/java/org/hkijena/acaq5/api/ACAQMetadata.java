package org.hkijena.acaq5.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;

/**
 * JSON-serializable project metadata
 */
public class ACAQMetadata implements ACAQParameterCollection {
    private EventBus eventBus = new EventBus();
    private String name = "New project";
    private String description = "An ACAQ5 project";
    private ACAQAuthorMetadata.List authors = new ACAQAuthorMetadata.List();
    private String website = "";
    private String license = "";
    private String citation = "";
    private StringList dependencyCitations = new StringList();

    /**
     * Creates new empty instance
     */
    public ACAQMetadata() {
    }

    /**
     * Copies metadata
     *
     * @param other The original metadata
     */
    public ACAQMetadata(ACAQMetadata other) {
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
    @ACAQDocumentation(name = "Name", description = "A name")
    @ACAQParameter(value = "name", uiOrder = 0)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name the name
     */
    @ACAQParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
        getEventBus().post(new ParameterChangedEvent(this, "name"));
    }

    /**
     * @return the description
     */
    @ACAQDocumentation(name = "Description", description = "A description")
    @ACAQParameter(value = "description", uiOrder = 1)
    @StringParameterSettings(multiline = true)
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description the description
     */
    @ACAQParameter("description")
    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        getEventBus().post(new ParameterChangedEvent(this, "description"));
    }

    /**
     * @return The authors
     */
    @ACAQDocumentation(name = "Authors", description = "The list of authors and their affiliations")
    @ACAQParameter(value = "authors", uiOrder = 2)
    @JsonGetter("authors")
    public ACAQAuthorMetadata.List getAuthors() {
        return authors;
    }

    /**
     * Sets the authors
     *
     * @param authors the authors
     */
    @ACAQParameter("authors")
    @JsonSetter("authors")
    public void setAuthors(ACAQAuthorMetadata.List authors) {
        this.authors = authors;
        getEventBus().post(new ParameterChangedEvent(this, "authors"));
    }

    /**
     * @return the website
     */
    @ACAQDocumentation(name = "Website", description = "The website")
    @ACAQParameter(value = "website", uiOrder = 3)
    @JsonGetter("website")
    public String getWebsite() {
        return website;
    }

    /**
     * Sets the website
     *
     * @param website the website
     */
    @ACAQParameter("website")
    @JsonSetter("website")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/filetype-html.png")
    public void setWebsite(String website) {
        this.website = website;
        getEventBus().post(new ParameterChangedEvent(this, "website"));
    }

    /**
     * @return the license
     */
    @ACAQDocumentation(name = "License", description = "A license name like GPL v2 or BSD 2-Clause. We recommend Open Source licenses.")
    @ACAQParameter(value = "license", uiOrder = 6)
    @JsonGetter("license")
    public String getLicense() {
        return license;
    }

    /**
     * Sets the license
     *
     * @param license the license
     */
    @ACAQParameter("license")
    @JsonSetter("license")
    public void setLicense(String license) {
        this.license = license;
        getEventBus().post(new ParameterChangedEvent(this, "license"));
    }

    /**
     * @return the citation
     */
    @ACAQDocumentation(name = "Citation", description = "Reference to the work where the project is published")
    @ACAQParameter(value = "citation", uiOrder = 4)
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
    @ACAQParameter("citation")
    @JsonSetter("citation")
    public void setCitation(String citation) {
        this.citation = citation;
        getEventBus().post(new ParameterChangedEvent(this, "citation"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Dependency citations", description = "A list of external work to cite")
    @ACAQParameter(value = "dependency-citations", uiOrder = 5)
    @StringParameterSettings(monospace = true)
    public StringList getDependencyCitations() {
        return dependencyCitations;
    }

    @ACAQParameter("dependency-citations")
    public void setDependencyCitations(StringList dependencyCitations) {
        this.dependencyCitations = dependencyCitations;
        getEventBus().post(new ParameterChangedEvent(this, "dependency-citations"));
    }
}
