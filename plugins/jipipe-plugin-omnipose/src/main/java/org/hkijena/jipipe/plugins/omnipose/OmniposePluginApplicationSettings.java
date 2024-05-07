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
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

import javax.swing.*;

public class OmniposePluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:omnipose";

    private PythonEnvironment defaultOmniposeEnvironment = new PythonEnvironment();

    public OmniposePluginApplicationSettings() {
        defaultOmniposeEnvironment.setLoadFromArtifact(true);
        defaultOmniposeEnvironment.setArtifactQuery(new JIPipeArtifactQueryParameter("com.github.kevinjohncutler.omnipose:*"));
    }

    public static OmniposePluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, OmniposePluginApplicationSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            OmniposePluginApplicationSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getDefaultOmniposeEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
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

    @SetJIPipeDocumentation(name = "Omnipose Python environment", description = "The default Omnipose environment that is associated to newly created projects. " +
            "Leave at default (<code>com.github.kevinjohncutler.omnipose:*</code>) to automatically select the best available environment from an artifact.")
    @JIPipeParameter("default-omnipose-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose", allowArtifact = true, artifactFilters = { "com.github.kevinjohncutler.omnipose:*" })
    public PythonEnvironment getDefaultOmniposeEnvironment() {
        return defaultOmniposeEnvironment;
    }

    @JIPipeParameter("default-omnipose-environment")
    public void setDefaultOmniposeEnvironment(PythonEnvironment defaultOmniposeEnvironment) {
        this.defaultOmniposeEnvironment = defaultOmniposeEnvironment;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Plugins;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return OmniposePlugin.RESOURCES.getIconFromResources("omnipose.png");
    }

    @Override
    public String getName() {
        return "Omnipose";
    }

    @Override
    public String getDescription() {
        return "Connect existing Omnipose installations to JIPipe or automatically install a new Omnipose environment if none is available";
    }
}
