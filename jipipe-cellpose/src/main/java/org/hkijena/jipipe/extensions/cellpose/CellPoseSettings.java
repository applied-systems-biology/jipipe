package org.hkijena.jipipe.extensions.cellpose;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;

public class CellPoseSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:cellpose";

    private final EventBus eventBus = new EventBus();

    private OptionalPythonEnvironment overridePythonEnvironment = new OptionalPythonEnvironment();

    private StringList easyInstallerRepositories = new StringList();

    public CellPoseSettings() {
        overridePythonEnvironment.setEnabled(true);
        easyInstallerRepositories.add("https://github.com/applied-systems-biology/jipipe/releases/download/current/easyinstall-cellpose.json");
    }

    public static CellPoseSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, CellPoseSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            CellPoseSettings instance = getInstance();
            JIPipeIssueReport report = new JIPipeIssueReport();
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
    public static void checkPythonSettings(JIPipeIssueReport report) {
        if (!pythonSettingsAreValid()) {
            report.reportIsInvalid("Python is not configured!",
                    "Project > Application settings > Extensions > Cellpose",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html and install Cellpose " +
                            "according to the documentation https://cellpose.readthedocs.io/en/latest/installation.html\n" +
                            "Then go to Project > Application settings > Extensions > Cellpose and choose the correct environment. " +
                            "Alternatively, the settings page will provide you with means to install Cellpose automatically.");
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Cellpose Python environment", description = "If enabled, a separate Python environment is used for CellPose. " +
            "Alternatively, the standard Python environment from the Python extension is used. Please ensure that CellPose is installed. " +
            "You can also install CellPose via the Select/Install button (CPU and GPU supported).")
    @JIPipeParameter("python-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Cellpose")
    public OptionalPythonEnvironment getOverridePythonEnvironment() {
        return overridePythonEnvironment;
    }

    @JIPipeParameter("python-environment")
    public void setOverridePythonEnvironment(OptionalPythonEnvironment overridePythonEnvironment) {
        this.overridePythonEnvironment = overridePythonEnvironment;
    }

    @JIPipeDocumentation(name = "Easy installer repositories", description = "Allows to change the repositories for the EasyInstaller")
    @JIPipeParameter("easy-installer-repositories")
    public StringList getEasyInstallerRepositories() {
        return easyInstallerRepositories;
    }

    @JIPipeParameter("easy-installer-repositories")
    public void setEasyInstallerRepositories(StringList easyInstallerRepositories) {
        this.easyInstallerRepositories = easyInstallerRepositories;
    }

    public PythonEnvironment getPythonEnvironment() {
        if (overridePythonEnvironment.isEnabled()) {
            return overridePythonEnvironment.getContent();
        } else {
            return PythonExtensionSettings.getInstance().getPythonEnvironment();
        }
    }
}
