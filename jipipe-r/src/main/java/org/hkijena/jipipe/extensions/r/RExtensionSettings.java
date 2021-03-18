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

import com.github.rcaller.rstuff.FailurePolicy;
import com.github.rcaller.rstuff.RCallerOptions;
import com.github.rcaller.rstuff.RProcessStartUpOptions;
import com.github.rcaller.util.Globals;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;

public class RExtensionSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:r";

    private final EventBus eventBus = new EventBus();

    private OptionalPathParameter RExecutable = new OptionalPathParameter();
    private OptionalPathParameter RScriptExecutable = new OptionalPathParameter();

    public RExtensionSettings() {
        if (!StringUtils.isNullOrEmpty(Globals.R_current)) {
            try {
                RExecutable.setContent(Paths.get(Globals.R_current));
            } catch (Exception e) {
            }
        }
        if (!StringUtils.isNullOrEmpty(Globals.Rscript_current)) {
            try {
                RScriptExecutable.setContent(Paths.get(Globals.Rscript_current));
            } catch (Exception e) {
            }
        }
    }

    public static RExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, RExtensionSettings.class);
    }

    /**
     * Checks if the R settings are valid or throws an exception
     */
    public static void checkRSettings() {
        if(!RSettingsAreValid()) {
            throw new UserFriendlyRuntimeException("The R installation is invalid!\n" +
                    "R=" + RExtensionSettings.getInstance().getRExecutable() + "\n" +
                    "RScript=" + RExtensionSettings.getInstance().getRScriptExecutable(),
                    "R is not configured!",
                    "Project > Application settings > Extensions > R  integration",
                    "This node requires an installation of R. Either R is not installed or JIPipe cannot find R.",
                    "Please install R from https://www.r-project.org/. If R is installed, go to Project > Application settings > Extensions > R  integration and " +
                            "manually override R executable and RScript executable (please refer to the documentation in the settings page).");
        }
    }

    /**
     * Checks if the R settings are valid or reports an invalid state
     * @param report the report
     */
    public static void checkRSettings(JIPipeValidityReport report) {
        if(!RSettingsAreValid()) {
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
        String executable = Globals.R_current;
        String scriptExecutable = Globals.Rscript_current;
        if (JIPipe.getInstance() != null) {
            RExtensionSettings instance = getInstance();
            if (instance.RExecutable.isEnabled()) {
                executable = instance.RExecutable.getContent().toString();
            }
            if (instance.RScriptExecutable.isEnabled()) {
                scriptExecutable = instance.RScriptExecutable.getContent().toString();
            }
        }
        boolean invalid = false;
        if (StringUtils.isNullOrEmpty(executable) || !Files.exists(Paths.get(executable))) {
            invalid = true;
        }
        if (StringUtils.isNullOrEmpty(scriptExecutable) || !Files.exists(Paths.get(scriptExecutable))) {
            invalid = true;
        }
        return !invalid;
    }

    public static RCallerOptions createRCallerOptions() {
        String executable = Globals.R_current;
        String scriptExecutable = Globals.Rscript_current;
        FailurePolicy failurePolicy = FailurePolicy.RETRY_5;
        long maxWaitTime = Long.MAX_VALUE;
        long initialWaitTime = 100;
        if (JIPipe.getInstance() != null) {
            RExtensionSettings instance = getInstance();
            if (instance.RExecutable.isEnabled()) {
                executable = instance.RExecutable.getContent().toString();
            }
            if (instance.RScriptExecutable.isEnabled()) {
                scriptExecutable = instance.RScriptExecutable.getContent().toString();
            }
        }
        return RCallerOptions.create(scriptExecutable,
                executable,
                failurePolicy,
                maxWaitTime,
                initialWaitTime,
                RProcessStartUpOptions.create());
    }



    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Override R executable", description = "Allows to override the R executable. Must point to R.exe (Windows) or equivalent on other systems.")
    @JIPipeParameter("r-executable")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesOnly, ioMode = PathEditor.IOMode.Open)
    public OptionalPathParameter getRExecutable() {
        return RExecutable;
    }

    @JIPipeParameter("r-executable")
    public void setRExecutable(OptionalPathParameter RExecutable) {
        this.RExecutable = RExecutable;
    }

    @JIPipeDocumentation(name = "Override RScript executable", description = "Allows to override the RScript executable. Must point to RScript.exe (Windows) or equivalent on other systems.")
    @JIPipeParameter("rscript-executable")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesOnly, ioMode = PathEditor.IOMode.Open)
    public OptionalPathParameter getRScriptExecutable() {
        return RScriptExecutable;
    }

    @JIPipeParameter("rscript-executable")
    public void setRScriptExecutable(OptionalPathParameter RScriptExecutable) {
        this.RScriptExecutable = RScriptExecutable;
    }
}
