package org.hkijena.jipipe.extensions.ijweka.datatypes;

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
import java.util.List;

/**
 * Wrapper around Cellpose models
 */
@JIPipeDocumentation(name = "Weka model", description = "A model for the Trainable Weka Filter")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A single file with the *.model extension",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/weka-model-data.schema.json")
public class WekaModelData implements JIPipeData {

    private final byte[] data;
    private final String name;

    public WekaModelData(Path file) {
        this.name = file.getFileName().toString();
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public WekaModelData(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public WekaModelData(WekaModelData other) {
        this.data = other.data;
        this.name = other.name;
    }

    public static WekaModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        List<Path> files = PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".model");
        return new WekaModelData(files.get(0));
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
        return new WekaModelData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JOptionPane.showMessageDialog(workbench.getWindow(), "Visualizing the model is currently not supported.",
                "Show Weka model", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public String toString() {
        return "Weka model: " + name + " (" + (data.length / 1024 / 1024) + " MB)";
    }
}
