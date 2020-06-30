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

package org.hkijena.acaq5.ui.compartments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.DefaultAlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.ExportCompartmentAsJsonAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.ExportCompartmentToAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.SelectAndMoveNodeHereAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.DeleteCompartmentUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard.GraphCompartmentCopyAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard.GraphCompartmentCutAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard.GraphCompartmentPasteAlgorithmUIAction;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.api.algorithm.ACAQGraph.COMPARTMENT_DEFAULT;

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

        // Copy & paste behavior
        getCanvasUI().setContextActions(Arrays.asList(
                new GraphCompartmentCutAlgorithmUIAction(),
                new GraphCompartmentCopyAlgorithmUIAction(),
                new GraphCompartmentPasteAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new ExportCompartmentAsJsonAlgorithmUIAction(),
                new ExportCompartmentToAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new DeleteCompartmentUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new SelectAndMoveNodeHereAlgorithmUIAction()
        ));
    }

    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        initializeAddNodesMenus();
        initializeCommonActions();
    }

//    @Override
//    public void installNodeUIFeatures(ACAQAlgorithmUI ui) {
//        ui.installContextMenu(Arrays.asList(
//                new OpenSettingsAlgorithmContextMenuFeature(),
//                new AddToSelectionAlgorithmContextMenuFeature(),
//                new CutCopyAlgorithmContextMenuFeature(),
//                new DeleteCompartmentContextMenuFeature()
//        ));
//    }

    @Override
    protected void updateSelection() {
        super.updateSelection();
        if (getSelection().isEmpty()) {
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            setPropertyPanel(new ACAQSingleCompartmentSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(),
                    (ACAQProjectCompartment) getSelection().iterator().next().getAlgorithm(), getCanvasUI()));
        } else {
            setPropertyPanel(new ACAQMultiCompartmentSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(),
                    getSelection().stream().map(ui -> (ACAQProjectCompartment) ui.getAlgorithm()).collect(Collectors.toSet()), getCanvasUI()));
        }
    }

    /**
     * Initializes the "Add nodes" area
     */
    protected void initializeAddNodesMenus() {
        ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDeclarationById("acaq:project-compartment");

        JButton addItem = new JButton("Add new compartment", UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlatH25(addItem);
        addItem.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
        addItem.addActionListener(e -> addCompartment());
        menuBar.add(addItem);

        JButton importItem = new JButton("Import compartment", UIUtils.getIconFromResources("open.png"));
        UIUtils.makeFlatH25(importItem);
        importItem.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        importItem.setToolTipText("Imports a compartment from a *.json file");
        importItem.addActionListener(e -> importCompartment());
        menuBar.add(importItem);
    }

    private void importCompartment() {
        Path selectedPath = FileChooserSettings.openFile(this, FileChooserSettings.KEY_PROJECT, "Open ACAQ5 graph compartment (*.json)");
        if (selectedPath != null) {
            try {
                ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                ACAQExportedCompartment exportedCompartment = objectMapper.readerFor(ACAQExportedCompartment.class).readValue(selectedPath.toFile());

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
    public void onOpenCompartment(DefaultAlgorithmUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getAlgorithm() instanceof ACAQProjectCompartment) {
            getProjectWorkbench().openCompartmentGraph((ACAQProjectCompartment) event.getUi().getAlgorithm(), true);
        }
    }
}
