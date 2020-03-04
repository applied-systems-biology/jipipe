package org.hkijena.acaq5.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class ACAQProjectMetadata {
    private String name = "New project";
    private String description = "An ACAQ5 project";
    private String authors = "";
    private String website = "";
    private String license = "";
    private String citation = "";

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonGetter("authors")
    public String getAuthors() {
        return authors;
    }

    @JsonSetter("authors")
    public void setAuthors(String authors) {
        this.authors = authors;
    }

    @JsonGetter("website")
    public String getWebsite() {
        return website;
    }

    @JsonSetter("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @JsonGetter("license")
    public String getLicense() {
        return license;
    }

    @JsonSetter("license")
    public void setLicense(String license) {
        this.license = license;
    }

    @JsonGetter("citation")
    public String getCitation() {
        return citation;
    }

    @JsonSetter("citation")
    public void setCitation(String citation) {
        this.citation = citation;
    }
}
