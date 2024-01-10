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
 *
 */

package org.hkijena.jipipe.extensions.omnipose.installers;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.EasyInstallExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.omnipose.OmniposeSettings;
import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironmentType;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JIPipeDocumentation(name = "Install Omnipose (EasyInstaller)", description = "Downloads a pre-packaged version of Omnipose")
@ExternalEnvironmentInfo(category = "Omnipose")
public class OmniposeEasyInstaller extends EasyInstallExternalEnvironmentInstaller<PythonEnvironment> {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public OmniposeEasyInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install Omnipose";
    }

    @Override
    public List<String> getRepositories() {
        return OmniposeSettings.getInstance().getEasyInstallerRepositories();
    }

    @Override
    public String getDialogHeading() {
        return "Install Omnipose";
    }

    @Override
    public HTMLText getDialogDescription() {
        return new HTMLText("Please select one of the pre-packaged versions of Omnipose. Please note that GPU acceleration is only supported by the GPU-capable packages. If you have issues, you can always try the version without CPU acceleration (CPU).");
    }

    @Override
    public HTMLText getFinishedMessage() {
        return new HTMLText("Omnipose was successfully installed. ");
    }

    @Override
    protected void writeEnvironmentToParameters(PythonEnvironment environment, JIPipeParameterAccess parameterAccess) {
        if (OptionalParameter.class.isAssignableFrom(parameterAccess.getFieldClass())) {
            parameterAccess.set(new OptionalPythonEnvironment(environment));
        } else {
            parameterAccess.set(environment);
        }
    }

    @Override
    protected void executePostprocess() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            getProgressInfo().log("Postprocess: Marking all files in " + getAbsolutePythonBinaryDir() + " as executable");
            try {
                Files.list(getAbsolutePythonBinaryDir()).forEach(path -> {
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
    protected PythonEnvironment generateEnvironment() {
        PythonEnvironment environment = new PythonEnvironment();
        environment.setType(PythonEnvironmentType.System);
        environment.setArguments(new JIPipeExpressionParameter("ARRAY(\"-u\", script_file)"));
        if (SystemUtils.IS_OS_WINDOWS) {
            environment.setExecutablePath(getRelativePythonBinaryDir().resolve("python.exe"));
        } else {
            environment.setExecutablePath(getRelativePythonBinaryDir().resolve("python3"));
        }
        environment.setName(getTargetPackage().getName());
        return environment;
    }

    private Path getRelativePythonBinaryDir() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return getRelativeInstallationPath().resolve("python");
        } else {
            return getRelativeInstallationPath().resolve("python").resolve("bin");
        }
    }

    private Path getAbsolutePythonBinaryDir() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return getAbsoluteInstallationPath().resolve("python");
        } else {
            return getAbsoluteInstallationPath().resolve("python").resolve("bin");
        }
    }
}
