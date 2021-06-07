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

package org.hkijena.jipipe.extensions.omero.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Data that stores a reference to an OMERO project
 */
@JIPipeDocumentation(name = "OMERO Project", description = "An OMERO project ID")
@JIPipeDataStorageDocumentation("Contains a single *.json file that stores the <pre>project-id</pre> in a JSON object.")
public class OMEROProjectReferenceData implements JIPipeData {
    private long projectId;

    public OMEROProjectReferenceData(long projectId) {
        this.projectId = projectId;
    }

    public static OMEROProjectReferenceData importFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROProjectReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("project-id")
    public long getProjectId() {
        return projectId;
    }

    @JsonSetter("project-id")
    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path jsonFile = storageFilePath.resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new OMEROProjectReferenceData(projectId);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return "OMERO Project ID=" + projectId;
    }
}
