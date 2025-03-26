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

package org.hkijena.jipipe;

import ij.IJ;
import net.imagej.ImageJ;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRunConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.compat.JIPipeDesktopRunSingleAlgorithmWindow;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopSplashScreen;
import org.hkijena.jipipe.plugins.settings.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;

/**
 * Runs a JIPipe algorithm on a single data set.
 */
@Plugin(type = Command.class, menuPath = "Plugins>JIPipe>Run JIPipe node", headless = true)
public class JIPipeRunAlgorithmCommand extends DynamicCommand implements Initializable {

    @Parameter(persist = false)
    private String nodeId = "";

    @Parameter(persist = false)
    private String parameters = "";

    @Parameter(persist = false)
    private String inputs = "";

    @Parameter(persist = false)
    private String outputs = "";

    @Parameter(persist = false)
    private int threads = 1;

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(JIPipeRunAlgorithmCommand.class, true);
    }

    @Override
    public void initialize() {
        resolveInput("nodeId");
        resolveInput("parameters");
        resolveInput("threads");
        resolveInput("inputs");
        resolveInput("outputs");
    }

    private void initializeRegistry(boolean withSplash) {
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        if (!JIPipe.isInstantiated()) {
            UIUtils.loadLookAndFeelFromSettings();
            if (!JIPipe.isInstantiated() && withSplash) {
                SwingUtilities.invokeLater(() -> JIPipeDesktopSplashScreen.getInstance().showSplash(getContext()));
            }
            JIPipe jiPipe = JIPipe.createInstance(getContext(), JIPipeMode.GUI);
            JIPipeDesktopSplashScreen.getInstance().setJIPipe(JIPipe.getInstance());
            jiPipe.initialize(extensionSettings, issues, true);
            SwingUtilities.invokeLater(() -> JIPipeDesktopSplashScreen.getInstance().hideSplash());
        }
        if (!extensionSettings.isSilent()) {
            JIPipeValidationReport report = new JIPipeValidationReport();
            issues.reportValidity(new UnspecifiedValidationReportContext(), report);
            if (!report.isValid()) {
                if (GraphicsEnvironment.isHeadless()) {
                    report.print();
                } else {
                    UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
                            null,
                            report,
                            "Errors while initializing JIPipe",
                            "There were some issues while initializing JIPipe. Please run the JIPipe GUI for more information.",
                            false);
                }
            }
        }
    }

    @Override
    public void run() {
        JIPipeGraphNode algorithm;
        SingleImageJAlgorithmRunConfiguration settings;
        if (StringUtils.isNullOrEmpty(nodeId) || StringUtils.isNullOrEmpty(parameters)) {
            UIUtils.loadLookAndFeelFromSettings();
            initializeRegistry(true);
            JIPipeDesktopRunSingleAlgorithmWindow dialog = new JIPipeDesktopRunSingleAlgorithmWindow(getContext());
            dialog.setTitle("Run JIPipe node");
            dialog.setIconImage(UIUtils.getJIPipeIcon128());
            dialog.pack();
            dialog.setSize(new Dimension(1024, 768));
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            return;
        } else {
            initializeRegistry(false);
            settings = new SingleImageJAlgorithmRunConfiguration(nodeId, parameters, inputs, outputs, threads);
            algorithm = settings.getAlgorithm();
            JIPipeValidationReport report = new JIPipeValidationReport();
            settings.reportValidity(new UnspecifiedValidationReportContext(), report);
            if (!report.isValid()) {
                StringBuilder message = new StringBuilder();
                message.append("The provided node options are invalid:\n\n");
                for (JIPipeValidationReportEntry entry : report) {
                    message.append(entry.toString()).append(", ");
                }
                cancel(message.toString());
                return;
            }
        }
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        progressInfo.getStatusUpdatedEventEmitter().subscribeLambda((emitter, event) -> {
            IJ.showStatus("[" + event.getProgress() + "/" + event.getMaxProgress() + "] " + event.getMessage());
        });
        IJ.showStatus("Running JIPipe node ...");
        IJ.showProgress(1, 3);

        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment projectCompartment = project.addCompartment("Default");
        project.getGraph().insertNode(algorithm, projectCompartment.getProjectCompartmentUUID());
        JIPipeGraphRunConfiguration runConfiguration = new JIPipeGraphRunConfiguration();
        runConfiguration.setOutputPath(project.newTemporaryDirectory());
        runConfiguration.setNumThreads(threads);
        runConfiguration.setLoadFromCache(false);
        runConfiguration.setStoreToCache(true);
        runConfiguration.setStoreToDisk(false);

        JIPipeGraphRun run = new JIPipeGraphRun(project, runConfiguration);
        JIPipeGraphNode runAlgorithm = run.getGraph().getNodeByUUID(algorithm.getUUIDInParentGraph());
        for (JIPipeInputDataSlot inputSlot : runAlgorithm.getInputSlots()) {
            inputSlot.setSkipDataGathering(true);
        }
        for (JIPipeOutputDataSlot outputSlot : runAlgorithm.getOutputSlots()) {
            outputSlot.setSkipGC(true);
        }
        settings.importInputsFromImageJ(runAlgorithm, progressInfo);
        IJ.showProgress(2, 3);

        run.getProgressInfo().setLogToStdOut(true);

        try {
            run.run();

            IJ.showProgress(3, 3);
            settings.exportOutputToImageJ(runAlgorithm, progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            project.getCache().clearAll(run.getProgressInfo());
            algorithm.clearSlotData(true, progressInfo);
            runAlgorithm.clearSlotData(true, progressInfo);
            PathUtils.deleteDirectoryRecursively(runConfiguration.getOutputPath(), run.getProgressInfo());
        }
    }
}
