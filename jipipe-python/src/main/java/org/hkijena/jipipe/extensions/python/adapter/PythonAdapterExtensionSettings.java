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

package org.hkijena.jipipe.extensions.python.adapter;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;

import java.util.List;

public class PythonAdapterExtensionSettings extends AbstractJIPipeParameterCollection implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python-adapter";
    private JIPipePythonAdapterLibraryEnvironment pythonAdapterLibraryEnvironment = new JIPipePythonAdapterLibraryEnvironment();
    private JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets = new JIPipePythonAdapterLibraryEnvironment.List();
    private StringList easyInstallerRepositories = new StringList();

    public PythonAdapterExtensionSettings() {
        easyInstallerRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/easyinstall/easyinstall-lib-jipipe-python.json");
    }

    public static PythonAdapterExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, PythonAdapterExtensionSettings.class);
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param report the report
     */
    public static void checkPythonAdapterSettings(JIPipeIssueReport report) {
        if (!pythonSettingsAreValid()) {
            report.reportIsInvalid("Python adapter is not configured!",
                    "Project > Application settings > Extensions > Python integration (adapter)",
                    "This node requires an installation of the JIPipe Python adapter. Please follow the instructions to download and install the Python adapter.",
                    "Go to Project > Application settings > Extensions > Python integration (adapter) and install the environment from ");
        }
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            PythonAdapterExtensionSettings instance = getInstance();
            JIPipeIssueReport report = new JIPipeIssueReport();
            instance.getPythonAdapterLibraryEnvironment().reportValidity(report);
            return report.isValid();
        }
        return false;
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

    @JIPipeDocumentation(name = "Python adapter presets", description = "List of presets stored for JIPipe Python adapters")
    @JIPipeParameter("python-adapter-presets")
    public JIPipePythonAdapterLibraryEnvironment.List getPythonAdapterPresets() {
        return pythonAdapterPresets;
    }

    @JIPipeParameter("python-adapter-presets")
    public void setPythonAdapterPresets(JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets) {
        this.pythonAdapterPresets = pythonAdapterPresets;
    }

    @JIPipeDocumentation(name = "JIPipe Python adapter", description = "This environment allows you to setup how the JIPipe Python adapter library is supplied. " +
            "By default, JIPipe will automatically extract the adapter into the ImageJ folder and add code to include it. Alternatively, you can install the Python adapter " +
            "into your Python environment and disable this feature.")
    @JIPipeParameter("python-adapter-library")
    public JIPipePythonAdapterLibraryEnvironment getPythonAdapterLibraryEnvironment() {
        return pythonAdapterLibraryEnvironment;
    }

    @JIPipeParameter("python-adapter-library")
    public void setPythonAdapterLibraryEnvironment(JIPipePythonAdapterLibraryEnvironment pythonAdapterLibraryEnvironment) {
        this.pythonAdapterLibraryEnvironment = pythonAdapterLibraryEnvironment;
    }

    @Override
    public List<ExternalEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(pythonAdapterPresets);
    }

    @Override
    public void setPresetsListInterface(List<ExternalEnvironment> presets, Class<?> environmentClass) {
        this.pythonAdapterPresets.clear();
        for (ExternalEnvironment preset : presets) {
            this.pythonAdapterPresets.add((JIPipePythonAdapterLibraryEnvironment) preset);
        }
    }
}
