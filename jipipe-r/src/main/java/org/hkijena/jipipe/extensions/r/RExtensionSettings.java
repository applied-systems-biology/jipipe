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

package org.hkijena.jipipe.extensions.r;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.List;

public class RExtensionSettings implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:r";

    private final EventBus eventBus = new EventBus();

    private REnvironment environment = new REnvironment();
    private REnvironment.List presets = new REnvironment.List();

    public RExtensionSettings() {
    }

    public static RExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, RExtensionSettings.class);
    }

    /**
     * Checks if the R settings are valid or throws an exception
     */
    public static void checkRSettings() {
        if (!RSettingsAreValid()) {
            throw new UserFriendlyRuntimeException("The R installation is invalid!\n" +
                    "R=" + RExtensionSettings.getInstance().getEnvironment().getRExecutablePath() + "\n" +
                    "RScript=" + RExtensionSettings.getInstance().getEnvironment().getRScriptExecutablePath(),
                    "R is not configured!",
                    "Project > Application settings > Extensions > R  integration",
                    "This node requires an installation of R. Either R is not installed or JIPipe cannot find R.",
                    "Please install R from https://www.r-project.org/. If R is installed, go to Project > Application settings > Extensions > R  integration and " +
                            "manually override R executable and RScript executable (please refer to the documentation in the settings page).");
        }
    }

    /**
     * Checks if the R settings are valid or reports an invalid state
     *
     * @param report the report
     */
    public static void checkRSettings(JIPipeIssueReport report) {
        if (!RSettingsAreValid()) {
            report.reportIsInvalid("R is not configured!",
                    "Project > Application settings > Extensions > R  integration",
                    "This node requires an installation of R. Either R is not installed or JIPipe cannot find R.",
                    "Please install R from https://www.r-project.org/. If R is installed, go to Project > Application settings > Extensions > R  integration and " +
                            "manually override R executable and RScript executable (please refer to the documentation in the settings page).");
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
            JIPipeIssueReport report = new JIPipeIssueReport();
            instance.getEnvironment().reportValidity(report);
            return report.isValid();
        }
        return false;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "R environment", description = "Describes the R environment to use.")
    @JIPipeParameter("r-environment")
    public REnvironment getEnvironment() {
        return environment;
    }

    @JIPipeParameter("r-environment")
    public void setEnvironment(REnvironment environment) {
        this.environment = environment;
    }

    @JIPipeDocumentation(name = "Presets", description = "List of presets stored for R environments.")
    @JIPipeParameter("presets")
    public REnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(REnvironment.List presets) {
        this.presets = presets;
    }

    @Override
    public List<ExternalEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<ExternalEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (ExternalEnvironment preset : presets) {
            this.presets.add((REnvironment) preset);
        }
    }
}
