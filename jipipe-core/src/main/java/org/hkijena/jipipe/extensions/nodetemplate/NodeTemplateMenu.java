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

import com.fasterxml.jackson.core.JsonProcessingException;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;

public class NodeTemplateMenu extends JMenu implements JIPipeWorkbenchAccess, NodeTemplatesRefreshedEventListener {
    private final JIPipeWorkbench workbench;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final JIPipeProject project;

    public NodeTemplateMenu(JIPipeWorkbench workbench, JIPipeGraphEditorUI graphEditorUI) {
        this.workbench = workbench;
        this.graphEditorUI = graphEditorUI;
        if (workbench instanceof JIPipeProjectWorkbench) {
            this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        } else {
            this.project = null;
        }
        setText("Templates");
        setIcon(UIUtils.getIconFromResources("actions/starred.png"));
        reloadTemplateList();
        JIPipe.getInstance().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    private void reloadTemplateList() {
        List<JIPipeNodeTemplate> templates = new ArrayList<>(NodeTemplateSettings.getInstance().getNodeTemplates());
        if (project != null) {
            templates.addAll(project.getMetadata().getNodeTemplates());
        }
        setVisible(!templates.isEmpty());
        removeAll();
        if (!templates.isEmpty()) {
            Map<String, Set<JIPipeNodeTemplate>> byMenuPath = JIPipeNodeTemplate.groupByMenuPaths(templates);
            Map<String, JMenu> menuTree = UIUtils.createMenuTree(this, byMenuPath.keySet());
            for (Map.Entry<String, Set<JIPipeNodeTemplate>> entry : byMenuPath.entrySet()) {
                JMenu subMenu = menuTree.get(entry.getKey());
                entry.getValue().stream().sorted(Comparator.comparing(JIPipeNodeTemplate::getName)).forEach(template -> {
                    ImageIcon icon = UIUtils.getIconFromResources(template.getIcon().getIconName());
                    JMenuItem item = new JMenuItem(template.getName(), icon);
                    item.setToolTipText("<html>" + TooltipUtils.getAlgorithmTooltip(template, true) + "</html>");
                    item.addActionListener(e -> addTemplateIntoGraph(template));
                    subMenu.add(item);
                });
            }
        }
    }

    private void addTemplateIntoGraph(JIPipeNodeTemplate template) {
        try {
            graphEditorUI.getCanvasUI().pasteNodes(template.getData());
        } catch (JsonProcessingException e) {
            IJ.handleException(e);
        }
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        reloadTemplateList();
    }
}
