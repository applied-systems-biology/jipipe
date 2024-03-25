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

package org.hkijena.jipipe.plugins.r.installers;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.desktop.api.environments.JIPipeDesktopEasyInstallExternalEnvironmentInstaller;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.r.OptionalREnvironment;
import org.hkijena.jipipe.plugins.r.REnvironment;
import org.hkijena.jipipe.plugins.r.RExtensionSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SetJIPipeDocumentation(name = "Install R (EasyInstaller)", description = "Downloads a pre-packaged version of R")
public class REasyInstaller extends JIPipeDesktopEasyInstallExternalEnvironmentInstaller<REnvironment> {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public REasyInstaller(JIPipeDesktopWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install R";
    }

    @Override
    public List<String> getRepositories() {
        return RExtensionSettings.getInstance().getEasyInstallerRepositories();
    }

    @Override
    public String getDialogHeading() {
        return "Install R";
    }

    @Override
    public HTMLText getDialogDescription() {
        return new HTMLText("Here you can select one of the pre-packaged versions of R. " +
                "If you want more customization then go to <code>Project &gt; Application settings &gt; Extensions &gt; R</code> and select Edit or Select/Install.");
    }

    @Override
    public HTMLText getFinishedMessage() {
        return new HTMLText("R was successfully installed. You can close this window.");
    }

    @Override
    protected void writeEnvironmentToParameters(REnvironment environment, JIPipeParameterAccess parameterAccess) {
        if (OptionalParameter.class.isAssignableFrom(parameterAccess.getFieldClass())) {
            parameterAccess.set(new OptionalREnvironment(environment));
        } else {
            parameterAccess.set(environment);
        }
    }

    @Override
    protected void executePostprocess() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            getProgressInfo().log("Postprocess: Marking all files in " + getAbsoluteRBinaryDir() + " as executable");
            try {
                Files.list(getAbsoluteRBinaryDir()).forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        getProgressInfo().log(" - chmod +x " + path);
                        PathUtils.makeUnixExecutable(path);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected REnvironment generateEnvironment() {
        REnvironment environment = new REnvironment();
        environment.setArguments(new JIPipeExpressionParameter("ARRAY(script_file)"));
        if (SystemUtils.IS_OS_WINDOWS) {
            environment.setRExecutablePath(getRelativeRBinaryDir().resolve("R.exe"));
            environment.setRScriptExecutablePath(getRelativeRBinaryDir().resolve("Rscript.exe"));
        } else {
            environment.setRExecutablePath(getRelativeRBinaryDir().resolve("R"));
            environment.setRScriptExecutablePath(getRelativeRBinaryDir().resolve("Rscript"));
        }
        environment.setName(getTargetPackage().getName());
        return environment;
    }

    private Path getRelativeRBinaryDir() {
        return getRelativeInstallationPath().resolve("bin");
    }

    private Path getAbsoluteRBinaryDir() {
        return getAbsoluteInstallationPath().resolve("bin");
    }
}
