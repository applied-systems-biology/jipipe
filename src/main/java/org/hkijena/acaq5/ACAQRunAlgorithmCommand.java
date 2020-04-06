package org.hkijena.acaq5;

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import net.imagej.ImageJ;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.compat.RunSingleAlgorithmDialog;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * Runs an ACAQ algorithm on a single data set.
 */
@Plugin(type = Command.class, menuPath = "Plugins>ACAQ5>Run ACAQ5 algorithm", headless = true)
public class ACAQRunAlgorithmCommand extends DynamicCommand implements Initializable {

    @Parameter(persist = false)
    private String algorithmId = "";

    @Parameter(persist = false)
    private String algorithmParameters = "";

    @Override
    public void initialize() {
        resolveInput("algorithmId");
        resolveInput("algorithmParameters");
    }

    @Override
    public void run() {
        ACAQDefaultRegistry.instantiate(getContext());
        ACAQAlgorithm algorithm;
        SingleImageJAlgorithmRun settings;
        if (StringUtils.isNullOrEmpty(algorithmId) || StringUtils.isNullOrEmpty(algorithmParameters)) {
            RunSingleAlgorithmDialog dialog = new RunSingleAlgorithmDialog(getContext());
            dialog.setTitle("Run ACAQ5 algorithm");
            dialog.setIconImage(UIUtils.getIconFromResources("acaq5-128.png").getImage());
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
                algorithm = dialog.getAlgorithm();
                settings = dialog.getRunSettings();
            }
        } else {
            ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDeclarationById(algorithmId);
            algorithm = declaration.newInstance();
            settings = new SingleImageJAlgorithmRun(algorithm);
            try {
                settings.fromJson(JsonUtils.getObjectMapper().readValue(algorithmParameters, JsonNode.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ACAQValidityReport report = new ACAQValidityReport();
            settings.reportValidity(report);
            if (!report.isValid()) {
                StringBuilder message = new StringBuilder();
                message.append("The provided algorithm options are invalid:\n\n");
                for (Map.Entry<String, String> entry : report.getMessages().entrySet()) {
                    message.append(entry.getKey()).append("\t").append(entry.getValue());
                }
                cancel(message.toString());
                return;
            }
        }
        IJ.showStatus("Running ACAQ5 algorithm ...");
        IJ.showProgress(1, 3);
        settings.pushInput();
        IJ.showProgress(2, 3);
        algorithm.run();
        IJ.showProgress(3, 3);
        settings.pullOutput();
    }

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(ACAQRunAlgorithmCommand.class, true);
    }
}
