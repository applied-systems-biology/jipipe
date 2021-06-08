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

package org.hkijena.jipipe.extensions.deeplearning.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningModelConfiguration;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Data that models a deep learning model
 */
@JIPipeDocumentation(name = "Deep learning model", description = "A Deep learning model")
@JIPipeDataStorageDocumentation("Two files: model.hdf5 stores the model in Keras/Tensorflow format. model-config.json stores the parameters of this model (dltoolbox format).")
public class DeepLearningModelData implements JIPipeData {

    private final byte[] modelData;
    private final DeepLearningModelConfiguration modelConfiguration;

    public DeepLearningModelData(Path modelPath, Path modelConfigPath) {
        try {
            modelData = Files.readAllBytes(modelPath);
            modelConfiguration = JsonUtils.getObjectMapper().readValue(modelConfigPath.toFile(), DeepLearningModelConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DeepLearningModelData(byte[] modelData, DeepLearningModelConfiguration modelConfiguration) {
        this.modelData = modelData;
        this.modelConfiguration = modelConfiguration;
    }

    public DeepLearningModelData(DeepLearningModelData other) {
        this.modelData = other.modelData;
        this.modelConfiguration = other.modelConfiguration;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path modelPath = forceName ? storageFilePath.resolve(name + ".hdf5") : storageFilePath.resolve("model.hdf5");
        Path modelConfigPath = forceName ? storageFilePath.resolve(name + ".json") : storageFilePath.resolve("model-config.json");
        try {
            Files.write(modelPath, modelData);
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(modelConfigPath.toFile(), modelConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getModelData() {
        return modelData;
    }

    public DeepLearningModelConfiguration getModelConfiguration() {
        return modelConfiguration;
    }

    @Override
    public JIPipeData duplicate() {
        return new DeepLearningModelData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JOptionPane.showMessageDialog(workbench.getWindow(), "Visualizing the model is currently not supported.",
                "Show Deep learning model", JOptionPane.ERROR_MESSAGE);
    }

    public static DeepLearningModelData importFrom(Path storagePath) {
        return new DeepLearningModelData(storagePath.resolve("model.hdf5"), storagePath.resolve("model-config.json"));
    }

    @Override
    public String toString() {
        return modelConfiguration.getArchitecture() +" model [" + modelConfiguration.getImageSize() + "x" + modelConfiguration.getImageSize() + " -> " + modelConfiguration.getNumClasses() + "-classification] " + " (" + (modelData.length / 1024 / 1024) + " MB)";
    }
}
