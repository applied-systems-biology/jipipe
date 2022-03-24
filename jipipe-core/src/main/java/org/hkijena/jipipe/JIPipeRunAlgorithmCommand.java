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

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import net.imagej.ImageJ;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.ui.compat.RunSingleAlgorithmDialog;
import org.hkijena.jipipe.ui.components.SplashScreen;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * Runs a JIPipe algorithm on a single data set.
 */
@Plugin(type = Command.class, menuPath = "Plugins>JIPipe>Run JIPipe algorithm", headless = true)
public class JIPipeRunAlgorithmCommand extends DynamicCommand implements Initializable {

    @Parameter(persist = false)
    private String nodeId = "";

    @Parameter(persist = false)
    private String parameters = "";

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
                    UIUtils.openValidityReportDialog(null, report, false);
                }
            }
        }
    }

    @Override
    public void run() {
        JIPipeGraphNode algorithm;
        SingleImageJAlgorithmRun settings;
        if (StringUtils.isNullOrEmpty(nodeId) || StringUtils.isNullOrEmpty(parameters)) {
            UIUtils.loadLookAndFeelFromSettings();
            initializeRegistry(true);
            RunSingleAlgorithmDialog dialog = new RunSingleAlgorithmDialog(getContext());
            dialog.setTitle("Run JIPipe algorithm");
            dialog.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
            dialog.setModal(true);
            dialog.pack();
            dialog.setSize(new Dimension(1024, 768));
            UIUtils.addEscapeListener(dialog);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            if (dialog.isCanceled()) {
                cancel("User clicked 'Cancel' in setup dialog.");
                return;
            } else {
                nodeId = dialog.getAlgorithmId();
                parameters = dialog.getAlgorithmParametersJson();
                threads = dialog.getNumThreads();
                algorithm = dialog.getAlgorithm();
                settings = dialog.getRunSettings();
            }
        } else {
            initializeRegistry(false);
            JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(nodeId);
            algorithm = info.newInstance();
            settings = new SingleImageJAlgorithmRun(algorithm);
            try {
                settings.fromJson(JsonUtils.getObjectMapper().readValue(parameters, JsonNode.class));
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to load parameters from JSON!",
                        "Run JIPipe algorithm", "Either the data is not valid JSON, does not fit to the selected algorithm, or was corrupted.",
                        "Please check if the text parameter is valid JSON. You can use a JSON online validator to validate the format. " +
                                "Also please check if the parameters really fit to the selected algorithm.");
            }
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
        IJ.showStatus("Running JIPipe algorithm ...");
        IJ.showProgress(1, 3);
        settings.pushInput();
        IJ.showProgress(2, 3);
        JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(threads);
        try {
            if (algorithm instanceof JIPipeAlgorithm)
                ((JIPipeAlgorithm) algorithm).setThreadPool(threadPool);
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
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
        settings.pullOutput();
    }
}
