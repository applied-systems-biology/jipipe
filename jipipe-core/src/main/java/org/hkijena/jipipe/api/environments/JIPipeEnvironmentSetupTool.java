package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import javax.swing.*;
import java.awt.*;

/**
 * A user-facing configuration tool that allows to quickly set up a {@link JIPipeEnvironment}.
 * Executed within the Swing dispatcher thread!
 */
public interface JIPipeEnvironmentSetupTool {
    /**
     * Applies the configuration for the given environment
     *
     * @param workbench   the workbench
     * @param parent      the parent component
     * @param environment the environment to configure
     * @return if the configuration was successful
     */
    boolean configure(JIPipeDesktopWorkbench workbench, Component parent, JIPipeEnvironment environment);

    /**
     * Returns the name of this tool within the UI
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the description of this tool
     *
     * @return the description
     */
    String getDescription();

    /**
     * Returns the icon of this tool
     *
     * @return the icon
     */
    Icon getIcon();

    /**
     * Determines whether this tool is shown to the user for a given environment class
     *
     * @param environmentClass the environment class
     * @return if the tool can be executed on an environment of a given class
     */
    boolean accepts(Class<? extends JIPipeEnvironment> environmentClass);
}
