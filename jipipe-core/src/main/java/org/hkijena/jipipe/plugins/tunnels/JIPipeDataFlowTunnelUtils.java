package org.hkijena.jipipe.plugins.tunnels;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnel;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

public class JIPipeDataFlowTunnelUtils {

    public static boolean tunnelEntranceIsUnique(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        boolean found = false;
        Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(compartmentUUID);
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnelEntrance tunnelEntrance && Objects.equals(tunnelEntrance.getTunnelKeyGroup(), tunnelKeyGroup) && Objects.equals(tunnelEntrance.getTunnelKey(), tunnelKey)) {
                if (found) {
                    return false;
                }
                found = true;
            }
        }
        return true;
    }

    public static List<JIPipeDataFlowTunnelEntrance> findTunnelEntrances(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        List<JIPipeDataFlowTunnelEntrance> result = new ArrayList<>();
        Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(compartmentUUID);
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnelEntrance tunnelEntrance && Objects.equals(tunnelEntrance.getTunnelKeyGroup(), tunnelKeyGroup) && Objects.equals(tunnelEntrance.getTunnelKey(), tunnelKey)) {
                result.add(tunnelEntrance);
            }
        }
        return result;
    }

    public static JIPipeDataFlowTunnelEntrance findTunnelEntrance(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(compartmentUUID);
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnelEntrance tunnelEntrance && Objects.equals(tunnelEntrance.getTunnelKeyGroup(), tunnelKeyGroup) && Objects.equals(tunnelEntrance.getTunnelKey(), tunnelKey)) {
                return tunnelEntrance;
            }
        }
        return null;
    }

    public static Set<JIPipeDataFlowTunnelExit> findTunnelExits(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        Set<JIPipeDataFlowTunnelExit> result = new HashSet<>();
        Set<JIPipeGraphNode> nodesWithinCompartment = graph.getNodesWithinCompartment(compartmentUUID);
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnelExit tunnelExit && Objects.equals(tunnelExit.getTunnelKeyGroup(), tunnelKeyGroup) && Objects.equals(tunnelExit.getTunnelKey(), tunnelKey)) {
                result.add(tunnelExit);
            }
        }
        return result;
    }

    public static JIPipeDataInfo findTunnelDataType(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        JIPipeDataFlowTunnelEntrance tunnelEntrance = findTunnelEntrance(graph, compartmentUUID, tunnelKeyGroup, tunnelKey);
        if (tunnelEntrance != null) {
            // Find connected inputs
            Set<Class<? extends JIPipeData>> sourceClasses = new HashSet<>();
            for (JIPipeInputDataSlot inputSlot : tunnelEntrance.getDataInputSlots()) {
                for (JIPipeDataSlot outputSlot : graph.getInputIncomingSourceSlots(inputSlot)) {
                    sourceClasses.add(outputSlot.getAcceptedDataType());
                }
            }

            Class<? extends JIPipeData> consensusDataType = JIPipe.getDataTypes().getConsensusDataType(sourceClasses);
            if (consensusDataType == null) {
                consensusDataType = JIPipeData.class;
            }
            return JIPipeDataInfo.getInstance(consensusDataType);
        }

        return JIPipeDataInfo.getInstance(JIPipeData.class);
    }

    public static Map<String, JIPipeDataInfo> findTunnelDataTypes(JIPipeGraph graph, UUID compartmentUUID, String tunnelKeyGroup, String tunnelKey) {
        Map<String, JIPipeDataInfo> result = new HashMap<>();
        JIPipeDataFlowTunnelEntrance tunnelEntrance = findTunnelEntrance(graph, compartmentUUID, tunnelKeyGroup, tunnelKey);
        if (tunnelEntrance != null) {
            // Find connected inputs
            for (JIPipeInputDataSlot inputSlot : tunnelEntrance.getDataInputSlots()) {
                Set<Class<? extends JIPipeData>> sourceClasses = new HashSet<>();
                for (JIPipeDataSlot outputSlot : graph.getInputIncomingSourceSlots(inputSlot)) {
                    sourceClasses.add(outputSlot.getAcceptedDataType());
                }
                Class<? extends JIPipeData> consensusDataType = JIPipe.getDataTypes().getConsensusDataType(sourceClasses);
                if (consensusDataType == null) {
                    consensusDataType = JIPipeData.class;
                }
                JIPipeDataInfo consensus = JIPipeDataInfo.getInstance(consensusDataType);
                result.put(inputSlot.getName(), consensus);
            }
        }
        return result;
    }

    public static float getTunnelColorHue(JIPipeDesktopGraphCanvasUI canvasUI, String tunnelKeyGroup, String tunnelKey) {
        if (StringUtils.isNullOrEmpty(tunnelKey)) {
            return 0;
        }
        Set<JIPipeGraphNode> nodesWithinCompartment = canvasUI.getGraph().getNodesWithinCompartment(canvasUI.getCompartmentUUID());
        List<String> tunnelKeys = new ArrayList<>();
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if (node instanceof JIPipeDataFlowTunnel tunnel && Objects.equals(tunnelKeyGroup, tunnel.getTunnelKeyGroup()) && tunnel.hasValidTunnelKey()) {
                String name = StringUtils.nullToEmpty(node.getCustomName());
                if (!tunnelKeys.contains(name)) {
                    tunnelKeys.add(name);
                }
            }
        }
        Collections.sort(tunnelKeys);
        if (tunnelKeys.isEmpty()) {
            return 0;
        }

        final float startHue = 0.1f;
        final float endHue = 1 - startHue;
        float hue = tunnelKeys.indexOf(tunnelKey) * 1.0f / tunnelKeys.size();
        hue = startHue + (endHue - startHue) * hue;
        return Math.max(0, Math.min(1, hue));
    }
}
