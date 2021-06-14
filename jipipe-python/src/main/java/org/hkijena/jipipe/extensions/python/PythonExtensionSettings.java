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

package org.hkijena.jipipe.extensions.python;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.List;

public class PythonExtensionSettings implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python";

    private final EventBus eventBus = new EventBus();
    private PythonEnvironment pythonEnvironment = new PythonEnvironment();
    private JIPipePythonAdapterLibraryEnvironment pythonAdapterLibraryEnvironment = new JIPipePythonAdapterLibraryEnvironment();
    private PythonEnvironment.List presets = new PythonEnvironment.List();
    private JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets = new JIPipePythonAdapterLibraryEnvironment.List();

    public PythonExtensionSettings() {
    }

    @JIPipeDocumentation(name = "Presets", description = "List of presets stored for Python environments.")
    @JIPipeParameter("presets")
    public PythonEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(PythonEnvironment.List presets) {
        this.presets = presets;
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
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

    @JIPipeDocumentation(name = "Python environment", description = "The Python environment that is utilized by the Python nodes. " +
            "Click the 'Select' button to select an existing environment or install a new Python.")
    @JIPipeParameter("python-environment")
    public PythonEnvironment getPythonEnvironment() {
        return pythonEnvironment;
    }

    @JIPipeParameter("python-environment")
    public void setPythonEnvironment(PythonEnvironment pythonEnvironment) {
        this.pythonEnvironment = pythonEnvironment;
    }

    @Override
    public List<ExternalEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        if (environmentClass == JIPipePythonAdapterLibraryEnvironment.class)
            return ImmutableList.copyOf(pythonAdapterPresets);
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<ExternalEnvironment> presets, Class<?> environmentClass) {
        if (environmentClass == JIPipePythonAdapterLibraryEnvironment.class) {
            this.pythonAdapterPresets.clear();
            for (ExternalEnvironment preset : presets) {
                this.pythonAdapterPresets.add((JIPipePythonAdapterLibraryEnvironment) preset);
            }
        } else {
            this.presets.clear();
            for (ExternalEnvironment preset : presets) {
                this.presets.add((PythonEnvironment) preset);
            }
        }
    }

    public static PythonExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, PythonExtensionSettings.class);
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param report the report
     */
    public static void checkPythonSettings(JIPipeValidityReport report) {
        if (!pythonSettingsAreValid()) {
            report.reportIsInvalid("Python is not configured!",
                    "Project > Application settings > Extensions > Python integration",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html. " +
                            "Then go to Project > Application settings > Extensions > Python integration and choose the environment. " +
                            "Alternatively, you can install a Conda environment from the settings page.");
        }
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            PythonExtensionSettings instance = getInstance();
            JIPipeValidityReport report = new JIPipeValidityReport();
            instance.getPythonEnvironment().reportValidity(report);
            return report.isValid();
        }
        return false;
    }
}
