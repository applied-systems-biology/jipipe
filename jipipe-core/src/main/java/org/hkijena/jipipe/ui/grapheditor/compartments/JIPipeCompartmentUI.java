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

package org.hkijena.jipipe.ui.grapheditor.compartments;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.grapheditor.JIPipePipelineGraphEditorUI;

import java.awt.BorderLayout;

/**
 * Editor for one project compartment graph
 * Contains a {@link JIPipePipelineGraphEditorUI} instance that allows editing the compartment's content
 */
public class JIPipeCompartmentUI extends JIPipeProjectWorkbenchPanel {

    private JIPipeProjectCompartment compartment;
    private JIPipePipelineGraphEditorUI graphUI;

    /**
     * Creates a new editor
     *
     * @param workbenchUI the workbench UI
     * @param compartment the compartment
     */
    public JIPipeCompartmentUI(JIPipeProjectWorkbench workbenchUI, JIPipeProjectCompartment compartment) {
        super(workbenchUI);
        this.compartment = compartment;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        graphUI = new JIPipePipelineGraphEditorUI(getProjectWorkbench(), compartment.getProject().getGraph(), compartment.getProjectCompartmentUUID());
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
    public JIPipeProjectCompartment getCompartment() {
        return compartment;
    }
}
