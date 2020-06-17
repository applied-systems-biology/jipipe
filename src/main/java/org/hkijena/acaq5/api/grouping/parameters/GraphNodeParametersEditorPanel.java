package org.hkijena.acaq5.api.grouping.parameters;

import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Editor component for {@link GraphNodeParameters}
 */
public class GraphNodeParametersEditorPanel extends ACAQWorkbenchPanel {

    private final GraphNodeParameters parameters;

    /**
     * @param workbench the workbench
     * @param parameters the parameters to edit
     */
    public GraphNodeParametersEditorPanel(ACAQWorkbench workbench, GraphNodeParameters parameters) {
        super(workbench);
        this.parameters = parameters;

        setLayout(new BorderLayout());
        add(new JButton("aaaaaaa"));
    }
}
