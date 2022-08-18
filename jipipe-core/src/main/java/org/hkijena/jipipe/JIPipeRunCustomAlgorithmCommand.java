/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRunConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.ui.compat.RunSingleAlgorithmWindow;
import org.hkijena.jipipe.ui.components.SplashScreen;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

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
                SwingUtilities.invokeLater(() -> SplashScreen.getInstance().showSplash(getContext()));
            }
            JIPipe jiPipe = JIPipe.createInstance(getContext());
            SplashScreen.getInstance().setJIPipe(JIPipe.getInstance());
            jiPipe.initialize(extensionSettings, issues);
            SwingUtilities.invokeLater(() -> SplashScreen.getInstance().hideSplash());
        }
        if (!extensionSettings.isSilent()) {
            JIPipeIssueReport report = new JIPipeIssueReport();
            issues.reportValidity(report);
            if (!report.isValid()) {
                if (GraphicsEnvironment.isHeadless()) {
                    report.print();
                } else {
                    UIUtils.openValidityReportDialog(null, report, "Errors while initializing JIPipe", "There were some issues while initializing JIPipe. Please run the JIPipe GUI for more information.", false);
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
            RunSingleAlgorithmWindow dialog = new RunSingleAlgorithmWindow(getContext(), JIPipe.getNodes().getInfoById(nodeId));
            dialog.setTitle(windowTitle);
            dialog.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
            dialog.pack();
            dialog.setSize(new Dimension(1024, 768));
            dialog.setVisible(true);
            return;
        } else {
            initializeRegistry(false);
            settings = new SingleImageJAlgorithmRunConfiguration(nodeId, parameters, inputs, outputs, threads);
            algorithm = settings.getAlgorithm();
            JIPipeIssueReport report = new JIPipeIssueReport();
            settings.reportValidity(report);
            if (!report.isValid()) {
                StringBuilder message = new StringBuilder();
                message.append("The provided algorithm options are invalid:\n\n");
                for (Map.Entry<String, JIPipeIssueReport.Issue> entry : report.getIssues().entries()) {
                    message.append(entry.getKey()).append("\t").append(entry.getValue());
                }
                cancel(message.toString());
                return;
            }
        }
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        progressInfo.getEventBus().register(new Object() {
            @Subscribe
            public void onProgress(JIPipeProgressInfo.StatusUpdatedEvent event) {
                IJ.showStatus("[" + event.getProgress() + "/" + event.getMaxProgress() + "] " + event.getMessage());
            }
        });
        IJ.showStatus(windowTitle + " ...");
        IJ.showProgress(1, 3);
        settings.importInputsFromImageJ(progressInfo);
        IJ.showProgress(2, 3);
        JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(threads);
        try {
            if (algorithm instanceof JIPipeAlgorithm)
                ((JIPipeAlgorithm) algorithm).setThreadPool(threadPool);
            algorithm.run(progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (algorithm instanceof JIPipeAlgorithm)
                ((JIPipeAlgorithm) algorithm).setThreadPool(null);
            threadPool.shutdown();
            threadPool = null;
        }
        IJ.showProgress(3, 3);
        settings.exportOutputToImageJ(progressInfo);
    }
}
