package org.hkijena.jipipe.extensions.omnipose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;

public class OmniposeSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:omnipose";

    private OptionalPythonEnvironment overridePythonEnvironment = new OptionalPythonEnvironment();

    private StringList easyInstallerRepositories = new StringList();

    public OmniposeSettings() {
        overridePythonEnvironment.setEnabled(true);
        easyInstallerRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/easyinstall/easyinstall-omnipose.json");
    }

    public static OmniposeSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, OmniposeSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            OmniposeSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getPythonEnvironment().reportValidity(parentCause, report);
            return report.isValid();
        }
        return false;
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param report the report
     */
    public static void checkPythonSettings(JIPipeValidationReport report) {
        if (!pythonSettingsAreValid()) {
            report.reportIsInvalid("Python is not configured!",
                    "Project > Application settings > Extensions > Omnipose",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html and install Omnipose " +
                            "according to the documentation https://cellpose.readthedocs.io/en/latest/installation.html\n" +
                            "Then go to Project > Application settings > Extensions > Omnipose and choose the correct environment. " +
                            "Alternatively, the settings page will provide you with means to install Omnipose automatically.");
        }
    }

    @JIPipeDocumentation(name = "Omnipose Python environment", description = "If enabled, a separate Python environment is used for Omnipose. " +
            "Alternatively, the standard Python environment from the Python extension is used. Please ensure that Omnipose is installed. " +
            "You can also install Omnipose via the Select/Install button (CPU and GPU supported).")
    @JIPipeParameter("python-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose")
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
