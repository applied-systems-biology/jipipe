package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWindow;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Exports a {@link GraphWrapperAlgorithmDeclaration}
 */
public class ACAQGraphWrapperAlgorithmExporter extends ACAQProjectWorkbenchPanel {

    private GraphWrapperAlgorithmDeclaration algorithmDeclaration;
    private ACAQParameterAccessUI parameterAccessUI;
    private JPopupMenu exportMenu;

    /**
     * Creates a new UI
     *
     * @param workbenchUI  The workbench UI
     * @param wrappedGraph The wrapped graph
     */
    public ACAQGraphWrapperAlgorithmExporter(ACAQProjectWorkbench workbenchUI, ACAQAlgorithmGraph wrappedGraph) {
        super(workbenchUI);
        algorithmDeclaration = new GraphWrapperAlgorithmDeclaration();
        algorithmDeclaration.setGraph(wrappedGraph);
        algorithmDeclaration.getMetadata().setName("My algorithm");
        algorithmDeclaration.getMetadata().setDescription("An ACAQ5 algorithm");
        for (ACAQAlgorithm algorithm : wrappedGraph.getAlgorithmNodes().values()) {
            algorithm.clearLocations();
        }

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        parameterAccessUI = new ACAQParameterAccessUI(getProjectWorkbench(),
                algorithmDeclaration,
                MarkdownDocument.fromPluginResource("documentation/exporting-algorithms.md"),
                false,
                true);
        add(parameterAccessUI, BorderLayout.CENTER);

        initializeToolBar();
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton createRandomIdButton = new JButton("Create random ID", UIUtils.getIconFromResources("random.png"));
        createRandomIdButton.addActionListener(e -> createRandomId());
        toolBar.add(createRandomIdButton);

        toolBar.add(Box.createHorizontalGlue());

//        JButton exportToFileButton = new JButton("Export to file", UIUtils.getIconFromResources("save.png"));
//        exportToFileButton.setToolTipText("Exports the algorithm as *.json file. Please note that the file is not a valid JSON extension.");
//        exportToFileButton.addActionListener(e -> exportToFile());
//        toolBar.add(exportToFileButton);

        JButton exportToExtensionButton = new JButton("Export to extension", UIUtils.getIconFromResources("export.png"));
        exportMenu = UIUtils.addPopupMenuToComponent(exportToExtensionButton);
        toolBar.add(exportToExtensionButton);
        reloadExportMenu();

        add(toolBar, BorderLayout.NORTH);
    }

    private void reloadExportMenu() {
        exportMenu.removeAll();

        JMenuItem exportToNewExtensionButton = new JMenuItem("New extension ...", UIUtils.getIconFromResources("new.png"));
        exportToNewExtensionButton.addActionListener(e -> exportToNewExtension());
        exportMenu.add(exportToNewExtensionButton);

        exportMenu.addSeparator();

        for (ACAQJsonExtensionWindow window : ACAQJsonExtensionWindow.getOpenWindows()) {
            JMenuItem exportToExtensionButton = new JMenuItem(window.getTitle(), UIUtils.getIconFromResources("module.png"));
            exportToExtensionButton.addActionListener(e -> exportToExtension(window.getProject()));
            exportMenu.add(exportToExtensionButton);
        }

        JMenuItem reloadButton = new JMenuItem("Reload", UIUtils.getIconFromResources("refresh.png"));
        reloadButton.addActionListener(e -> reloadExportMenu());
        exportMenu.add(reloadButton);
    }

    private void exportToExtension(ACAQJsonExtension extension) {
        if (!checkValidity())
            return;
        extension.addAlgorithm(algorithmDeclaration);
        getProjectWorkbench().getDocumentTabPane().remove(this);
    }

    private void exportToNewExtension() {
        if (!checkValidity())
            return;
        ACAQJsonExtension extension = new ACAQJsonExtension();
        extension.addAlgorithm(algorithmDeclaration);
        getProjectWorkbench().getDocumentTabPane().remove(this);
        ACAQJsonExtensionWindow.newWindow(getProjectWorkbench().getCommand(), extension);
    }

    private void createRandomId() {
        String name = algorithmDeclaration.getName();
        if (name == null || name.isEmpty()) {
            name = "my-algorithm";
        }
        name = StringUtils.jsonify(name);
        name = StringUtils.makeUniqueString(name, "-", id -> ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id));
        algorithmDeclaration.setId(name);
    }

    private boolean checkValidity() {
        ACAQValidityReport report = new ACAQValidityReport();
        algorithmDeclaration.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
        }
        return report.isValid();
    }

//    private void exportToFile() {
//        if (!checkValidity())
//            return;
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setDialogTitle("Export as *.json");
//        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
//            try {
//                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileChooser.getSelectedFile(), algorithmDeclaration);
//                getWorkbenchUI().sendStatusBarText("Exported custom algorithm '" + algorithmDeclaration.getName() + "' to " + fileChooser.getSelectedFile());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    /**
     * @return The declaration
     */
    public GraphWrapperAlgorithmDeclaration getAlgorithmDeclaration() {
        return algorithmDeclaration;
    }
}
