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

package org.hkijena.jipipe.plugins.nodetemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;

public class NodeTemplatePopupMenu extends JPopupMenu implements JIPipeDesktopWorkbenchAccess, NodeTemplatesRefreshedEventListener {
    private final JIPipeDesktopWorkbench workbench;
    private final AbstractJIPipeDesktopGraphEditorUI graphEditorUI;
    private final JIPipeProject project;

    public NodeTemplatePopupMenu(JIPipeDesktopWorkbench workbench, AbstractJIPipeDesktopGraphEditorUI graphEditorUI) {
        this.workbench = workbench;
        this.graphEditorUI = graphEditorUI;
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            this.project = workbench.getProject();
        } else {
            this.project = null;
        }
        reloadTemplateList();
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    private void reloadTemplateList() {
        List<JIPipeNodeTemplate> templates = new ArrayList<>(JIPipe.getNodeTemplates().getGlobalTemplates());
        if (project != null) {
            templates.addAll(project.getMetadata().getNodeTemplates());
        }
        setVisible(!templates.isEmpty());
        removeAll();
        if (!templates.isEmpty()) {
            Map<String, Set<JIPipeNodeTemplate>> byMenuPath = JIPipeNodeTemplate.groupByMenuPaths(templates);
            Map<String, JComponent> menuTree = UIUtils.createMenuTree(this, byMenuPath.keySet());
            for (Map.Entry<String, Set<JIPipeNodeTemplate>> entry : byMenuPath.entrySet()) {
                JComponent subMenu = menuTree.get(entry.getKey());
                entry.getValue().stream().sorted(Comparator.comparing(JIPipeNodeTemplate::getName)).forEach(template -> {
                    JMenuItem item = new JMenuItem(template.getName(), template.getIconImage());
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

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        reloadTemplateList();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
