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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.ui.compat.RunSingleAlgorithmDialog;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * Runs a JIPipe algorithm on a single data set.
 */
@Plugin(type = Command.class, menuPath = "Plugins>JIPipe>Run JIPipe algorithm", headless = true)
public class JIPipeRunAlgorithmCommand extends DynamicCommand implements Initializable {

    @Parameter(persist = false)
    private String algorithmId = "";

    @Parameter(persist = false)
    private String algorithmParameters = "";

    @Parameter(persist = false)
    private int threads = 1;

    @Override
    public void initialize() {
        resolveInput("algorithmId");
        resolveInput("algorithmParameters");
        resolveInput("threads");
    }

    @Override
    public void run() {
        JIPipeDefaultRegistry.instantiate(getContext());
        JIPipeGraphNode algorithm;
        SingleImageJAlgorithmRun settings;
        if (StringUtils.isNullOrEmpty(algorithmId) || StringUtils.isNullOrEmpty(algorithmParameters)) {
            UIUtils.loadLookAndFeelFromSettings();
            RunSingleAlgorithmDialog dialog = new RunSingleAlgorithmDialog(getContext());
            dialog.setTitle("Run JIPipe algorithm");
            dialog.setIconImage(UIUtils.getIconFromResources("jipipe-128.png").getImage());
            dialog.setModal(true);
            dialog.pack();
            dialog.setSize(new Dimension(800, 600));
            UIUtils.addEscapeListener(dialog);
            dialog.setVisible(true);
            if (dialog.isCanceled()) {
                cancel("User clicked 'Cancel' in setup dialog.");
                return;
            } else {
                algorithmId = dialog.getAlgorithmId();
                algorithmParameters = dialog.getAlgorithmParametersJson();
                threads = dialog.getNumThreads();
                algorithm = dialog.getAlgorithm();
                settings = dialog.getRunSettings();
            }
        } else {
            JIPipeNodeInfo info = JIPipeNodeRegistry.getInstance().getInfoById(algorithmId);
            algorithm = info.newInstance();
            settings = new SingleImageJAlgorithmRun(algorithm);
            try {
                settings.fromJson(JsonUtils.getObjectMapper().readValue(algorithmParameters, JsonNode.class));
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to load parameters from JSON!",
                        "Run JIPipe algorithm", "Either the data is not valid JSON, does not fit to the selected algorithm, or was corrupted.",
                        "Please check if the text parameter is valid JSON. You can use a JSON online validator to validate the format. " +
                                "Also please check if the parameters really fit to the selected algorithm.");
            }
            JIPipeValidityReport report = new JIPipeValidityReport();
            settings.reportValidity(report);
            if (!report.isValid()) {
                StringBuilder message = new StringBuilder();
                message.append("The provided algorithm options are invalid:\n\n");
                for (Map.Entry<String, JIPipeValidityReport.Message> entry : report.getMessages().entrySet()) {
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
            algorithm.run(new JIPipeRunnerSubStatus(), s -> IJ.showStatus("Running JIPipe algorithm ... " + s), () -> false);
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

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(JIPipeRunAlgorithmCommand.class, true);
    }
}
