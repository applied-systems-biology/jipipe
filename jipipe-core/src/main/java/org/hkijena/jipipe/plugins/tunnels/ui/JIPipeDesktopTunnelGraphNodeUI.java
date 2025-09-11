package org.hkijena.jipipe.plugins.tunnels.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class JIPipeDesktopTunnelGraphNodeUI extends JIPipeDesktopGraphNodeUI {
    /**
     * Creates a new UI
     *
     * @param workbench     thr workbench
     * @param graphCanvasUI The graph UI that contains this UI
     * @param node          The algorithm
     */
    public JIPipeDesktopTunnelGraphNodeUI(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphCanvasUI graphCanvasUI, JIPipeGraphNode node) {
        super(workbench, graphCanvasUI, node);
        setBuffered(false);
    }

    public float getTunnelColorHue() {
        Set<JIPipeGraphNode> nodesWithinCompartment = getGraphCanvasUI().getGraph().getNodesWithinCompartment(getGraphCanvasUI().getCompartmentUUID());
        List<String> tunnelKeys = new ArrayList<>();
        for (JIPipeGraphNode node : nodesWithinCompartment) {
            if(node instanceof JIPipeDataFlowTunnelEntrance || node instanceof JIPipeDataFlowTunnelExit) {
                String name = StringUtils.nullToEmpty(node.getCustomName());
                if(!tunnelKeys.contains(name)) {
                    tunnelKeys.add(name);
                }
            }
        }
        Collections.sort(tunnelKeys);
        if(tunnelKeys.isEmpty()) {
            return 0;
        }
        return tunnelKeys.indexOf(StringUtils.nullToEmpty(getNode().getCustomName())) * 1.0f / tunnelKeys.size();
    }

    @Override
    public Color getNodeFillColor() {
        return Color.getHSBColor(getTunnelColorHue(), Math.min(1, ThemeUtils.getCurrentStyle().getNodeFillSaturation() * 3f), ThemeUtils.getCurrentStyle().getNodeFillBrightness());
    }

    @Override
    public Color getNodeBorderColor() {
        return ThemeUtils.getNodeBorderColor(getTunnelColorHue());
    }

    @Override
    public Color getSlotFillColor() {
        return Color.getHSBColor(getTunnelColorHue(), ThemeUtils.getCurrentStyle().getNodeFillSaturation(), ThemeUtils.getCurrentStyle().getNodeFillBrightness());
    }

    public boolean tunnelIsValid() {
        return true;
    }

    @Override
    protected int getGridHeight() {
        return 2;
    }

    @Override
    public Image getNodeIcon() {
        return !StringUtils.isNullOrEmpty(getNode().getCustomName()) ? JIPipe.RESOURCES.getIcon16("actions/key.png").getImage() : JIPipe.RESOURCES.getIcon16("emblems/warning.png").getImage();
    }

    @Override
    protected String getDisplayedNodeName() {
        return StringUtils.orElse(getNode().getCustomName(), "Double-click to configure");
    }

    @Override
    public Font getNativeMainFont() {
        return new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal());
    }

    @Override
    protected Image getDisplayedSlotIcon(JIPipeDesktopGraphNodeUISlotActiveArea slotState) {
        if(slotState.isInput()) {
            return JIPipe.RESOURCES.getIcon16("actions/xfce-wm-unstick.png").getImage();
        }
        else {
            return JIPipe.RESOURCES.getIcon16("actions/xfce-wm-stick.png").getImage();
        }
    }
}
