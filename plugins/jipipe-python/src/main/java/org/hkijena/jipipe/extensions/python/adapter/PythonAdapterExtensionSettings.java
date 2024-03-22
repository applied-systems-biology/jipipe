/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.python.adapter;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;

import java.util.List;

public class PythonAdapterExtensionSettings extends AbstractJIPipeParameterCollection implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python-adapter";
    private JIPipePythonAdapterLibraryEnvironment pythonAdapterLibraryEnvironment = new JIPipePythonAdapterLibraryEnvironment();
    private JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets = new JIPipePythonAdapterLibraryEnvironment.List();
    private StringList easyInstallerRepositories = new StringList();
    private boolean checkForUpdates = true;

    public PythonAdapterExtensionSettings() {
        easyInstallerRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/easyinstall/easyinstall-lib-jipipe-python.json");
    }

    public static PythonAdapterExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, PythonAdapterExtensionSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            PythonAdapterExtensionSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getPythonAdapterLibraryEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
            return report.isValid();
        }
        return false;
    }

    @SetJIPipeDocumentation(name = "Easy installer repositories", description = "Allows to change the repositories for the EasyInstaller")
    @JIPipeParameter("easy-installer-repositories")
    public StringList getEasyInstallerRepositories() {
        return easyInstallerRepositories;
    }

    @JIPipeParameter("easy-installer-repositories")
    public void setEasyInstallerRepositories(StringList easyInstallerRepositories) {
        this.easyInstallerRepositories = easyInstallerRepositories;
    }

    @SetJIPipeDocumentation(name = "Python adapter presets", description = "List of presets stored for JIPipe Python adapters")
    @JIPipeParameter("python-adapter-presets")
    public JIPipePythonAdapterLibraryEnvironment.List getPythonAdapterPresets() {
        return pythonAdapterPresets;
    }

    @JIPipeParameter("python-adapter-presets")
    public void setPythonAdapterPresets(JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets) {
        this.pythonAdapterPresets = pythonAdapterPresets;
    }

    @SetJIPipeDocumentation(name = "JIPipe Python adapter", description = "This environment allows you to setup how the JIPipe Python adapter library is supplied. " +
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

    @SetJIPipeDocumentation(name = "Automatically check for updates", description = "If enabled, automatically check for updates of the adapter library when JIPipe is started")
    @JIPipeParameter("check-for-updates")
    public boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    @JIPipeParameter("check-for-updates")
    public void setCheckForUpdates(boolean checkForUpdates) {
        this.checkForUpdates = checkForUpdates;
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(pythonAdapterPresets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.pythonAdapterPresets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.pythonAdapterPresets.add((JIPipePythonAdapterLibraryEnvironment) preset);
        }
    }
}
