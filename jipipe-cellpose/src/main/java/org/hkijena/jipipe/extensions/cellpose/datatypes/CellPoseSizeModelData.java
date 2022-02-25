package org.hkijena.jipipe.extensions.cellpose.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper around Cellpose models
 */
@JIPipeDocumentation(name = "Cellpose size model", description = "A Cellpose size model")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A single .npy file that contains the Cellpose size model",
jsonSchemaURL = "https://jipipe.org/schemas/datatypes/cellpose-size-model-data.schema.json")
public class CellPoseSizeModelData implements JIPipeData {

    final byte[] data;
    final String name;

    public CellPoseSizeModelData(Path file) {
        this.name = extractFileName(file);
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CellPoseSizeModelData(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public CellPoseSizeModelData(CellPoseSizeModelData other) {
        this.data = other.data;
        this.name = other.name;
    }

    public static CellPoseSizeModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new CellPoseSizeModelData(PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".npy"));
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
        return new CellPoseSizeModelData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JOptionPane.showMessageDialog(workbench.getWindow(), "Visualizing the model is currently not supported.",
                "Show Cellpose size model", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public String toString() {
        return "Cellpose size model: " + name + " (" + (data.length / 1024 / 1024) + " MB)";
    }
}
