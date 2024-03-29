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

package org.hkijena.jipipe.extensions.r;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;

import java.util.List;

public class RExtensionSettings extends AbstractJIPipeParameterCollection implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:r";
    private REnvironment environment = new REnvironment();
    private REnvironment.List presets = new REnvironment.List();

    private StringList easyInstallerRepositories = new StringList();

    public RExtensionSettings() {
        easyInstallerRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/easyinstall/easyinstall-r.json");
    }

    public static RExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, RExtensionSettings.class);

    }

    /**
     * Checks if the R settings are valid or reports an invalid state
     *
     * @param context the context
     * @param report  the report
     */
    public static void checkRSettings(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!RSettingsAreValid()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context, "R is not configured!",
                    "This node requires an installation of R. Either R is not installed or JIPipe cannot find R.",
                    "Please install R from https://www.r-project.org/. If R is installed, go to Project > Application settings > Extensions > R  integration and " +
                            "manually override R executable and RScript executable (please refer to the documentation in the settings page)."));
        }
    }

    /**
     * Checks the R settings
     *
     * @return if the settings are correct
     */
    public static boolean RSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            RExtensionSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
            return report.isValid();
        }
        return false;
    }

    @SetJIPipeDocumentation(name = "R environment", description = "Describes the R environment to use.")
    @JIPipeParameter("r-environment")
    public REnvironment getEnvironment() {
        return environment;
    }

    @JIPipeParameter("r-environment")
    public void setEnvironment(REnvironment environment) {
        this.environment = environment;
    }

    @SetJIPipeDocumentation(name = "Presets", description = "List of presets stored for R environments.")
    @JIPipeParameter("presets")
    public REnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(REnvironment.List presets) {
        this.presets = presets;
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

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((REnvironment) preset);
        }
    }
}
