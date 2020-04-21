package org.hkijena.acaq5.ui.extensionbuilder.grapheditor;

import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.*;

import java.util.stream.Collectors;

/**
 * Graph editor UI used within an {@link ACAQJsonExtension}
 */
public class ACAQJsonExtensionAlgorithmGraphUI extends ACAQAlgorithmGraphEditorUI {

    private MarkdownReader documentationPanel;

    /**
     * Creates a new instance
     *
     * @param workbenchUI    The workbench UI
     * @param algorithmGraph The algorithm graph
     * @param compartment    The compartment
     */
    public ACAQJsonExtensionAlgorithmGraphUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbenchUI, algorithmGraph, compartment);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        setPropertyPanel(documentationPanel);

        // Set D&D and Copy&Paste behavior
        getGraphUI().setDragAndDropBehavior(new ACAQStandardDragAndDropBehavior());
        getGraphUI().setCopyPasteBehavior(new ACAQStandardCopyPasteBehavior(this));
        reloadContextMenu();
    }

    /**
     * Reloads the menu bar
     */
    @Override
    public void reloadMenuBar() {
        menuBar.removeAll();
        getAddableAlgorithms().clear();
        ACAQAlgorithmGraphCompartmentUI.initializeAddNodesMenus(menuBar, getAlgorithmGraph(), getCompartment(), getAddableAlgorithms());
        initializeCommonActions();
        updateNavigation();
    }


    @Override
    protected void updateSelection() {
        super.updateSelection();
        if (getSelection().isEmpty()) {
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new ACAQJsonExtensionSingleAlgorithmSelectionPanelUI((ACAQJsonExtensionWorkbench) getWorkbench(),
                    getGraphUI(), getSelection().iterator().next().getAlgorithm()));
        } else {
            setPropertyPanel(new ACAQJsonExtensionMultiAlgorithmSelectionPanelUI((ACAQJsonExtensionWorkbench) getWorkbench(), getGraphUI(),
                    getSelection().stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
        }
    }
}
