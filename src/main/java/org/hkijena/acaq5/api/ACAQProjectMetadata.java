package org.hkijena.acaq5.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.parameters.ACAQParameter;

public class ACAQProjectMetadata {
    private String name = "New project";
    private String description = "An ACAQ5 project";
    private String authors = "";
    private String website = "";
    private String license = "";
    private String citation = "";

    @ACAQDocumentation(name = "Name")
    @ACAQParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @ACAQParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @ACAQDocumentation(name = "Description")
    @ACAQParameter("description")
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @ACAQParameter("description")
    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @ACAQDocumentation(name = "Authors")
    @ACAQParameter("authors")
    @JsonGetter("authors")
    public String getAuthors() {
        return authors;
    }

    @ACAQParameter("authors")
    @JsonSetter("authors")
    public void setAuthors(String authors) {
        this.authors = authors;
    }

    @ACAQDocumentation(name = "Website")
    @ACAQParameter("website")
    @JsonGetter("website")
    public String getWebsite() {
        return website;
    }

    @ACAQParameter("website")
    @JsonSetter("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @ACAQDocumentation(name = "License")
    @ACAQParameter("license")
    @JsonGetter("license")
    public String getLicense() {
        return license;
    }

    @ACAQParameter("license")
    @JsonSetter("license")
    public void setLicense(String license) {
        this.license = license;
    }

    @ACAQDocumentation(name = "Citation")
    @ACAQParameter("citation")
    @JsonGetter("citation")
    public String getCitation() {
        return citation;
    }

    @ACAQParameter("citation")
    @JsonSetter("citation")
    public void setCitation(String citation) {
        this.citation = citation;
    }
}
