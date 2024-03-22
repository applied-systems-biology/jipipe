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

package org.hkijena.jipipe.desktop.app.project.templatedownloader;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

public class JIPipeDesktopProjectTemplateDownloaderRepository {
    private String name;
    private List<JIPipeDesktopProjectTemplateDownloaderPackage> files = new ArrayList<>();

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("files")
    public List<JIPipeDesktopProjectTemplateDownloaderPackage> getFiles() {
        return files;
    }

    @JsonSetter("files")
    public void setFiles(List<JIPipeDesktopProjectTemplateDownloaderPackage> files) {
        this.files = files;
    }
}
