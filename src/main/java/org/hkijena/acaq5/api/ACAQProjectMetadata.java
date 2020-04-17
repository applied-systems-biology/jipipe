package org.hkijena.acaq5.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.StringParameterSettings;

/**
 * JSON-serializable project metadata
 */
public class ACAQProjectMetadata implements ACAQParameterHolder {
    private EventBus eventBus = new EventBus();
    private String name = "New project";
    private String description = "An ACAQ5 project";
    private String authors = "";
    private String website = "";
    private String license = "";
    private String citation = "";

    /**
     * Creates new empty instance
     */
    public ACAQProjectMetadata() {
    }

    /**
     * Copies metadata
     *
     * @param other The original metadata
     */
    public ACAQProjectMetadata(ACAQProjectMetadata other) {
        this.name = other.name;
        this.description = other.description;
        this.authors = other.authors;
        this.website = other.website;
        this.license = other.license;
        this.citation = other.citation;
    }

    /**
     * @return Gets the name
     */
    @ACAQDocumentation(name = "Name")
    @ACAQParameter("name")
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
    @ACAQDocumentation(name = "Description")
    @ACAQParameter("description")
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
    @ACAQDocumentation(name = "Authors")
    @ACAQParameter("authors")
    @JsonGetter("authors")
    public String getAuthors() {
        return authors;
    }

    /**
     * Sets the authors
     *
     * @param authors the authors
     */
    @ACAQParameter("authors")
    @JsonSetter("authors")
    public void setAuthors(String authors) {
        this.authors = authors;
        getEventBus().post(new ParameterChangedEvent(this, "authors"));
    }

    /**
     * @return the website
     */
    @ACAQDocumentation(name = "Website")
    @ACAQParameter("website")
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
    public void setWebsite(String website) {
        this.website = website;
        getEventBus().post(new ParameterChangedEvent(this, "website"));
    }

    /**
     * @return the license
     */
    @ACAQDocumentation(name = "License")
    @ACAQParameter("license")
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
    @ACAQDocumentation(name = "Citation")
    @ACAQParameter("citation")
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
}
