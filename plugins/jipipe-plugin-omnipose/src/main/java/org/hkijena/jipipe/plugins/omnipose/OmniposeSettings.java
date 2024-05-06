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

package org.hkijena.jipipe.plugins.omnipose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonExtensionSettings;

public class OmniposeSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:omnipose";

    private PythonEnvironment pythonEnvironment = new PythonEnvironment();

    public OmniposeSettings() {
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
            instance.getPythonEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
            return report.isValid();
        }
        return false;
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param context the context
     * @param report  the report
     */
    public static void checkPythonSettings(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!pythonSettingsAreValid()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context, "Omnipose is not configured!",
                    "This node requires an installation of Python. You have to point JIPipe to a Python installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html and install Omnipose " +
                            "according to the documentation https://cellpose.readthedocs.io/en/latest/installation.html\n" +
                            "Then go to Project > Application settings > Extensions > Omnipose and choose the correct environment. " +
                            "Alternatively, the settings page will provide you with means to install Omnipose automatically."));
        }
    }

    @SetJIPipeDocumentation(name = "Omnipose Python environment", description = "If enabled, a separate Python environment is used for Omnipose. " +
            "Alternatively, the standard Python environment from the Python extension is used. Please ensure that Omnipose is installed. " +
            "You can also install Omnipose via the Select/Install button (CPU and GPU supported).")
    @JIPipeParameter("omnipose-python-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose", allowArtifact = true, artifactFilters = { "com.github.kevinjohncutler.omnipose.*" })
    public PythonEnvironment getPythonEnvironment() {
        return pythonEnvironment;
    }

    @JIPipeParameter("omnipose-python-environment")
    public void setPythonEnvironment(PythonEnvironment pythonEnvironment) {
        this.pythonEnvironment = pythonEnvironment;
    }
}
