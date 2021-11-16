package org.hkijena.jipipe.ui.nodetemplate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

public class JIPipeNodeTemplateParameterEditorUI extends JIPipeParameterEditorUI {

    private JButton infoButton;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public JIPipeNodeTemplateParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editButton = new JButton("Edit template", UIUtils.getIconFromResources("actions/edit.png"));
        editButton.addActionListener(e -> editParameters());
        add(editButton, BorderLayout.EAST);
        infoButton = new JButton();
        infoButton.setHorizontalAlignment(SwingConstants.LEFT);
        add(infoButton, BorderLayout.CENTER);
    }

    private void editParameters() {

    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeNodeTemplate parameter = getParameter(JIPipeNodeTemplate.class);
        JIPipeNodeInfo nodeInfo = parameter.getNodeInfo();
        if(nodeInfo != null) {
            infoButton.setText("<html>" + parameter.getName() + "<br/><i>" + nodeInfo.getName() + "</i></html>");
            infoButton.setIcon(JIPipe.getNodes().getIconFor(nodeInfo));
        }
        else {
            infoButton.setText("<html>" + parameter.getName() + "<br/><i><span style=\"color: red;\">Unable to load</span></i></html>");
            infoButton.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }
}
