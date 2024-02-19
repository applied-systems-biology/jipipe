package org.hkijena.jipipe.extensions.ijweka.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import trainableSegmentation.WekaSegmentation;
import trainableSegmentation.Weka_Segmentation;
import weka.core.Instances;
import weka.gui.GenericObjectEditor;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Wrapper around Cellpose models
 */
@SetJIPipeDocumentation(name = "Weka model", description = "A model for the Trainable Weka Filter")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A file with *.json extension containing metadata. A *.model file that contains the classifier. " +
        "An optional *.arff file that contains the data used to train the model.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/weka-model-data.schema.json")
public class WekaModelData implements JIPipeData {

    private final WekaSegmentation segmentation;


    public WekaModelData(WekaSegmentation segmentation) {
        this.segmentation = segmentation;
    }

    public WekaModelData(WekaModelData other) {
        try {
            this.segmentation = (WekaSegmentation) GenericObjectEditor.makeCopy(other.segmentation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WekaModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        WekaSegmentation segmentation;

        Metadata metadata = JsonUtils.readFromFile(PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".json").get(0), Metadata.class);

        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            segmentation = new WekaSegmentation(metadata.isProcessing3D());
            segmentation.loadClassifier(PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".model").get(0).toString());
            segmentation.loadTrainingData(PathUtils.findFilesByExtensionIn(storage.getFileSystemPath(), ".arff").get(0).toString());
        }

        return new WekaModelData(segmentation);
    }

    public WekaSegmentation getSegmentation() {
        return segmentation;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path modelOutputPath = storage.getFileSystemPath().resolve(StringUtils.orElse(name, "classifier") + ".model");
        Path dataOutputPath = storage.getFileSystemPath().resolve(StringUtils.orElse(name, "data") + ".arff");
        Path metadataOutputPath = storage.getFileSystemPath().resolve(StringUtils.orElse(name, "metadata") + ".json");
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            segmentation.saveClassifier(modelOutputPath.toString());
            segmentation.saveData(dataOutputPath.toString());
        }
        JsonUtils.saveToFile(new Metadata(segmentation), metadataOutputPath);
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
        if (data == null) {
            JOptionPane.showMessageDialog(workbench.getWindow(), "The model has no data to visualize!",
                    "Show Weka model", JOptionPane.ERROR_MESSAGE);
        }
        Weka_Segmentation.displayGraphs(data, segmentation.getClassifier());
    }

    @Override
    public String toString() {
        return (segmentation.isProcessing3D() ? "3D" : "2D") + " Weka model: " + segmentation.getClassifier();
    }

    /**
     * Additional metada required for the correct deserialization of the model
     */
    public static class Metadata {
        private boolean processing3D;

        public Metadata() {
        }

        public Metadata(WekaSegmentation segmentation) {
            this.processing3D = segmentation.isProcessing3D();
        }

        @JsonGetter("is-processing-3d")
        public boolean isProcessing3D() {
            return processing3D;
        }

        @JsonSetter("is-processing-3d")
        public void setProcessing3D(boolean processing3D) {
            this.processing3D = processing3D;
        }
    }
}
