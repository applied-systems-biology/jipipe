package org.hkijena.acaq5;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import org.hkijena.acaq5.algorithms.enhancers.IlluminationCorrectionEnhancer;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
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
public class ACACCommand implements Command {
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
//        MISAImageJRegistryService.instantiate(pluginService);
//        SwingUtilities.invokeLater(() -> {
//            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
//            ToolTipManager.sharedInstance().setInitialDelay(1000);
//            MISAModuleRepositoryUI.getInstance(this).setVisible(true);
//        });
        ImagePlus img = IJ.openImage("/data/Glomeruli/misaxx_example_data_sliced/input/example_data_small/z0000.tif");
        IlluminationCorrectionEnhancer enhancer = new IlluminationCorrectionEnhancer();
        enhancer.getInputSlot().setData(new ACAQGreyscaleImageData(img));
        enhancer.run();
        enhancer.getOutputSlot().getData().getImage().show();
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
        ij.command().run(ACACCommand.class, true);
    }
}
