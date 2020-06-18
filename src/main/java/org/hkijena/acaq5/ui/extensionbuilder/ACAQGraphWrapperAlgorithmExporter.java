package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.grouping.JsonAlgorithmDeclaration;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.AddToSelectionAlgorithmContextMenuFeature;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.CollapseIOInterfaceAlgorithmContextMenuFeature;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.DeleteAlgorithmContextMenuFeature;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.SeparatorAlgorithmContextMenuFeature;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Arrays;

/**
 * Exports a {@link JsonAlgorithmDeclaration}
 */
public class ACAQGraphWrapperAlgorithmExporter extends ACAQAlgorithmGraphEditorUI {

    private final JsonAlgorithmDeclaration algorithmDeclaration;
    private JPopupMenu exportMenu;

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param group       the node group that will be converted into an algorithm
     */
    public ACAQGraphWrapperAlgorithmExporter(ACAQProjectWorkbench workbenchUI, NodeGroup group) {
        super(workbenchUI, group.getWrappedGraph(), ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        algorithmDeclaration = new JsonAlgorithmDeclaration(group);
        algorithmDeclaration.getMetadata().setName(group.getName());
        algorithmDeclaration.getMetadata().setDescription(group.getCustomDescription());
//        initialize();
    }

    @Override
    public void installNodeUIFeatures(ACAQAlgorithmUI ui) {
        ui.installContextMenu(Arrays.asList(
                new AddToSelectionAlgorithmContextMenuFeature(),
                new SeparatorAlgorithmContextMenuFeature(),
                new CollapseIOInterfaceAlgorithmContextMenuFeature(),
                new DeleteAlgorithmContextMenuFeature()
        ));
    }

//    private void initialize() {
//        setLayout(new BorderLayout());
//
////        ParameterPanel parameterAccessUI = new ParameterPanel(getProjectWorkbench(),
////                algorithmDeclaration,
////                MarkdownDocument.fromPluginResource("documentation/exporting-algorithms.md"),
////                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
////        add(parameterAccessUI, BorderLayout.CENTER);
////
////        initializeToolBar();
//    }

//    private void initializeToolBar() {
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//
//        JButton createRandomIdButton = new JButton("Create random ID", UIUtils.getIconFromResources("random.png"));
//        createRandomIdButton.addActionListener(e -> createRandomId());
//        toolBar.add(createRandomIdButton);
//
//        toolBar.add(Box.createHorizontalGlue());
//
//        JButton exportToExtensionButton = new JButton("Export to extension", UIUtils.getIconFromResources("export.png"));
//        exportMenu = UIUtils.addPopupMenuToComponent(exportToExtensionButton);
//        toolBar.add(exportToExtensionButton);
//        reloadExportMenu();
//
//        add(toolBar, BorderLayout.NORTH);
//    }
//
//    private void reloadExportMenu() {
//        exportMenu.removeAll();
//
//        JMenuItem exportToNewExtensionButton = new JMenuItem("New extension ...", UIUtils.getIconFromResources("new.png"));
//        exportToNewExtensionButton.addActionListener(e -> exportToNewExtension());
//        exportMenu.add(exportToNewExtensionButton);
//
//        exportMenu.addSeparator();
//
//        for (ACAQJsonExtensionWindow window : ACAQJsonExtensionWindow.getOpenWindows()) {
//            JMenuItem exportToExtensionButton = new JMenuItem(window.getTitle(), UIUtils.getIconFromResources("module.png"));
//            exportToExtensionButton.addActionListener(e -> exportToExtension(window.getProject()));
//            exportMenu.add(exportToExtensionButton);
//        }
//
//        JMenuItem reloadButton = new JMenuItem("Reload", UIUtils.getIconFromResources("refresh.png"));
//        reloadButton.addActionListener(e -> reloadExportMenu());
//        exportMenu.add(reloadButton);
//    }

//    private void exportToExtension(ACAQJsonExtension extension) {
//        if (!checkValidity())
//            return;
//        extension.addAlgorithm(algorithmDeclaration);
//        getWorkbench().getDocumentTabPane().remove(this);
//    }
//
//    private void exportToNewExtension() {
//        if (!checkValidity())
//            return;
//        ACAQJsonExtension extension = new ACAQJsonExtension();
//        extension.addAlgorithm(algorithmDeclaration);
//        getWorkbench().getDocumentTabPane().remove(this);
//        ACAQJsonExtensionWindow.newWindow(getWorkbench().getContext(), extension);
//    }
//
//    private void createRandomId() {
//        String name = algorithmDeclaration.getName();
//        if (name == null || name.isEmpty()) {
//            name = "my-algorithm";
//        }
//        name = StringUtils.jsonify(name);
//        name = StringUtils.makeUniqueString(name, "-", id -> ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id));
//        algorithmDeclaration.setId(name);
//    }

    private boolean checkValidity() {
        ACAQValidityReport report = new ACAQValidityReport();
        algorithmDeclaration.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
        }
        return report.isValid();
    }

    /**
     * @return The declaration
     */
    public JsonAlgorithmDeclaration getAlgorithmDeclaration() {
        return algorithmDeclaration;
    }

    /**
     * Creates a new exporter tab
     *
     * @param workbench   the workbench
     * @param nodeGroup   the exported algorithm. Will be copied.
     * @param name        predefined name
     * @param description predefined description
     */
    public static void createExporter(ACAQProjectWorkbench workbench, NodeGroup nodeGroup, String name, String description) {
        ACAQGraphWrapperAlgorithmExporter exporter = new ACAQGraphWrapperAlgorithmExporter(workbench, (NodeGroup) nodeGroup.duplicate());
        exporter.getAlgorithmDeclaration().getMetadata().setName(name);
        exporter.getAlgorithmDeclaration().getMetadata().setDescription(description);
        workbench.getDocumentTabPane().addTab("Export algorithm '" + name + "'",
                UIUtils.getIconFromResources("export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
        workbench.getDocumentTabPane().switchToLastTab();
    }
}
