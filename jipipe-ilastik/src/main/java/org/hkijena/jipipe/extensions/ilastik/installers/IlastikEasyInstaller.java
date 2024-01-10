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

package org.hkijena.jipipe.extensions.ilastik.installers;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.EasyInstallExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.ilastik.IlastikSettings;
import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.processes.OptionalProcessEnvironment;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JIPipeDocumentation(name = "Install Ilastik (EasyInstaller)", description = "Downloads a pre-packaged version of Ilastik")
@ExternalEnvironmentInfo(category = "Ilastik")
public class IlastikEasyInstaller extends EasyInstallExternalEnvironmentInstaller<ProcessEnvironment> {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public IlastikEasyInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install Ilastik";
    }

    @Override
    public List<String> getRepositories() {
        return IlastikSettings.getInstance().getEasyInstallerRepositories();
    }

    @Override
    public String getDialogHeading() {
        return "Install Ilastik";
    }

    @Override
    public HTMLText getDialogDescription() {
        return new HTMLText("Please select one of the pre-packaged versions of Ilastik.");
    }

    @Override
    public HTMLText getFinishedMessage() {
        return new HTMLText("Ilastik was successfully installed. ");
    }

    @Override
    protected void writeEnvironmentToParameters(ProcessEnvironment environment, JIPipeParameterAccess parameterAccess) {
        if (OptionalParameter.class.isAssignableFrom(parameterAccess.getFieldClass())) {
            parameterAccess.set(new OptionalProcessEnvironment(environment));
        } else {
            parameterAccess.set(environment);
        }
    }

    @Override
    protected void executePostprocess() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            Path binDir = getAbsoluteIlastikDir().resolve("bin");
            Path scriptPath = getAbsoluteIlastikDir().resolve("run_ilastik.sh");
            getProgressInfo().log("Postprocess: Marking runnable files in " + binDir + " as executable");
            try {
                Files.list(binDir).forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        getProgressInfo().log(" - chmod +x " + path);
                        PathUtils.makeUnixExecutable(path);
                    }
                });
                getProgressInfo().log(" - chmod +x " + scriptPath);
                PathUtils.makeUnixExecutable(scriptPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected ProcessEnvironment generateEnvironment() {
        ProcessEnvironment environment = new ProcessEnvironment();
        if (SystemUtils.IS_OS_WINDOWS) {
            environment.setExecutablePathWindows(getRelativeIlastikDir().resolve("ilastik.exe"));
            environment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
        } else if(SystemUtils.IS_OS_LINUX) {
            environment.setExecutablePathLinux(getRelativeIlastikDir().resolve("run_ilastik.sh"));
            environment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
        } else {
            environment.setExecutablePathOSX(getRelativeIlastikDir().resolve("run_ilastik.sh"));
            environment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
        }
        environment.setName(getTargetPackage().getName());
        return environment;
    }

    private Path getRelativeIlastikDir() {
        return getRelativeInstallationPath().resolve("ilastik");
    }

    private Path getAbsoluteIlastikDir() {
        return getAbsoluteInstallationPath().resolve("ilastik");
    }
}
