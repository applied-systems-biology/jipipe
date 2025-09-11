package org.hkijena.jipipe.plugins.tunnels.nodes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.tunnels.ui.JIPipeDesktopTunnelExitGraphNodeUI;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for all tunnel exits
 */
public abstract class JIPipeDataFlowTunnelExit extends JIPipeDataFlowTunnel {

    public JIPipeDataFlowTunnelExit(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    public JIPipeDataFlowTunnelExit(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeDataFlowTunnelExit(JIPipeDataFlowTunnelExit other) {
        super(other);
    }

    @Override
    public Set<String> getCompatibleTunnelKeys() {
        // Find valid input keys
        Set<String> result = new HashSet<>();

        JIPipeGraph graph = getParentGraph();
        if (graph != null) {
            Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(getCompartmentUUIDInParentGraph());

            // Remove from existing inputs
            for (JIPipeGraphNode node : nodesWithinCompartment) {
                if (node instanceof JIPipeDataFlowTunnelEntrance tunnelEntrance && tunnelEntrance.isInSameGroup(this) && tunnelEntrance.hasValidTunnelKey()) {
                    result.add(tunnelEntrance.getTunnelKey());
                }
            }

        }

        return result;
    }

    @Override
    public Class<? extends JIPipeDesktopGraphNodeUI> getNodeUiClass() {
        return JIPipeDesktopTunnelExitGraphNodeUI.class;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report) {

    }
}
