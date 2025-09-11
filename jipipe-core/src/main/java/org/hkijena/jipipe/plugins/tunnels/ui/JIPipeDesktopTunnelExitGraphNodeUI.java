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

package org.hkijena.jipipe.plugins.tunnels.ui;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;

public class JIPipeDesktopTunnelExitGraphNodeUI extends JIPipeDesktopTunnelGraphNodeUI {

    /**
     * Creates a new UI
     *
     * @param workbench     the workbench
     * @param graphCanvasUI The graph UI that contains this UI
     * @param node          The algorithm
     */
    public JIPipeDesktopTunnelExitGraphNodeUI(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphCanvasUI graphCanvasUI, JIPipeGraphNode node) {
        super(workbench, graphCanvasUI, node);
        if(!JIPipeDataFlowTunnelExit.class.isAssignableFrom(node.getClass())) {
            throw new IllegalArgumentException("Node must be a JIPipeDataFlowTunnelExit");
        }
    }

    @Override
    protected int getOutputSlotYLocation() {
        return JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT;
    }

    @Override
    protected String getDisplayedSlotLabel(JIPipeDesktopGraphNodeUISlotActiveArea slotState) {
        return "Out";
    }
}
