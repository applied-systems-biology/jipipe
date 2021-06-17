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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@JIPipeDataStorageDocumentation("Contains a single *.json file. The JSON data has following structure: " +
        "<pre>" +
        "{\n" +
        "    \"jipipe:data-type\": \"[Data type ID]\",\n" +
        "    \"path\": \"[The path]\"\n" +
        "    \"label-path\": \"[The label path]\"\n" +
        "}" +
        "</pre>")
@JIPipeDocumentation(name = "Labeled image file", description = "Data that references to an image file and a second image file that contains labels")
public class LabeledImageFileData extends FileData {

    private String labelPath;

    public LabeledImageFileData(Path path, Path labelPath) {
        super(path);
        this.labelPath = labelPath.toString();
    }

    public LabeledImageFileData(String path, String labelPath) {
        super(path);
        this.labelPath = labelPath;
    }

    @JsonGetter("label-path")
    public String getLabelPath() {
        return labelPath;
    }

    @JsonSetter("label-path")
    public void setLabelPath(String labelPath) {
        this.labelPath = labelPath;
    }

    public Path labelToPath() {
        return Paths.get(labelPath);
    }

    public static LabeledImageFileData importFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(LabeledImageFileData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
