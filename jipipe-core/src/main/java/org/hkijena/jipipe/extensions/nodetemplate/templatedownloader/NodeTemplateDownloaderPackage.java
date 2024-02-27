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

package org.hkijena.jipipe.extensions.nodetemplate.templatedownloader;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

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
