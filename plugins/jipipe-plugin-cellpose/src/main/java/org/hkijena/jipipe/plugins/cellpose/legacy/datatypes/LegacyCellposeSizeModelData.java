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

package org.hkijena.jipipe.plugins.cellpose.legacy.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper around Cellpose models
 */
@SetJIPipeDocumentation(name = "Cellpose size model", description = "A Cellpose size model")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A single .npy file that contains the Cellpose size model",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/cellpose-size-model-data.schema.json")
@Deprecated
@LabelAsJIPipeHidden
public class LegacyCellposeSizeModelData implements JIPipeData {

    final byte[] data;
    final String name;

    public LegacyCellposeSizeModelData(Path file) {
        this.name = extractFileName(file);
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LegacyCellposeSizeModelData(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public LegacyCellposeSizeModelData(LegacyCellposeSizeModelData other) {
        this.data = other.data;
        this.name = other.name;
    }

    public static LegacyCellposeSizeModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new LegacyCellposeSizeModelData(PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".npy"));
    }

    private String extractFileName(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".npy"))
            return name.substring(0, name.length() - 4);
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (!forceName)
            name = this.name;
        try {
            Files.write(storage.getFileSystemPath().resolve(name + ".npy"), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new LegacyCellposeSizeModelData(this);
    }

    @Override
    public String toString() {
        return "Cellpose size model: " + name + " (" + (data.length / 1024 / 1024) + " MB)";
    }
}
