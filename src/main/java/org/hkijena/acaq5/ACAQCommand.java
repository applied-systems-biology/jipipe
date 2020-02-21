package org.hkijena.acaq5;

import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.ui.ACAQWorkbenchWindow;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import javax.swing.*;

@Plugin(type = Command.class, menuPath = "Plugins>ACAQ5")
public class ACAQCommand implements Command {
    @Parameter
    private OpService ops;

    @Parameter
    private LogService log;

    @Parameter
    private UIService ui;

    @Parameter
    private CommandService cmd;

    @Parameter
    private StatusService status;

    @Parameter
    private ThreadService thread;

    @Parameter
    private DatasetIOService datasetIO;

    @Parameter
    private DisplayService display;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private PluginService pluginService;

    @Override
    public void run() {
        ACAQRegistryService.instantiate(pluginService);
        SwingUtilities.invokeLater(() -> {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            ToolTipManager.sharedInstance().setInitialDelay(1000);
            ACAQWorkbenchWindow window = ACAQWorkbenchWindow.newWindow(this, new ACAQProject());
            window.setTitle("New project");
        });
    }

    public LogService getLogService() {
        return log;
    }

    public StatusService getStatusService() {
        return status;
    }

    public ThreadService getThreadService() {
        return thread;
    }

    public UIService getUiService() {
        return ui;
    }

    public DatasetIOService getDatasetIOService() {
        return datasetIO;
    }

    public DisplayService getDisplayService() {
        return display;
    }

    public DatasetService getDatasetService() {
        return datasetService;
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(ACAQCommand.class, true);
    }
}
