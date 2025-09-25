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
import org.hkijena.jipipe.plugins.tunnels.ui.JIPipeDesktopTunnelEntranceGraphNodeUI;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for all tunnel entrances
 */
public abstract class JIPipeDataFlowTunnelEntrance extends JIPipeDataFlowTunnel {

    public JIPipeDataFlowTunnelEntrance(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    public JIPipeDataFlowTunnelEntrance(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeDataFlowTunnelEntrance(JIPipeDataFlowTunnelEntrance other) {
        super(other);
    }

    @Override
    public Set<String> getCompatibleTunnelKeys() {
        // Find keys within the same group that don't have another input with the same name
        Set<String> result = new HashSet<>();

        JIPipeGraph graph = getParentGraph();
        if (graph != null) {
            Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(getCompartmentUUIDInParentGraph());

            // Collect from all outputs
            for (JIPipeGraphNode node : nodesWithinCompartment) {
                if (node instanceof JIPipeDataFlowTunnelExit tunnelExit && tunnelExit.isInSameGroup(this) && tunnelExit.hasValidTunnelKey()) {
                    result.add(tunnelExit.getTunnelKey());
                }
            }

            // Remove from existing inputs
            for (JIPipeGraphNode node : nodesWithinCompartment) {
                if (node instanceof JIPipeDataFlowTunnelEntrance tunnelEntrance && tunnelEntrance.isInSameGroup(this) && tunnelEntrance.hasValidTunnelKey()) {
                    result.remove(tunnelEntrance.getTunnelKey());
                }
            }

        }

        return result;
    }

    @Override
    public Class<? extends JIPipeDesktopGraphNodeUI> getNodeUiClass() {
        return JIPipeDesktopTunnelEntranceGraphNodeUI.class;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {

    }
}
