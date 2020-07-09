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

import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCompartmentUI;

import java.awt.*;

/**
 * Editor for one project compartment graph
 * Contains a {@link ACAQGraphCompartmentUI} instance that allows editing the compartment's content
 */
public class ACAQCompartmentUI extends ACAQProjectWorkbenchPanel {

    private ACAQProjectCompartment compartment;
    private ACAQGraphCompartmentUI graphUI;

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

        graphUI = new ACAQGraphCompartmentUI(getProjectWorkbench(), compartment.getProject().getGraph(), compartment.getProjectCompartmentId());
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
