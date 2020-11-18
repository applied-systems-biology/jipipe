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
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

@JIPipeDocumentation(name = "OMERO Image", description = "An OMERO image ID")
public class OMEROImageReferenceData implements JIPipeData {
    private long imageId;

    public OMEROImageReferenceData(long imageId) {
        this.imageId = imageId;
    }

    @JsonGetter("image-id")
    public long getImageId() {
        return imageId;
    }

    @JsonSetter("image-id")
    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progress) {
        Path jsonFile = storageFilePath.resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new OMEROImageReferenceData(imageId);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return "OMERO Image ID=" + imageId;
    }

    public static OMEROImageReferenceData importFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMEROImageReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
