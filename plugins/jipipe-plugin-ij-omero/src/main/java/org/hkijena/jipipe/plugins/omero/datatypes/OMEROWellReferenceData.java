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
import omero.gateway.model.ProjectData;
import omero.gateway.model.WellData;
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
 * Data that stores a reference to an OMERO well
 */
@SetJIPipeDocumentation(name = "OMERO Well", description = "An OMERO well ID")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file that stores the <pre>well-id</pre> in a JSON object.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/omero-well-reference-data.schema.json")
public class OMEROWellReferenceData implements JIPipeData {
    private long wellId;
    private String name;
    private String url;
    private String wellType;
    private String status;
    private int row;
    private int column;
    private int red;
    private int green;
    private int blue;
    private int alpha;

    public OMEROWellReferenceData() {
    }

    public OMEROWellReferenceData(long wellId) {
        this.wellId = wellId;
    }

    public OMEROWellReferenceData(WellData wellData, OMEROCredentialsEnvironment environment) {
        this.wellId = wellData.getId();
        this.wellType = wellData.getWellType();
        this.status = wellData.getStatus();
        this.row = wellData.getRow() != null ? wellData.getRow() : -1;
        this.column = wellData.getColumn() != null ? wellData.getColumn() : -1;
        this.red = wellData.getRed();
        this.green = wellData.getGreen();
        this.blue = wellData.getBlue();
        this.alpha = wellData.getAlpha();
        this.url = OMEROUtils.tryGetWebClientURL(environment.getWebclientUrl(), "well", wellId);
    }

    public static OMEROWellReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROWellReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("color-alpha")
    public int getAlpha() {
        return alpha;
    }

    @JsonSetter("color-alpha")
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @JsonGetter("color-blue")
    public int getBlue() {
        return blue;
    }

    @JsonSetter("color-blue")
    public void setBlue(int blue) {
        this.blue = blue;
    }

    @JsonGetter("column")
    public int getColumn() {
        return column;
    }

    @JsonSetter("column")
    public void setColumn(int column) {
        this.column = column;
    }

    @JsonGetter("color-green")
    public int getGreen() {
        return green;
    }

    @JsonSetter("color-green")
    public void setGreen(int green) {
        this.green = green;
    }

    @JsonGetter("color-red")
    public int getRed() {
        return red;
    }

    @JsonSetter("color-red")
    public void setRed(int red) {
        this.red = red;
    }

    @JsonGetter("row")
    public int getRow() {
        return row;
    }

    @JsonSetter("row")
    public void setRow(int row) {
        this.row = row;
    }

    @JsonGetter("status")
    public String getStatus() {
        return status;
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonGetter("well-type")
    public String getWellType() {
        return wellType;
    }

    @JsonSetter("well-type")
    public void setWellType(String wellType) {
        this.wellType = wellType;
    }

    @JsonGetter("well-id")
    public long getWellId() {
        return wellId;
    }

    @JsonSetter("well-id")
    public void setWellId(long wellId) {
        this.wellId = wellId;
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
        return new OMEROWellReferenceData(wellId);
    }

    @Override
    public String toString() {
        if (StringUtils.isNullOrEmpty(name)) {
            return "OMERO well ID=" + wellId;
        } else {
            return "OMERO well '" + name + "' [ID=" + wellId + "]";
        }
    }
}
