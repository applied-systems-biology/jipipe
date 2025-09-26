package org.hkijena.jipipe.plugins.python.setup;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

import javax.swing.*;
import java.awt.*;

public class PythonEnvironmentFromVirtualEnvSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {

        // At this point we know it's a PythonEnvironment
        PythonEnvironment pythonEnvironment = (PythonEnvironment) environment;

        // TODO
        // Info: opening files or directories: use static methods in JIPipeDesktop.java

        // INFO: HERE IS SOME LEGACY CODE FROM SOME OLDER VERSION
//        Path selectedPath = configuration.virtualEnvDirectory;
//        generatedEnvironment = new PythonEnvironment();
//        generatedEnvironment.setType(PythonEnvironmentType.VirtualEnvironment);
//        if (SystemUtils.IS_OS_WINDOWS) {
//            generatedEnvironment.setExecutablePath(selectedPath.resolve("Scripts").resolve("python.exe"));
//            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
//                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.resolve("Scripts").toString()) + ";\"" + " + Path",
//                    "Path"
//            ));
//            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
//                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.toString()) + "\"",
//                    "VIRTUAL_ENV"
//            ));
//        } else {
//            generatedEnvironment.setExecutablePath(selectedPath.resolve("bin").resolve("python"));
//            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
//                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.resolve("bin").toString()) + ":\"" + " + PATH",
//                    "PATH"
//            ));
//            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
//                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.toString()) + "\"",
//                    "VIRTUAL_ENV"
//            ));
//        }
//
//        generatedEnvironment.setArguments(new JIPipeExpressionParameter("ARRAY(\"-u\", script_file)"));

        return false;
    }

    @Override
    public String getName() {
        return "Import existing Python (venv)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing Python virtual environment.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("apps/python.png");
    }

    @Override
    public boolean accepts(Class<? extends JIPipeEnvironment> environmentClass) {
        return PythonEnvironment.class.isAssignableFrom(environmentClass);
    }
}
