package org.hkijena.jipipe.extensions.cellpose;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.environments.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.environments.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;

public class CellPoseSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:cellpose";

    private final EventBus eventBus = new EventBus();

    private OptionalPythonEnvironment overridePythonEnvironment = new OptionalPythonEnvironment();

    public CellPoseSettings() {
        overridePythonEnvironment.setEnabled(true);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static CellPoseSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, CellPoseSettings.class);
    }

    @JIPipeDocumentation(name = "Cellpose Python environment", description = "If enabled, a separate Python environment is used for CellPose. " +
            "Alternatively, the standard Python environment from the Python extension is used. Please ensure that CellPose is installed. " +
            "You can also install CellPose via the Select/Install button (CPU and GPU supported).")
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
            CellPoseSettings instance = getInstance();
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
                    "Project > Application settings > Extensions > Cellpose",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html. " +
                            "If Python is installed, go to Project > Application settings > Extensions > Cellpose and " +
                            "set the Python executable. virtualenv is supported (you can find the exe in the environment bin folder).");
        }
    }
}
