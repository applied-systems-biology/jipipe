package org.hkijena.acaq5.ui.extensionbuilder.traiteditor;

import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.components.ACAQTraitPicker;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitGraph;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitNode;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitNodeInheritanceData;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph.COMPARTMENT_DEFAULT;

/**
 * Graph editor UI to organize traits
 */
public class ACAQTraitGraphUI extends ACAQAlgorithmGraphEditorUI {

    private MarkdownReader documentationPanel;

    /**
     * @param workbenchUI the workbench
     */
    public ACAQTraitGraphUI(ACAQJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI, new ACAQTraitGraph(workbenchUI.getProject()), COMPARTMENT_DEFAULT);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/trait-graph.md"));
        setPropertyPanel(documentationPanel);
    }

    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        initializeAddNodesMenus();
        initializeCommonActions();
    }

    /**
     * Initializes the "Add" menu
     */
    protected void initializeAddNodesMenus() {
        JButton addItem = new JButton("New annotation", UIUtils.getIconFromResources("new.png"));
        UIUtils.makeFlat(addItem);
        addItem.setToolTipText("Adds a new custom annotation type.");
        addItem.addActionListener(e -> addNewAnnotation(false));
        menuBar.add(addItem);

        JButton addWithInputItem = new JButton("New sub-annotation", UIUtils.getIconFromResources("new.png"));
        UIUtils.makeFlat(addWithInputItem);
        addWithInputItem.setToolTipText("Adds a new custom annotation type. It already comes with an input slot for inheritance.");
        addWithInputItem.addActionListener(e -> addNewAnnotation(true));
        menuBar.add(addWithInputItem);

        JButton importItem = new JButton("Add existing annotation", UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(importItem);
        importItem.addActionListener(e -> addExistingAnnotation());
        menuBar.add(importItem);
    }

    private void addExistingAnnotation() {
        ACAQTraitGraph graph = (ACAQTraitGraph) getAlgorithmGraph();
        Set<ACAQTraitDeclaration> available = ACAQTraitRegistry.getInstance().getRegisteredTraits().values()
                .stream().filter(d -> !graph.containsTrait(d)).collect(Collectors.toSet());
        Set<ACAQTraitDeclaration> selected = ACAQTraitPicker.showDialog(this,
                ACAQTraitPicker.Mode.Multiple,
                available);
        for (ACAQTraitDeclaration declaration : selected) {
            graph.addExternalTrait(declaration);
        }
    }

    private void addNewAnnotation(boolean withInputSlot) {
        ACAQJsonTraitDeclaration declaration = new ACAQJsonTraitDeclaration();
        getProject().addTrait(declaration);
        if (withInputSlot) {
            ACAQTraitNode node = getTraitGraph().getNodeFor(declaration);
            if (node != null) {
                ((ACAQMutableSlotConfiguration) node.getSlotConfiguration()).addSlot("Input 1",
                        new ACAQSlotDefinition(ACAQTraitNodeInheritanceData.class, ACAQDataSlot.SlotType.Input, "Input 1", null), false);
            }
        }
    }

    public ACAQJsonExtensionWorkbench getExtensionWorkbench() {
        return (ACAQJsonExtensionWorkbench) getWorkbench();
    }

    public ACAQJsonExtension getProject() {
        return getExtensionWorkbench().getProject();
    }

    public ACAQTraitGraph getTraitGraph() {
        return (ACAQTraitGraph) getAlgorithmGraph();
    }

    @Override
    protected void updateSelection() {
        if (getSelection().isEmpty()) {
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new ACAQSingleTraitSelectionPanelUI(getExtensionWorkbench(),
                    (ACAQTraitNode) getSelection().iterator().next().getAlgorithm(),
                    getTraitGraph()));
        } else {
            setPropertyPanel(new ACAQMultiTraitSelectionPanelUI(getExtensionWorkbench(),
                    getTraitGraph(), getSelection().stream().map(a -> (ACAQTraitNode) a.getAlgorithm()).collect(Collectors.toSet())));
        }
    }

}
