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

package org.hkijena.jipipe.extensions.deeplearning.nodes;

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningSettings;
import org.hkijena.jipipe.extensions.deeplearning.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningModelConfiguration;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Create Model", description = "Creates a new Deep Learning model")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = DeepLearningModelData.class, slotName = "Output", autoCreate = true)
public class CreateModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DeepLearningModelConfiguration modelConfiguration = new DeepLearningModelConfiguration();
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private OptionalDeepLearningDeviceEnvironment overrideDevices = new OptionalDeepLearningDeviceEnvironment();
    private boolean cleanUpAfterwards = true;

    public CreateModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(modelConfiguration);
    }

    public CreateModelAlgorithm(CreateModelAlgorithm other) {
        super(other);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.modelConfiguration = new DeepLearningModelConfiguration(other.modelConfiguration);
        this.overrideDevices = new OptionalDeepLearningDeviceEnvironment(other.overrideDevices);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        registerSubParameter(modelConfiguration);
    }

    @JIPipeDocumentation(name = "Override device configuration", description = "If enabled, this nodes provides a custom device configuration, " +
            "different to the one inside the application settings")
    @JIPipeParameter("override-devices")
    public OptionalDeepLearningDeviceEnvironment getOverrideDevices() {
        return overrideDevices;
    }

    @JIPipeParameter("override-devices")
    public void setOverrideDevices(OptionalDeepLearningDeviceEnvironment overrideDevices) {
        this.overrideDevices = overrideDevices;
    }

    @JIPipeDocumentation(name = "Model", description = "Use following settings to change the properties of the generated model")
    @JIPipeParameter(value = "model",
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/dl-model.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/dl-model.png")
    public DeepLearningModelConfiguration getModelConfiguration() {
        return modelConfiguration;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path workDirectory = RuntimeSettings.generateTempDirectory("dltoolbox");
        DeepLearningModelConfiguration modelConfiguration = new DeepLearningModelConfiguration(this.modelConfiguration);
        Path modelConfigurationPath = workDirectory.resolve("model-configuration.json");
        Path modelPath = workDirectory.resolve("model.hdf5");
        Path modelJsonPath = workDirectory.resolve("model.json");
        Path deviceConfigurationPath = workDirectory.resolve("device-configuration.json");

        // Configure
        modelConfiguration.setOutputModelPath(modelPath);
        modelConfiguration.setOutputModelJsonPath(modelJsonPath);

        // Save the config
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(modelConfigurationPath.toFile(), modelConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (getOverrideDevices().isEnabled())
            getOverrideDevices().getContent().saveAsJson(deviceConfigurationPath);
        else
            DeepLearningSettings.getInstance().getDeepLearningDevice().saveAsJson(deviceConfigurationPath);

        // Build arguments and run
        List<String> arguments = new ArrayList<>();
        arguments.add("-m");
        arguments.add("dltoolbox");
        arguments.add("--operation");
        arguments.add("create-model");
        arguments.add("--config");
        arguments.add(modelConfigurationPath.toString());
        arguments.add("--device-config");
        arguments.add(deviceConfigurationPath.toString());

        if (DeepLearningSettings.getInstance().getDeepLearningToolkit().needsInstall())
            DeepLearningSettings.getInstance().getDeepLearningToolkit().install(progressInfo);
        PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                        DeepLearningSettings.getInstance().getPythonEnvironment(),
                Collections.singletonList(DeepLearningSettings.getInstance().getDeepLearningToolkit().getLibraryDirectory().toAbsolutePath()), progressInfo);

        DeepLearningModelData modelData = new DeepLearningModelData(modelPath, modelConfigurationPath, modelJsonPath);
        dataBatch.addOutputData(getFirstOutputSlot(), modelData, progressInfo);

        if (cleanUpAfterwards) {
            try {
                FileUtils.deleteDirectory(workDirectory.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Deep Learning is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @JIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }
}
