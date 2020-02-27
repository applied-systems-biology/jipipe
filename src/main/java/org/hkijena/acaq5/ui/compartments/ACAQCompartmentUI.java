package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphUI;

import javax.swing.*;
import java.awt.BorderLayout;

public class ACAQCompartmentUI extends ACAQUIPanel {

    private ACAQProjectCompartment compartment;
    private ACAQAlgorithmGraphUI graphUI;

    public ACAQCompartmentUI(ACAQWorkbenchUI workbenchUI, ACAQProjectCompartment compartment) {
        super(workbenchUI);
        this.compartment = compartment;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        graphUI = new ACAQAlgorithmGraphUI(getWorkbenchUI(), compartment.getProject().getGraph(), compartment.getName());
        add(graphUI, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//
//
//        add(toolBar, BorderLayout.NORTH);
    }
}
