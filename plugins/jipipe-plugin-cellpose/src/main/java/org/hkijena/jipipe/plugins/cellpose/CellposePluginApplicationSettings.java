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

package org.hkijena.jipipe.plugins.cellpose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CellposePluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:cellpose";

    private final PythonEnvironment standardEnvironment = new PythonEnvironment();
    private OptionalPythonEnvironment defaultEnvironment = new OptionalPythonEnvironment();

    public CellposePluginApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    private void preconfigureEnvironment(PythonEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("com.github.mouseland.cellpose:*"));
    }

    public PythonEnvironment getReadOnlyDefaultEnvironment() {
        if(defaultEnvironment.isEnabled()) {
            return new PythonEnvironment(defaultEnvironment.getContent());
        }
        else {
            return new PythonEnvironment(standardEnvironment);
        }
    }

    public static CellposePluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, CellposePluginApplicationSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            CellposePluginApplicationSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getReadOnlyDefaultEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
            return report.isValid();
        }
        return false;
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param context the validation context
     * @param report  the report
     */
    public static void checkPythonSettings(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!pythonSettingsAreValid()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "Cellpose is not configured!",
                    "This node requires an installation of Python with Cellpose. You have to point JIPipe to a Cellpose installation.",
                    "Please install Python from https://www.python.org/, or from https://www.anaconda.com/ or https://docs.conda.io/en/latest/miniconda.html and install Cellpose " +
                            "according to the documentation https://cellpose.readthedocs.io/en/latest/installation.html\n" +
                            "Then go to Project > Application settings > Extensions > Cellpose and choose the correct environment. " +
                            "Alternatively, the settings page will provide you with means to install Cellpose automatically."));
        }
    }

    @SetJIPipeDocumentation(name = "Default Cellpose environment", description = "The default Cellpose environment that is associated to newly created projects. " +
            "Leave at default (<code>com.github.mouseland.cellpose:*</code>) to automatically select the best available environment from an artifact. " +
            "If disabled, falls back to <code>com.github.mouseland.cellpose:*</code>.")
    @JIPipeParameter("default-cellpose-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Cellpose", allowArtifact = true, artifactFilters = {"com.github.mouseland.cellpose:*"})
    public OptionalPythonEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-cellpose-environment")
    public void setDefaultEnvironment(OptionalPythonEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
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
        return UIUtils.getIconFromResources("apps/cellpose.png");
    }

    @Override
    public String getName() {
        return "Cellpose";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Cellpose integration";
    }
}
