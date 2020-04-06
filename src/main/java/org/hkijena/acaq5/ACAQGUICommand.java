package org.hkijena.acaq5;

import net.imagej.ImageJ;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.ui.ACAQProjectWindow;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;

/**
 * Command that runs the GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>ACAQ5>ACAQ5 GUI")
public class ACAQGUICommand implements Command {

    @Parameter
    private PluginService pluginService;

    @Parameter
    private Context context;

    @Override
    public void run() {
        ACAQDefaultRegistry.instantiate(context);
        SwingUtilities.invokeLater(() -> {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            ToolTipManager.sharedInstance().setInitialDelay(1000);
            ACAQProjectWindow window = ACAQProjectWindow.newWindow(this, new ACAQProject());
            window.setTitle("New project");
        });
    }

    /**
     * @return The context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(ACAQGUICommand.class, true);
    }
}
