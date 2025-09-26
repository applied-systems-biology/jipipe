package org.hkijena.jipipe.plugins.r.setup;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.r.REnvironment;

import javax.swing.*;
import java.awt.*;

public class REnvironmentFromSystemSetupTool implements JIPipeEnvironmentSetupTool {
    @Override
    public boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment) {
        // TODO
        // Ideally try to find the existing R automatically and just offer the user if they accept that
        // Info: opening files or directories: use static methods in JIPipeDesktop.java
        return false;
    }

    @Override
    public String getName() {
        return "Import existing R (standalone/system-wide)";
    }

    @Override
    public String getDescription() {
        return "Configures this service connector to utilize an existing standalone or system-wide R.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("apps/rlogo_icon.png");
    }

    @Override
    public boolean accepts(Class<? extends JIPipeEnvironment> environmentClass) {
        return REnvironment.class.isAssignableFrom(environmentClass);
    }
}
