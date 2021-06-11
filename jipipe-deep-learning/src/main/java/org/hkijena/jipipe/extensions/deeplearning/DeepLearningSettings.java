package org.hkijena.jipipe.extensions.deeplearning;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;

import java.util.List;

public class DeepLearningSettings implements JIPipeParameterCollection, ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:deep-learning";

    private final EventBus eventBus = new EventBus();

    private OptionalPythonEnvironment overridePythonEnvironment = new OptionalPythonEnvironment();
    private DeepLearningToolkitLibraryEnvironment.List deepLearningToolkitPresets = new DeepLearningToolkitLibraryEnvironment.List();
    private DeepLearningToolkitLibraryEnvironment deepLearningToolkit = new DeepLearningToolkitLibraryEnvironment();
    private DeepLearningDeviceEnvironment deepLearningDevice = new DeepLearningDeviceEnvironment();
    private DeepLearningDeviceEnvironment.List deepLearningDevicePresets = new DeepLearningDeviceEnvironment.List();

    public DeepLearningSettings() {
        overridePythonEnvironment.setEnabled(true);
    }

    public static DeepLearningSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DeepLearningSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            DeepLearningSettings instance = getInstance();
            JIPipeValidityReport report = new JIPipeValidityReport();
            instance.getPythonEnvironment().reportValidity(report);
            return report.isValid();
        }
        return false;
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param report the report
     */
    public static void checkPythonSettings(JIPipeValidityReport report) {
        if (!pythonSettingsAreValid()) {
            report.reportIsInvalid("Python is not configured!",
                    "Project > Application settings > Extensions > Deep Learning",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html and install the Deep Learning Toolkit " +
                            "according to the documentation https://www.jipipe.org/documentation/standard-library/deep-learning/\n" +
                            "Then go to Project > Application settings > Extensions > Deep Learning and choose the correct environment. " +
                            "Alternatively, the settings page will provide you with means to install the Deep Learning Toolkit automatically.");
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Deep Learning Python environment", description = "If enabled, a separate Python environment is used for Deep Learning. " +
            "Alternatively, the standard Python environment from the Python extension is used. Please ensure that the Deep Learning Toolkit is installed. " +
            "You can also install the Deep Learning Toolkit via the Select/Install button (CPU and GPU supported).")
    @JIPipeParameter("python-environment")
    public OptionalPythonEnvironment getOverridePythonEnvironment() {
        return overridePythonEnvironment;
    }

    @JIPipeParameter("python-environment")
    public void setOverridePythonEnvironment(OptionalPythonEnvironment overridePythonEnvironment) {
        this.overridePythonEnvironment = overridePythonEnvironment;
    }

    public PythonEnvironment getPythonEnvironment() {
        if (overridePythonEnvironment.isEnabled()) {
            return overridePythonEnvironment.getContent();
        } else {
            return PythonExtensionSettings.getInstance().getPythonEnvironment();
        }
    }

    @Override
    public List<ExternalEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        if(environmentClass == DeepLearningToolkitLibraryEnvironment.class)
            return ImmutableList.copyOf(deepLearningToolkitPresets);
        else if(environmentClass == DeepLearningDeviceEnvironment.class)
            return ImmutableList.copyOf(deepLearningDevicePresets);
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public void setPresetsListInterface(List<ExternalEnvironment> presets, Class<?> environmentClass) {
        if(environmentClass == DeepLearningToolkitLibraryEnvironment.class) {
            deepLearningToolkitPresets.clear();
            for (ExternalEnvironment preset : presets) {
                deepLearningToolkitPresets.add((DeepLearningToolkitLibraryEnvironment) preset);
            }
        }
        else if(environmentClass == DeepLearningDeviceEnvironment.class) {
            deepLearningDevicePresets.clear();
            for (ExternalEnvironment preset : presets) {
                deepLearningDevicePresets.add((DeepLearningDeviceEnvironment) preset);
            }
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    @JIPipeDocumentation(name = "Deep learning toolkit presets", description = "Presets for the Deep Learning Toolkit library")
    @JIPipeParameter("deep-learning-toolkit-presets")
    public DeepLearningToolkitLibraryEnvironment.List getDeepLearningToolkitPresets() {
        return deepLearningToolkitPresets;
    }

    @JIPipeParameter("deep-learning-toolkit-presets")
    public void setDeepLearningToolkitPresets(DeepLearningToolkitLibraryEnvironment.List deepLearningToolkitPresets) {
        this.deepLearningToolkitPresets = deepLearningToolkitPresets;
    }

    @JIPipeDocumentation(name = "Deep learning toolkit", description = "This environment allows you to setup how the JIPipe Deep Learning Toolkit library is supplied. " +
            "By default, JIPipe will automatically extract the library into the ImageJ folder and add code to include it. Alternatively, you can install the toolkit " +
            "into your Python environment and disable this feature.")
    @JIPipeParameter("deep-learning-toolkit")
    public DeepLearningToolkitLibraryEnvironment getDeepLearningToolkit() {
        return deepLearningToolkit;
    }

    @JIPipeParameter("deep-learning-toolkit")
    public void setDeepLearningToolkit(DeepLearningToolkitLibraryEnvironment deepLearningToolkit) {
        this.deepLearningToolkit = deepLearningToolkit;
    }

    @JIPipeDocumentation(name = "Devices", description = "Determines the default devices to be used for the processing")
    @JIPipeParameter("deep-learning-device")
    public DeepLearningDeviceEnvironment getDeepLearningDevice() {
        return deepLearningDevice;
    }

    @JIPipeParameter("deep-learning-device")
    public void setDeepLearningDevice(DeepLearningDeviceEnvironment deepLearningDevice) {
        this.deepLearningDevice = deepLearningDevice;
    }

    @JIPipeDocumentation(name = "Deep learning device presets", description = "Presets for different device configurations")
    @JIPipeParameter("deep-learning-device-presets")
    public DeepLearningDeviceEnvironment.List getDeepLearningDevicePresets() {
        return deepLearningDevicePresets;
    }

    @JIPipeParameter("deep-learning-device-presets")
    public void setDeepLearningDevicePresets(DeepLearningDeviceEnvironment.List deepLearningDevicePresets) {
        this.deepLearningDevicePresets = deepLearningDevicePresets;
    }
}
