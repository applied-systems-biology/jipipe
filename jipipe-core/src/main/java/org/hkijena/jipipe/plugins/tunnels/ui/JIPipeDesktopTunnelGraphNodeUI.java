package org.hkijena.jipipe.plugins.tunnels.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.plugins.tunnels.JIPipeDataFlowTunnelUtils;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnel;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import java.awt.*;
import java.util.*;
import java.util.List;

public class JIPipeDesktopTunnelGraphNodeUI extends JIPipeDesktopGraphNodeUI {

    private Map<String, JIPipeDataInfo> lastDataTypes = new HashMap<>();
    private long lastDataTypesRecalculated = 0;

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

    public String getTunnelKey() {
        return ((JIPipeDataFlowTunnel)getNode()).getTunnelKey();
    }

    public String getTunnelKeyGroup() {
        return ((JIPipeDataFlowTunnel)getNode()).getTunnelKeyGroup();
    }

    @Override
    public Color getNodeFillColor() {
        float hue = getTunnelColorHue();
        return Color.getHSBColor(hue, Math.min(1, ThemeUtils.getCurrentStyle().getNodeFillSaturation() * 3f), ThemeUtils.getCurrentStyle().getNodeFillBrightness());
    }

    @Override
    public Color getNodeBorderColor() {
        float hue = getTunnelColorHue();
        return ThemeUtils.getNodeBorderColor(hue);
    }

    private float getTunnelColorHue() {
        return JIPipeDataFlowTunnelUtils.getTunnelColorHue(getGraphCanvasUI(), getTunnelKeyGroup(), getTunnelKey());
    }

    private Map<String, JIPipeDataInfo> getLastDataTypes() {
        long currentTime = System.currentTimeMillis();
        if(currentTime - lastDataTypesRecalculated > 60) {
            lastDataTypes = JIPipeDataFlowTunnelUtils.findTunnelDataTypes(getGraphCanvasUI().getGraph(),
                    getGraphCanvasUI().getCompartmentUUID(),
                    getTunnelKeyGroup(),
                    getTunnelKey());
            lastDataTypesRecalculated = currentTime;
        }
        return lastDataTypes;
    }

    @Override
    public Color getSlotFillColor() {
        float hue = getTunnelColorHue();
        return Color.getHSBColor(hue, ThemeUtils.getCurrentStyle().getNodeFillSaturation(), ThemeUtils.getCurrentStyle().getNodeFillBrightness());
    }

    @Override
    protected boolean isDrawSlotIndicators() {
        return false;
    }

    @Override
    protected int getGridHeight() {
        return 2;
    }

    @Override
    public Image getNodeIcon() {
        return tunnelIsValid() ? JIPipe.RESOURCES.getIcon16("actions/key.png").getImage() : JIPipe.RESOURCES.getIcon16("emblems/warning.png").getImage();
    }

    public boolean tunnelIsValid() {
        return !StringUtils.isNullOrEmpty(getNode().getCustomName());
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
            Map<String, JIPipeDataInfo> updatedDataTypes = getLastDataTypes();
            JIPipeDataInfo dataType = updatedDataTypes.getOrDefault(slotState.getSlotName(), null);
            if(dataType == null || dataType.getDataClass() == JIPipeData.class) {
                return JIPipe.RESOURCES.getIcon16("actions/xfce-wm-stick.png").getImage();
            }
            else {
                return JIPipe.getDataTypes().getIconFor(dataType.getDataClass()).getImage();
            }
        }
    }
}
