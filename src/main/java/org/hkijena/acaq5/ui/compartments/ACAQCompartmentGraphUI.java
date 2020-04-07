package org.hkijena.acaq5.ui.compartments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.DefaultUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph.COMPARTMENT_DEFAULT;

/**
 * Graph editor UI for a project compartment graph
 */
public class ACAQCompartmentGraphUI extends ACAQAlgorithmGraphEditorUI {
    private final MarkdownReader documentationPanel;

    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQCompartmentGraphUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI, workbenchUI.getProject().getCompartmentGraph(), COMPARTMENT_DEFAULT);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/compartment-graph.md"));
        setPropertyPanel(documentationPanel);
    }

    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        initializeAddNodesMenus();
        initializeCommonActions();
    }

    @Override
    protected void updateSelection() {
        if (getSelection().isEmpty()) {
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new ACAQSingleCompartmentSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(),
                    (ACAQProjectCompartment) getSelection().iterator().next().getAlgorithm()));
        } else {
            setPropertyPanel(new ACAQMultiCompartmentSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(),
                    getSelection().stream().map(ui -> (ACAQProjectCompartment) ui.getAlgorithm()).collect(Collectors.toSet())));
        }
    }

    /**
     * Initializes the "Add nodes" area
     */
    protected void initializeAddNodesMenus() {
        ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDeclarationById("acaq:project-compartment");

        JButton addItem = new JButton("Add new compartment", UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(addItem);
        addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
        addItem.addActionListener(e -> addCompartment());
        menuBar.add(addItem);

        JButton importItem = new JButton("Import compartment", UIUtils.getIconFromResources("open.png"));
        UIUtils.makeFlat(importItem);
        importItem.setToolTipText("Imports a compartment from a *.json file");
        importItem.addActionListener(e -> importCompartment());
        menuBar.add(importItem);
    }

    private void importCompartment() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import compartment *.json");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                ACAQExportedCompartment exportedCompartment = objectMapper.readerFor(ACAQExportedCompartment.class).readValue(fileChooser.getSelectedFile());

                String name = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the new compartment:",
                        exportedCompartment.getSuggestedName(), s -> getProject().getCompartments().containsKey(s));
                if (name != null && !name.isEmpty()) {
                    exportedCompartment.addTo(getProject(), name);
                }
            } catch (IOException e) {
            }
        }
    }

    private ACAQProject getProject() {
        return ((ACAQProjectWorkbench) getWorkbench()).getProject();
    }

    private ACAQProjectWorkbench getProjectWorkbench() {
        return (ACAQProjectWorkbench) getWorkbench();
    }

    private void addCompartment() {
        String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                "Compartment", s -> getProject().getCompartments().containsKey(s));
        if (compartmentName != null && !compartmentName.trim().isEmpty()) {
            getProject().addCompartment(compartmentName);
        }
    }

    /**
     * Should be triggered when a user double-clicks a graph node to open it in the graph editor
     *
     * @param event Generated event
     */
    @Subscribe
    public void onOpenCompartment(DefaultUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getAlgorithm() instanceof ACAQProjectCompartment) {
            getProjectWorkbench().openCompartmentGraph((ACAQProjectCompartment) event.getUi().getAlgorithm(), true);
        }
    }
}
