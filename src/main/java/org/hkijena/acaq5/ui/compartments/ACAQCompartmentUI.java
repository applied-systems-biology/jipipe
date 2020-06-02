package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCompartmentUI;

import java.awt.BorderLayout;

/**
 * Editor for one project compartment graph
 * Contains a {@link ACAQAlgorithmGraphCompartmentUI} instance that allows editing the compartment's content
 */
public class ACAQCompartmentUI extends ACAQProjectWorkbenchPanel {

    private ACAQProjectCompartment compartment;
    private ACAQAlgorithmGraphCompartmentUI graphUI;

    /**
     * Creates a new editor
     *
     * @param workbenchUI the workbench UI
     * @param compartment the compartment
     */
    public ACAQCompartmentUI(ACAQProjectWorkbench workbenchUI, ACAQProjectCompartment compartment) {
        super(workbenchUI);
        this.compartment = compartment;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        graphUI = new ACAQAlgorithmGraphCompartmentUI(getProjectWorkbench(), compartment.getProject().getGraph(), compartment.getProjectCompartmentId());
        add(graphUI, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//
//
//        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @return The displayed compartment
     */
    public ACAQProjectCompartment getCompartment() {
        return compartment;
    }
}
