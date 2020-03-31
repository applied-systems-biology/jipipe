package org.hkijena.acaq5;

import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.ui.ACAQProjectWindow;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

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
        ACAQDefaultRegistry.instantiate(pluginService);
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
