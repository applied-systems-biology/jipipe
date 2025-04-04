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

package org.hkijena.jipipe.plugins.omero.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import omero.gateway.model.AnnotationData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Data that stores a reference to an OMERO project
 */
@SetJIPipeDocumentation(name = "OMERO Annotation", description = "An OMERO annotation ID")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file that stores the <pre>annotation-id</pre> in a JSON object.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/omero-project-reference-data.schema.json")
public class OMEROAnnotationReferenceData implements JIPipeData {
    private long annotationId;
    private String name;
    private String description;
    private String contentString;
    private String url;

    public OMEROAnnotationReferenceData() {
    }

    public OMEROAnnotationReferenceData(long annotationId) {
        this.annotationId = annotationId;
    }

    public OMEROAnnotationReferenceData(AnnotationData annotationData, OMEROCredentialsEnvironment environment) {
        this.annotationId = annotationData.getId();
        this.description = annotationData.getDescription();
        this.contentString = annotationData.getContentAsString();
        this.url = OMEROUtils.tryGetWebClientURL(environment.getWebclientUrl(), "annotation", annotationId);
    }

    public static OMEROAnnotationReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROAnnotationReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("project-id")
    public long getAnnotationId() {
        return annotationId;
    }

    @JsonSetter("project-id")
    public void setAnnotationId(long annotationId) {
        this.annotationId = annotationId;
    }

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

    @JsonGetter("content-string")
    public String getContentString() {
        return contentString;
    }

    @JsonSetter("content-string")
    public void setContentString(String contentString) {
        this.contentString = contentString;
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
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path jsonFile = storage.getFileSystemPath().resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new OMEROAnnotationReferenceData(annotationId);
    }

    @Override
    public String toString() {
        if (StringUtils.isNullOrEmpty(name)) {
            return "OMERO annotation ID=" + annotationId;
        } else {
            return "OMERO annotation '" + name + "' [ID=" + annotationId + "]";
        }
    }
}
