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

package org.hkijena.jipipe.ui.extensionbuilder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Exports a {@link JsonNodeInfo}
 */
public class JIPipeJsonExporter extends JIPipeGraphEditorUI {

    private final JsonNodeInfo nodeInfo;
    private JPopupMenu exportMenu;
    private JPanel exportPanel;

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param group       the node group that will be converted into an algorithm
     */
    public JIPipeJsonExporter(JIPipeProjectWorkbench workbenchUI, NodeGroup group) {
        super(workbenchUI, group.getWrappedGraph(), null);
        nodeInfo = new JsonNodeInfo(group);
        nodeInfo.setName(group.getName());
        nodeInfo.setDescription(group.getCustomDescription());
        initialize();
        updateSelection();
    }

//    @Override
//    public void installNodeUIFeatures(JIPipeAlgorithmUI ui) {
//        ui.installContextMenu(Arrays.asList(
//                new AddToSelectionAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new CollapseIOInterfaceAlgorithmContextMenuFeature(),
//                new DeleteAlgorithmContextMenuFeature()
//        ));
//    }

    private void initialize() {
        exportPanel = new JPanel(new BorderLayout());

        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), nodeInfo,
                MarkdownDocument.fromPluginResource("documentation/exporting-algorithms.md", new HashMap<>()),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        exportPanel.add(parameterPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton createRandomIdButton = new JButton("Create random algorithm ID", UIUtils.getIconFromResources("actions/random.png"));
        createRandomIdButton.addActionListener(e -> createRandomId());
        toolBar.add(createRandomIdButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton exportToExtensionButton = new JButton("Export to extension", UIUtils.getIconFromResources("actions/document-export.png"));
        exportMenu = UIUtils.addPopupMenuToComponent(exportToExtensionButton);
        toolBar.add(exportToExtensionButton);
        reloadExportMenu();

        exportPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void reloadExportMenu() {
        exportMenu.removeAll();

        JMenuItem exportToNewExtensionButton = new JMenuItem("New extension ...", UIUtils.getIconFromResources("actions/document-new.png"));
        exportToNewExtensionButton.addActionListener(e -> exportToNewExtension());
        exportMenu.add(exportToNewExtensionButton);

        exportMenu.addSeparator();

        for (JIPipeJsonExtensionWindow window : JIPipeJsonExtensionWindow.getOpenWindows()) {
            JMenuItem exportToExtensionButton = new JMenuItem(window.getTitle(), UIUtils.getIconFromResources("actions/plugins.png"));
            exportToExtensionButton.addActionListener(e -> exportToExtension(window.getProject()));
            exportMenu.add(exportToExtensionButton);
        }

        JMenuItem reloadButton = new JMenuItem("Reload", UIUtils.getIconFromResources("actions/view-refresh.png"));
        reloadButton.addActionListener(e -> reloadExportMenu());
        exportMenu.add(reloadButton);
    }

    private void exportToExtension(JIPipeJsonExtension extension) {
        extension.addAlgorithm(nodeInfo);
        getWorkbench().getDocumentTabPane().remove(this);
    }

    private void exportToNewExtension() {
        JIPipeJsonExtension extension = new JIPipeJsonExtension();
        extension.addAlgorithm(nodeInfo);
        getWorkbench().getDocumentTabPane().remove(this);
        JIPipeJsonExtensionWindow.newWindow(getWorkbench().getContext(), extension, false);
    }

    private void createRandomId() {
        String name = nodeInfo.getName();
        if (name == null || name.isEmpty()) {
            name = "my-algorithm";
        }
        name = StringUtils.jsonify(name);
        name = StringUtils.makeUniqueString(name, "-", id -> JIPipe.getNodes().hasNodeInfoWithId(id));
        nodeInfo.setId(name);
    }

    @Override
    protected void updateSelection() {
        super.updateSelection();
        if (getSelection().isEmpty()) {
            setPropertyPanel(exportPanel);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new JIPipeJsonAlgorithmExporterSingleSelectionPanelUI(this,
                    getSelection().iterator().next().getNode()));
        } else {
            setPropertyPanel(new JIPipeJsonAlgorithmExporterMultiSelectionPanelUI(getWorkbench(),
                    getCanvasUI(),
                    getSelection().stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet())));
        }
    }

    /**
     * @return The info
     */
    public JsonNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
     * Creates a new exporter tab
     *
     * @param workbench   the workbench
     * @param nodeGroup   the exported algorithm. Will be copied.
     * @param name        predefined name
     * @param description predefined description
     */
    public static void createExporter(JIPipeProjectWorkbench workbench, NodeGroup nodeGroup, String name, HTMLText description) {
        JIPipeJsonExporter exporter = new JIPipeJsonExporter(workbench, (NodeGroup) nodeGroup.duplicate());
        exporter.getNodeInfo().setName(name);
        exporter.getNodeInfo().setDescription(description);
        workbench.getDocumentTabPane().addTab("Export algorithm '" + name + "'",
                UIUtils.getIconFromResources("actions/document-export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
        workbench.getDocumentTabPane().switchToLastTab();
    }
}
