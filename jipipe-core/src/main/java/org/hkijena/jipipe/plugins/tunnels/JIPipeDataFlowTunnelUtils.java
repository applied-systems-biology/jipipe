package org.hkijena.jipipe.plugins.tunnels;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JIPipeDataFlowTunnelUtils {
    public static JIPipeDataFlowTunnelEntrance findTunnelEntrance(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(compartmentUUID);
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnelEntrance tunnelEntrance && tunnelEntrance.getTunnelKeyGroup().equals(tunnelKeyGroup) && tunnelEntrance.getTunnelKey().equals(tunnelKey)) {
                return tunnelEntrance;
            }
        }
        return null;
    }

    public static Set<JIPipeDataFlowTunnelExit> findTunnelExits(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        Set<JIPipeDataFlowTunnelExit> result = new HashSet<>();
        Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(compartmentUUID);
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnelExit tunnelExit && tunnelExit.getTunnelKeyGroup().equals(tunnelKeyGroup) && tunnelExit.getTunnelKey().equals(tunnelKey)) {
                result.add(tunnelExit);
            }
        }
        return result;
    }

    public static JIPipeDataInfo findTunnelDataType(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        JIPipeDataFlowTunnelEntrance tunnelEntrance = findTunnelEntrance(graph, compartmentUUID, tunnelKeyGroup, tunnelKey);
        if(tunnelEntrance != null) {
            // Find connected inputs

        }

        return JIPipeDataInfo.getInstance(JIPipeData.class);
    }
}
