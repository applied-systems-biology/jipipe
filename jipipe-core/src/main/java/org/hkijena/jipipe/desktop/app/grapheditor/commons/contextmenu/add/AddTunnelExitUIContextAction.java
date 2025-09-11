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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.add;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.DefaultNodeUIActionRequestedEvent;
import org.hkijena.jipipe.plugins.tunnels.nodes.tunnel1.JIPipeDataFlowTunnel1Exit;

import javax.swing.*;
import java.util.Set;
import java.util.UUID;

public class AddTunnelExitUIContextAction implements GraphInteractiveObjectUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        JIPipeDataFlowTunnel1Exit node = JIPipe.createNode(JIPipeDataFlowTunnel1Exit.class);
        UUID uuid = canvasUI.getGraph().insertNode(node, canvasUI.getCompartmentUUID());
        SwingUtilities.invokeLater(() -> {
            node.onDefaultNodeUIActionRequested(canvasUI.getGraphEditorUI(), new DefaultNodeUIActionRequestedEvent(canvasUI.getNodeUI(uuid)));
        });
    }

    @Override
    public String getName() {
        return "Add tunnel exit here ...";
    }


    @Override
    public String getDescription() {
        return "Adds a tunnel exit node and opens its configuration window";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/xfce-wm-stick.png");
    }
}
