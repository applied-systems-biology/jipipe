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

import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.api.history.JIPipeDedicatedGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbench;
import org.hkijena.jipipe.extensions.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.dragdrop.JIPipeCreateNodesFromDraggedDataDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphCopyNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphCutNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphPasteNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Graph editor UI used within an {@link JIPipeJsonPlugin}
 */
public class JIPipeDesktopJsonExtensionGraphUI extends JIPipeDesktopGraphEditorUI {

    private JPanel defaultPanel;

    /**
     * Creates a new instance
     *
     * @param workbenchUI    The workbench UI
     * @param algorithmGraph The algorithm graph
     * @param compartment    The compartment
     */
    public JIPipeDesktopJsonExtensionGraphUI(JIPipeDesktopJsonExtensionWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment) {
        super(workbenchUI, algorithmGraph, compartment, new JIPipeDedicatedGraphHistoryJournal(algorithmGraph));
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel, true);

        // Set D&D and Copy&Paste behavior
        getCanvasUI().setDragAndDropBehavior(new JIPipeCreateNodesFromDraggedDataDragAndDropBehavior());
        getCanvasUI().setContextActions(Arrays.asList(
                new AddNewNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new AlgorithmGraphCutNodeUIContextAction(),
                new AlgorithmGraphCopyNodeUIContextAction(),
                new AlgorithmGraphPasteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new IsolateNodesUIContextAction(),
                new JsonAlgorithmToGroupNodeUIContextAction(),
                new GroupNodeUIContextAction(),
                new CollapseIOInterfaceNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new EnableNodeUIContextAction(),
                new DisableNodeUIContextAction(),
                new EnablePassThroughNodeUIContextAction(),
                new DisablePassThroughNodeUIContextAction(),
                new EnableSaveOutputsNodeUIContextAction(),
                new DisableSaveOutputsNodeUIContextAction(),
//                new EnableVirtualOutputsNodeUIContextAction(),
//                new DisableVirtualOutputNodeUIContextAction(),
                new DeleteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SendToForegroundUIContextAction(),
                new RaiseUIContextAction(),
                new LowerUIContextAction(),
                new SendToBackgroundUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction(),
                new LockNodeLocationSizeUIContextAction(),
                new UnlockNodeLocationSizeUIContextAction()
        ));
    }

    private void initializeDefaultPanel() {
        defaultPanel = new JPanel(new BorderLayout());

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_3);
        defaultPanel.add(splitPane, BorderLayout.CENTER);

        JIPipeDesktopGraphEditorMinimap minimap = new JIPipeDesktopGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        JIPipeDesktopMarkdownReader markdownReader = new JIPipeDesktopMarkdownReader(false);
        markdownReader.setDocument(MarkdownText.fromPluginResource("documentation/algorithm-graph.md", new HashMap<>()));
        splitPane.setBottomComponent(markdownReader);
    }

    /**
     * Reloads the menu bar
     */
    @Override
    public void reloadMenuBar() {
        menuBar.removeAll();
        getAddableAlgorithms().clear();
        JIPipePipelineGraphEditorUI.initializeAddNodesMenus(this, menuBar, getAddableAlgorithms());
        initializeCommonActions();
    }

//    @Override
//    public void installNodeUIFeatures(JIPipeAlgorithmUI ui) {
//        ui.installContextMenu(Arrays.asList(
//                new OpenSettingsAlgorithmContextMenuFeature(),
//                new AddToSelectionAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new CutCopyAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new EnableDisablePassThroughAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new DeleteAlgorithmContextMenuFeature()
//        ));
//    }

    @Override
    protected void updateSelection() {
        super.updateSelection();
        if (getSelection().isEmpty()) {
            setPropertyPanel(defaultPanel, true);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new JIPipeDesktopJsonExtensionSingleAlgorithmSelectionPanelUI(this,
                    getSelection().iterator().next().getNode()), true);
        } else {
            setPropertyPanel(new JIPipeDesktopJsonExtensionMultiAlgorithmSelectionPanelUI((JIPipeDesktopJsonExtensionWorkbench) getDesktopWorkbench(), getCanvasUI(),
                    getSelection().stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet())), true);
        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }
}
