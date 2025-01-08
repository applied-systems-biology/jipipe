/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.grouping.parameters;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.groups.JIPipeDesktopNodeGroupUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link NodeGroupContents}
 */
public class NodeGroupContentsDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public NodeGroupContentsDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editGraphButton = new JButton("Edit graph", UIUtils.getIconFromResources("actions/edit.png"));
        editGraphButton.addActionListener(e -> editGraph());
        add(editGraphButton, BorderLayout.CENTER);
    }

    private void editGraph() {
        NodeGroupContents contents = getParameter(NodeGroupContents.class);
        SwingUtilities.invokeLater(() -> JIPipeDesktopNodeGroupUI.openGroupNodeGraph(getDesktopWorkbench(), contents.getParent(), true));
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
