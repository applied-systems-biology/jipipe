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

package org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.history.JIPipeDedicatedGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWindow;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.extensions.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Exports a {@link JsonNodeInfo}
 */
public class JIPipeDesktopJsonExporter extends JIPipeDesktopGraphEditorUI {

    private final JsonNodeInfo nodeInfo;
    private JPopupMenu exportMenu;
    private JPanel exportPanel;

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param group       the node group that will be converted into an algorithm
     */
    public JIPipeDesktopJsonExporter(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeNodeGroup group) {
        super(workbenchUI, group.getWrappedGraph(), null, new JIPipeDedicatedGraphHistoryJournal(group.getWrappedGraph()));
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

    /**
     * Creates a new exporter tab
     *
     * @param workbench   the workbench
     * @param nodeGroup   the exported algorithm. Will be copied.
     * @param name        predefined name
     * @param description predefined description
     */
    public static void createExporter(JIPipeDesktopProjectWorkbench workbench, JIPipeNodeGroup nodeGroup, String name, HTMLText description) {
        JIPipeDesktopJsonExporter exporter = new JIPipeDesktopJsonExporter(workbench, (JIPipeNodeGroup) nodeGroup.duplicate());
        exporter.getNodeInfo().setName(name);
        exporter.getNodeInfo().setDescription(description);
        workbench.getDocumentTabPane().addTab("Export algorithm '" + name + "'",
                UIUtils.getIconFromResources("actions/document-export.png"),
                exporter,
                JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton);
        workbench.getDocumentTabPane().switchToLastTab();
    }

    private void initialize() {
        exportPanel = new JPanel(new BorderLayout());

        JIPipeDesktopParameterPanel parameterPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(), nodeInfo,
                MarkdownText.fromPluginResource("documentation/exporting-algorithms.md", new HashMap<>()),
                JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterPanel.WITH_SEARCH_BAR);
        exportPanel.add(parameterPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton createRandomIdButton = new JButton("Create random algorithm ID", UIUtils.getIconFromResources("actions/random.png"));
        createRandomIdButton.addActionListener(e -> createRandomId());
        toolBar.add(createRandomIdButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton exportToExtensionButton = new JButton("Export to extension", UIUtils.getIconFromResources("actions/document-export.png"));
        exportMenu = UIUtils.addPopupMenuToButton(exportToExtensionButton);
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

        for (JIPipeDesktopJsonExtensionWindow window : JIPipeDesktopJsonExtensionWindow.getOpenWindows()) {
            JMenuItem exportToExtensionButton = new JMenuItem(window.getTitle(), UIUtils.getIconFromResources("actions/plugins.png"));
            exportToExtensionButton.addActionListener(e -> exportToExtension(window.getProject()));
            exportMenu.add(exportToExtensionButton);
        }

        JMenuItem reloadButton = new JMenuItem("Reload", UIUtils.getIconFromResources("actions/view-refresh.png"));
        reloadButton.addActionListener(e -> reloadExportMenu());
        exportMenu.add(reloadButton);
    }

    private void exportToExtension(JIPipeJsonPlugin extension) {
        extension.addAlgorithm(nodeInfo);
        getDesktopWorkbench().getDocumentTabPane().remove(this);
    }

    private void exportToNewExtension() {
        JIPipeJsonPlugin extension = new JIPipeJsonPlugin();
        extension.addAlgorithm(nodeInfo);
        getDesktopWorkbench().getDocumentTabPane().remove(this);
        JIPipeDesktopJsonExtensionWindow.newWindow(getDesktopWorkbench().getContext(), extension, false);
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
            setPropertyPanel(exportPanel, true);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new JIPipeDesktopJsonAlgorithmExporterSingleSelectionPanelUI(this,
                    getSelection().iterator().next().getNode()), true);
        } else {
            setPropertyPanel(new JIPipeDesktopJsonAlgorithmExporterMultiSelectionPanelUI(getDesktopWorkbench(),
                    getCanvasUI(),
                    getSelection().stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet())), true);
        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }

    /**
     * @return The info
     */
    public JsonNodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
