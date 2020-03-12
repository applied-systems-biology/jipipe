package org.hkijena.acaq5.ui.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import ij.IJ;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.extension.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.IOException;

public class ACAQGraphWrapperAlgorithmExporter extends ACAQUIPanel {

    private GraphWrapperAlgorithmDeclaration algorithmDeclaration;

    public ACAQGraphWrapperAlgorithmExporter(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph wrappedGraph) {
        super(workbenchUI);
        algorithmDeclaration = new GraphWrapperAlgorithmDeclaration();
        algorithmDeclaration.setGraph(wrappedGraph);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        ACAQParameterAccessUI parameterAccessUI = new ACAQParameterAccessUI(getWorkbenchUI(),
                algorithmDeclaration.getMetadata(),
                MarkdownDocument.fromPluginResource("documentation/exporting-algorithms.md"),
                false,
                true);
        add(parameterAccessUI, BorderLayout.CENTER);


        initializeToolBar();
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        JButton exportToFileButton = new JButton("Export to file", UIUtils.getIconFromResources("save.png"));
        exportToFileButton.setToolTipText("Exports the algorithm as *.json file");
        exportToFileButton.addActionListener(e -> exportToFile());
        toolBar.add(exportToFileButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private boolean checkValidity() {
        ACAQValidityReport report = new ACAQValidityReport();
        algorithmDeclaration.reportValidity(report);
        if(!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
        }
        return report.isValid();
    }

    private void exportToFile() {
        if(!checkValidity())
            return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export as *.json");
        if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileChooser.getSelectedFile(), algorithmDeclaration);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
