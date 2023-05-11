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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class NodeContextActionWrapperUIContextAction implements NodeUIContextAction {

    private final JIPipeNodeInfo nodeInfo;
    private final String name;
    private final String description;
    private final Icon icon;
    private final Method method;

    public NodeContextActionWrapperUIContextAction(JIPipeNodeInfo nodeInfo, String name, String description, Icon icon, Method method) {
        this.nodeInfo = nodeInfo;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.method = method;
    }

    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if (selection.isEmpty())
            return false;
        for (JIPipeNodeUI ui : selection) {
            if (ui.getNode().getInfo() != nodeInfo)
                return false;
        }
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        try {
            for (JIPipeNodeUI ui : selection) {
                method.invoke(ui.getNode(), canvasUI.getWorkbench());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public Method getMethod() {
        return method;
    }

}
