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
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "OMERO Image", description = "An OMERO image ID")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file that stores the <pre>image-id</pre> in a JSON object.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/omero-image-reference-data.schema.json")
public class OMEROImageReferenceData implements JIPipeData {
    private long imageId;
    private String name;
    private String url;

    public OMEROImageReferenceData() {
    }

    public OMEROImageReferenceData(long imageId) {
        this.imageId = imageId;
    }

    public OMEROImageReferenceData(ImageData imageData, OMEROCredentialsEnvironment environment) {
        this.imageId = imageData.getId();
        this.name = imageData.getName();
        this.url = OMEROUtils.tryGetWebClientURL(environment.getWebclientUrl(), "image", imageId);
    }

    public static OMEROImageReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROImageReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("image-id")
    public long getImageId() {
        return imageId;
    }

    @JsonSetter("image-id")
    public void setImageId(long imageId) {
        this.imageId = imageId;
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
        return new OMEROImageReferenceData(imageId);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if(!StringUtils.isNullOrEmpty(url)) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            JOptionPane.showMessageDialog(workbench.getWindow(), "The OMERO image with ID=" + imageId + " is not associated to a webclient URL. " +
                            "Please configure the OMERO default credentials or 'Override OMERO credentials' with a URL to the webclient.",
                    "Display OMERO project",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public String toString() {
        if(StringUtils.isNullOrEmpty(name)) {
            return "OMERO image ID=" + imageId;
        }
        else {
            return "OMERO image '" + name + "' [ID=" + imageId + "]";
        }
    }
}
