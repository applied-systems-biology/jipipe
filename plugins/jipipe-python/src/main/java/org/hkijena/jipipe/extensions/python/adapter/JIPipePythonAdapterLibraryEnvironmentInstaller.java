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

package org.hkijena.jipipe.extensions.python.adapter;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.desktop.api.environments.JIPipeDesktopEasyInstallExternalEnvironmentInstaller;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@SetJIPipeDocumentation(name = "Install Python adapter (EasyInstall)", description = "Installs the JIPipe-Python library from a remote repository")
public class JIPipePythonAdapterLibraryEnvironmentInstaller extends JIPipeDesktopEasyInstallExternalEnvironmentInstaller<JIPipePythonAdapterLibraryEnvironment> {


    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public JIPipePythonAdapterLibraryEnvironmentInstaller(JIPipeDesktopWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install Python adapter";
    }

    @Override
    protected void writeEnvironmentToParameters(JIPipePythonAdapterLibraryEnvironment environment, JIPipeParameterAccess parameterAccess) {
        if (OptionalParameter.class.isAssignableFrom(parameterAccess.getFieldClass())) {
            parameterAccess.set(new OptionalJIPipePythonAdapterLibraryEnvironment(environment));
        } else {
            parameterAccess.set(environment);
        }
    }

    @Override
    protected JIPipePythonAdapterLibraryEnvironment generateEnvironment() {
        JIPipePythonAdapterLibraryEnvironment environment = new JIPipePythonAdapterLibraryEnvironment();
        environment.setProvidedByEnvironment(false);
        Path[] libDir = new Path[1];
        try (Stream<Path> stream = Files.list(getAbsoluteInstallationPath())) {
            stream.forEach(path -> {
                if (Files.isDirectory(path)) {
                    libDir[0] = path;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getProgressInfo().log("Detected library: " + libDir[0]);
        Path relativeLibDir = PathUtils.getJIPipeUserDir().relativize(libDir[0]);
        environment.setLibraryDirectory(relativeLibDir);
        return environment;
    }

    @Override
    public List<String> getRepositories() {
        return PythonAdapterExtensionSettings.getInstance().getEasyInstallerRepositories();
    }

    @Override
    public String getDialogHeading() {
        return "Install JIPipe Python adapter";
    }

    @Override
    public HTMLText getDialogDescription() {
        return new HTMLText("Please choose the Python adapter library version that should be installed.");
    }

    @Override
    public HTMLText getFinishedMessage() {
        return new HTMLText("The Python adapter is now ready. You can close this message.");
    }
}
