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

package org.hkijena.jipipe.api.grouping.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameterreference.GraphNodeParameterReferenceGroupCollectionEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.OKCancelDialog;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link GraphNodeParameterReferenceGroupCollection}
 */
public class GraphNodeParameterReferenceGroupCollectionParameterEditorUI extends JIPipeParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public GraphNodeParameterReferenceGroupCollectionParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editGraphButton = new JButton("Edit parameter references", UIUtils.getIconFromResources("actions/edit.png"));
        editGraphButton.addActionListener(e -> editParameters());
        add(editGraphButton, BorderLayout.CENTER);
    }

    private void editParameters() {
        GraphNodeParameterReferenceGroupCollection original = getParameter(GraphNodeParameterReferenceGroupCollection.class);
        GraphNodeParameterReferenceGroupCollection copy = new GraphNodeParameterReferenceGroupCollection(original);
        copy.setGraph(original.getGraph());
        GraphNodeParameterReferenceGroupCollectionEditorUI parametersUI = new GraphNodeParameterReferenceGroupCollectionEditorUI(getWorkbench(), copy, null, false);
        if (OKCancelDialog.showDialog(getWorkbench().getWindow(), "Edit parameters", parametersUI, "OK", new Dimension(1024, 768))) {
            original.setParameterReferenceGroups(copy.getParameterReferenceGroups());
            setParameter(original, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
