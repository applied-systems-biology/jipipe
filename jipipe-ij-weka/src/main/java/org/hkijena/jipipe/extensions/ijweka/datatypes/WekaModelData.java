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
import org.hkijena.jipipe.utils.StringUtils;
import trainableSegmentation.WekaSegmentation;
import trainableSegmentation.Weka_Segmentation;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

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

    private final WekaSegmentation segmentation;

    public WekaModelData(WekaSegmentation segmentation) {
        this.segmentation = segmentation;
    }

    public WekaModelData(WekaModelData other) {
        this.segmentation = other.segmentation;
    }

    public static WekaModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        WekaSegmentation segmentation = new WekaSegmentation();
        segmentation.loadTrainingData(PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".arff").get(0).toString());
        segmentation.loadClassifier(PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".model").get(0).toString());
        return new WekaModelData(segmentation);
    }

    public WekaSegmentation getSegmentation() {
        return segmentation;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path modelOutputPath = storage.getFileSystemPath().resolve(StringUtils.orElse(name, "classifier") + ".model");
        Path dataOutputPath = storage.getFileSystemPath().resolve(StringUtils.orElse(name, "classifier") + ".arff");
        segmentation.saveClassifier(modelOutputPath.toString());
        segmentation.saveData(dataOutputPath.toString());
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new WekaModelData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
//        JOptionPane.showMessageDialog(workbench.getWindow(), "Visualizing the model is currently not supported.",
//                "Show Weka model", JOptionPane.ERROR_MESSAGE);
        final Instances data;
        if (segmentation.getTraceTrainingData() != null)
            data = segmentation.getTraceTrainingData();
        else
            data = segmentation.getLoadedTrainingData();
        if(data == null) {
            JOptionPane.showMessageDialog(workbench.getWindow(), "The model has no data to visualize!",
                "Show Weka model", JOptionPane.ERROR_MESSAGE);
        }
        Weka_Segmentation.displayGraphs(data, segmentation.getClassifier());
    }

    @Override
    public String toString() {
        return "Weka model: " + segmentation.getClassifier();
    }
}
