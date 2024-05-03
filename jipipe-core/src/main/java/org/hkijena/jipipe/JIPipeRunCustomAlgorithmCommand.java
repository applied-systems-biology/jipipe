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
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRunConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.compat.JIPipeDesktopRunSingleAlgorithmWindow;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopSplashScreen;
import org.hkijena.jipipe.plugins.settings.ExtensionSettings;
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
 * This {@link Command} allows to run a specified {@link JIPipeNodeInfo} from within ImageJ.
 * Inherit from this class and specify the {@link JIPipeNodeInfo} in the constructor. Add a {@link Plugin}
 * annotation to make the command discoverable from within ImageJ.
 */
public abstract class JIPipeRunCustomAlgorithmCommand extends DynamicCommand implements Initializable {

    private final String nodeId;
    private final String windowTitle;

    @Parameter(persist = false)
    private String parameters = "";

    @Parameter(persist = false)
    private String inputs = "";

    @Parameter(persist = false)
    private String outputs = "";

    @Parameter(persist = false)
    private int threads = 1;

    /**
     * Creates a new instance
     *
     * @param nodeId      ID of the node type that will be encapsulated within this command
     * @param windowTitle the title of the dialog that is created if the user runs this command from the GUI
     */
    public JIPipeRunCustomAlgorithmCommand(String nodeId, String windowTitle) {
        this.nodeId = nodeId;
        this.windowTitle = windowTitle;
    }

    @Override
    public void initialize() {
        resolveInput("parameters");
        resolveInput("threads");
        resolveInput("inputs");
        resolveInput("outputs");
    }

    private void initializeRegistry(boolean withSplash) {
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        ExtensionSettings extensionSettings = ExtensionSettings.getInstanceFromRaw();
        if (!JIPipe.isInstantiated()) {
            UIUtils.loadLookAndFeelFromSettings();
            if (!JIPipe.isInstantiated() && withSplash) {
                SwingUtilities.invokeLater(() -> JIPipeDesktopSplashScreen.getInstance().showSplash(getContext()));
            }
            JIPipe jiPipe = JIPipe.createInstance(getContext());
            JIPipeDesktopSplashScreen.getInstance().setJIPipe(JIPipe.getInstance());
            jiPipe.initialize(extensionSettings, issues);
            SwingUtilities.invokeLater(() -> JIPipeDesktopSplashScreen.getInstance().hideSplash());
        }
        if (!extensionSettings.isSilent()) {
            JIPipeValidationReport report = new JIPipeValidationReport();
            issues.reportValidity(new UnspecifiedValidationReportContext(), report);
            if (!report.isValid()) {
                if (GraphicsEnvironment.isHeadless()) {
                    report.print();
                } else {
                    UIUtils.openValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
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
            JIPipeDesktopRunSingleAlgorithmWindow dialog = new JIPipeDesktopRunSingleAlgorithmWindow(getContext(), JIPipe.getNodes().getInfoById(nodeId));
            dialog.setTitle(windowTitle);
            dialog.setIconImage(UIUtils.getJIPipeIcon128());
            dialog.pack();
            dialog.setSize(new Dimension(1024, 768));
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
                message.append("The provided algorithm options are invalid:\n\n");
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
        IJ.showStatus(windowTitle + " ...");
        IJ.showProgress(1, 3);
        settings.importInputsFromImageJ(progressInfo);
        IJ.showProgress(2, 3);
        JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(threads);
        JIPipeGraphNodeRunContext runContext = new JIPipeGraphNodeRunContext();
        runContext.setThreadPool(threadPool);

        try {
            algorithm.run(runContext, progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
        IJ.showProgress(3, 3);
        settings.exportOutputToImageJ(progressInfo);
    }
}
