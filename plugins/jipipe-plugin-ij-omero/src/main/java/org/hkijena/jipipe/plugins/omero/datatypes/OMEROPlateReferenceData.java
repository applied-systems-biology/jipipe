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
import omero.gateway.model.PlateData;
import omero.gateway.model.ProjectData;
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
 * Data that stores a reference to an OMERO plate
 */
@SetJIPipeDocumentation(name = "OMERO Plate", description = "An OMERO plate ID")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file that stores the <pre>plate-id</pre> in a JSON object.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/omero-plate-reference-data.schema.json")
public class OMEROPlateReferenceData implements JIPipeData {
    private long plateId;
    private String name;
    private String url;

    public OMEROPlateReferenceData() {
    }

    public OMEROPlateReferenceData(long plateId) {
        this.plateId = plateId;
    }

    public OMEROPlateReferenceData(PlateData plateData, OMEROCredentialsEnvironment environment) {
        this.plateId = plateData.getId();
        this.name = plateData.getName();
        this.url = OMEROUtils.tryGetWebClientURL(environment.getWebclientUrl(), "plate", plateId);
    }

    public static OMEROPlateReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROPlateReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("plate-id")
    public long getPlateId() {
        return plateId;
    }

    @JsonSetter("plate-id")
    public void setPlateId(long plateId) {
        this.plateId = plateId;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
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
        return new OMEROPlateReferenceData(plateId);
    }

    @Override
    public String toString() {
        if (StringUtils.isNullOrEmpty(name)) {
            return "OMERO plate ID=" + plateId;
        } else {
            return "OMERO plate '" + name + "' [ID=" + plateId + "]";
        }
    }
}
