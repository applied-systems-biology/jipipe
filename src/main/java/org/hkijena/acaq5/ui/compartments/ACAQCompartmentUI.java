package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphUI;

import java.awt.*;

public class ACAQCompartmentUI extends ACAQProjectUIPanel {

    private ACAQProjectCompartment compartment;
    private ACAQAlgorithmGraphUI graphUI;

    public ACAQCompartmentUI(ACAQProjectUI workbenchUI, ACAQProjectCompartment compartment) {
        super(workbenchUI);
        this.compartment = compartment;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        graphUI = new ACAQAlgorithmGraphUI(getWorkbenchUI(), compartment.getProject().getGraph(), compartment.getProjectCompartmentId());
        add(graphUI, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//
//
//        add(toolBar, BorderLayout.NORTH);
    }

    public ACAQProjectCompartment getCompartment() {
        return compartment;
    }
}
