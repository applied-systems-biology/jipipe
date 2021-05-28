package org.hkijena.jipipe.extensions.deeplearning;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;

public class DeepLearningSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:deep-learning";

    private final EventBus eventBus = new EventBus();

    private OptionalPythonEnvironment overridePythonEnvironment = new OptionalPythonEnvironment();

    public DeepLearningSettings() {
        overridePythonEnvironment.setEnabled(true);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static DeepLearningSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DeepLearningSettings.class);
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
        if(overridePythonEnvironment.isEnabled()) {
            return overridePythonEnvironment.getContent();
        }
        else {
            return PythonExtensionSettings.getInstance().getPythonEnvironment();
        }
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
}
