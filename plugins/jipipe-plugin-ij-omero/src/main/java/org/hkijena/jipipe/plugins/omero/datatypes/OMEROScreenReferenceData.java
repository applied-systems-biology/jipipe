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
import omero.gateway.model.ScreenData;
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
 * Data that stores a reference to an OMERO screen
 */
@SetJIPipeDocumentation(name = "OMERO Screen", description = "An OMERO screen ID")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file that stores the <pre>screen-id</pre> in a JSON object.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/omero-screen-reference-data.schema.json")
public class OMEROScreenReferenceData implements JIPipeData {
    private long screenId;
    private String name;
    private String description;
    private String url;
    private String protocolDescription;
    private String protocolIdentifier;
    private String reagentSetDescription;
    private String reagentSetIdentifier;

    public OMEROScreenReferenceData() {
    }

    public OMEROScreenReferenceData(long screenId) {
        this.screenId = screenId;
    }

    public OMEROScreenReferenceData(ScreenData screenData, OMEROCredentialsEnvironment environment) {
        this.screenId = screenData.getId();
        this.name = screenData.getName();
        this.description = screenData.getDescription();
        this.url = OMEROUtils.tryGetWebClientURL(environment.getWebclientUrl(), "screen", screenId);
        this.protocolDescription = screenData.getProtocolDescription();
        this.protocolIdentifier = screenData.getProtocolIdentifier();
        this.reagentSetDescription = screenData.getReagentSetDescripion();
        this.reagentSetIdentifier = screenData.getReagentSetIdentifier();
    }

    public static OMEROScreenReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROScreenReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("protocol-description")
    public String getProtocolDescription() {
        return protocolDescription;
    }

    @JsonSetter("protocol-description")
    public void setProtocolDescription(String protocolDescription) {
        this.protocolDescription = protocolDescription;
    }

    @JsonGetter("protocol-identifier")
    public String getProtocolIdentifier() {
        return protocolIdentifier;
    }

    @JsonSetter("protocol-identifier")
    public void setProtocolIdentifier(String protocolIdentifier) {
        this.protocolIdentifier = protocolIdentifier;
    }

    @JsonGetter("reagent-set-description")
    public String getReagentSetDescription() {
        return reagentSetDescription;
    }

    @JsonSetter("reagent-set-description")
    public void setReagentSetDescription(String reagentSetDescription) {
        this.reagentSetDescription = reagentSetDescription;
    }

    @JsonGetter("reagent-set-identifier")
    public String getReagentSetIdentifier() {
        return reagentSetIdentifier;
    }

    @JsonSetter("reagent-set-identifier")
    public void setReagentSetIdentifier(String reagentSetIdentifier) {
        this.reagentSetIdentifier = reagentSetIdentifier;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonGetter("screen-id")
    public long getScreenId() {
        return screenId;
    }

    @JsonSetter("screen-id")
    public void setScreenId(long screenId) {
        this.screenId = screenId;
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
        return new OMEROScreenReferenceData(screenId);
    }

    @Override
    public String toString() {
        if (StringUtils.isNullOrEmpty(name)) {
            return "OMERO screen ID=" + screenId;
        } else {
            return "OMERO screen '" + name + "' [ID=" + screenId + "]";
        }
    }
}
