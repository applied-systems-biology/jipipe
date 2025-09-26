package org.hkijena.jipipe.plugins.python.setup;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

import javax.swing.*;
import java.awt.*;

public class PythonEnvironmentFromSystemSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {

        // At this point we know it's a PythonEnvironment
        PythonEnvironment pythonEnvironment = (PythonEnvironment) environment;

        // TODO
        // Info: opening files or directories: use static methods in JIPipeDesktop.java

        // INFO: HERE IS THE LEGACY CODE FROM SOME OLDER VERSION:
//        public static PythonEnvironment generateEnvironment(Configuration configuration) {
//            PythonEnvironment generatedEnvironment = new PythonEnvironment();
//            generatedEnvironment.setType(PythonEnvironmentType.System);
//            generatedEnvironment.setArguments(new JIPipeExpressionParameter("ARRAY(\"-u\", script_file)"));
//            generatedEnvironment.setExecutablePath(configuration.getPythonExecutable());
//            generatedEnvironment.setName(configuration.getName());
//            return generatedEnvironment;
//        }

        return false;
    }

    @Override
    public String getName() {
        return "Import existing Python (standalone/system-wide)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing standalone or system-wide Python. " +
                "For Conda/VEnv use other tools!";
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
