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

package org.hkijena.jipipe.extensions.nodetemplate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeNodeTemplateParameterEditorUI extends JIPipeParameterEditorUI {

    private JButton infoButton;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public JIPipeNodeTemplateParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editButton = new JButton("Edit template", UIUtils.getIconFromResources("actions/edit.png"));
        editButton.addActionListener(e -> editParameters());
        add(editButton, BorderLayout.EAST);
        infoButton = new JButton();
        infoButton.addActionListener(e -> editParameters());
        infoButton.setHorizontalAlignment(SwingConstants.LEFT);
        add(infoButton, BorderLayout.CENTER);
    }

    private void editParameters() {
        JIPipeNodeTemplate parameter = new JIPipeNodeTemplate(getParameter(JIPipeNodeTemplate.class));
        if (ParameterPanel.showDialog(getWorkbench(), parameter, new MarkdownDocument("# Node templates\n\nUse this user interface to modify node templates."), "Edit template",
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION)) {
            setParameter(parameter, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeNodeTemplate parameter = getParameter(JIPipeNodeTemplate.class);
        JIPipeGraph graph = parameter.getGraph();
        if (graph != null) {
            if (graph.getGraphNodes().size() == 1) {
                JIPipeNodeInfo nodeInfo = graph.getGraphNodes().iterator().next().getInfo();
                infoButton.setText("<html>" + parameter.getName() + "<br/><i>" + nodeInfo.getName() + "</i></html>");
                infoButton.setIcon(JIPipe.getNodes().getIconFor(nodeInfo));
            } else {
                infoButton.setText("<html>" + parameter.getName() + "<br/><i>" + graph.getGraphNodes().size() + " nodes</i></html>");
                infoButton.setIcon(UIUtils.getIconFromResources("actions/distribute-graph.png"));
            }
        } else {
            infoButton.setText("<html>" + parameter.getName() + "<br/><i><span style=\"color: red;\">Unable to load</span></i></html>");
            infoButton.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }
}
