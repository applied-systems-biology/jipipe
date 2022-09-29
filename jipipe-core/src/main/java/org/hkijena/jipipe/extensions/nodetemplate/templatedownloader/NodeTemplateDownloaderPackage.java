package org.hkijena.jipipe.extensions.nodetemplate.templatedownloader;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;

public class NodeTemplateDownloaderPackage {
    private String name;
    private String type;
    private String description;
    private String sizeInfo;
    private String website;
    private String url;

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("type")
    public String getType() {
        return type;
    }

    @JsonSetter("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonGetter("size-info")
    public String getSizeInfo() {
        return sizeInfo;
    }

    @JsonSetter("size-info")
    public void setSizeInfo(String sizeInfo) {
        this.sizeInfo = sizeInfo;
    }

    @JsonGetter("website")
    public String getWebsite() {
        return website;
    }

    @JsonSetter("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @JsonGetter("url")
    public String getUrl() {
        return url;
    }

    @JsonSetter("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return name + " [" + url + "]";
    }
}
