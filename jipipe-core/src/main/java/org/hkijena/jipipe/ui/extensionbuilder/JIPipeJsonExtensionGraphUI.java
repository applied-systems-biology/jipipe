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

import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCompartmentUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeStandardDragAndDropBehavior;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCopyNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCutNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphPasteNodeUIContextAction;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Graph editor UI used within an {@link JIPipeJsonExtension}
 */
public class JIPipeJsonExtensionGraphUI extends JIPipeGraphEditorUI {

    private MarkdownReader documentationPanel;

    /**
     * Creates a new instance
     *
     * @param workbenchUI    The workbench UI
     * @param algorithmGraph The algorithm graph
     * @param compartment    The compartment
     */
    public JIPipeJsonExtensionGraphUI(JIPipeJsonExtensionWorkbench workbenchUI, JIPipeGraph algorithmGraph, String compartment) {
        super(workbenchUI, algorithmGraph, compartment);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        setPropertyPanel(documentationPanel);

        // Set D&D and Copy&Paste behavior
        getCanvasUI().setDragAndDropBehavior(new JIPipeStandardDragAndDropBehavior());
        getCanvasUI().setContextActions(Arrays.asList(
                new AlgorithmGraphCutNodeUIContextAction(),
                new AlgorithmGraphCopyNodeUIContextAction(),
                new AlgorithmGraphPasteNodeUIContextAction()
        ));
    }

    /**
     * Reloads the menu bar
     */
    @Override
    public void reloadMenuBar() {
        menuBar.removeAll();
        getAddableAlgorithms().clear();
        JIPipeGraphCompartmentUI.initializeAddNodesMenus(this, menuBar, getAddableAlgorithms());
        initializeCommonActions();
        updateNavigation();
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
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new JIPipeJsonExtensionSingleAlgorithmSelectionPanelUI(this,
                    getSelection().iterator().next().getNode()));
        } else {
            setPropertyPanel(new JIPipeJsonExtensionMultiAlgorithmSelectionPanelUI((JIPipeJsonExtensionWorkbench) getWorkbench(), getCanvasUI(),
                    getSelection().stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet())));
        }
    }
}
