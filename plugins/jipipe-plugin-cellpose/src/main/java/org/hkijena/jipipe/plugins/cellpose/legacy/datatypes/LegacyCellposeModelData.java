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
import java.util.List;

/**
 * Wrapper around Cellpose models
 */
@SetJIPipeDocumentation(name = "Cellpose model", description = "A Cellpose model")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A single file without extension that contains the Cellpose model",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/cellpose-model-data.schema.json")
@Deprecated
@LabelAsJIPipeHidden
public class LegacyCellposeModelData implements JIPipeData {

    private final byte[] data;
    private final String name;

    public LegacyCellposeModelData(Path file) {
        this.name = file.getFileName().toString();
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LegacyCellposeModelData(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public LegacyCellposeModelData(LegacyCellposeModelData other) {
        this.data = other.data;
        this.name = other.name;
    }

    public static LegacyCellposeModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        List<Path> files = PathUtils.findFilesByExtensionIn(storage.getFileSystemPath());
        Path file = null;

        for (Path path : files) {
            String name = path.getFileName().toString();
            // Skip dot files
            if (name.startsWith("."))
                continue;
            if (name.contains("cellpose")) {
                file = path;
                break;
            }
        }
        if (file == null)
            file = files.get(0);
        return new LegacyCellposeModelData(file);
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
            Files.write(storage.getFileSystemPath().resolve(name), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new LegacyCellposeModelData(this);
    }

    @Override
    public String toString() {
        return "Cellpose model: " + name + " (" + (data.length / 1024 / 1024) + " MB)";
    }
}
