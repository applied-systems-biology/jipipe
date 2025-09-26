package org.hkijena.jipipe.plugins.python.setup;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

import javax.swing.*;
import java.awt.*;

public class PythonEnvironmentFromCondaSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {
        // TODO
        // Info: opening files or directories: use static methods in JIPipeDesktop.java
        // TODO: try to auto-detect conda if possible, then offer selection of conda environment (JOptionDialog). Otherwise user has to select the correct script.
        return false;
    }

    @Override
    public String getName() {
        return "Import existing Python (Conda)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing Conda environment.";
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
