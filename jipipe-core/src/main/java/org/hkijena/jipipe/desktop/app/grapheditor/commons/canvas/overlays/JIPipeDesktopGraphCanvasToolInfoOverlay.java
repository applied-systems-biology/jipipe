package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.api.grapheditortool.JIPipeDefaultGraphEditorTool;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;

public class JIPipeDesktopGraphCanvasToolInfoOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasToolInfoOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw cursor info
        Rectangle visibleRect = canvasUI.getVisibleRect();
        if (visibleRect != null) {

            if (!canvasUI.getToolManager().hasDefaultTool() && canvasUI.getSettings().isShowToolInfo() && !(canvasUI.getToolManager().getCurrentTool() instanceof JIPipeDefaultGraphEditorTool)) {
                Color toolColor = ThemeUtils.getCurrentStyle().getPrimaryColor();
                if (canvasUI.getGraphEditorUI() != null) {
                    int index = canvasUI.getGraphEditorUI().getTools().indexOf(canvasUI.getToolManager().getCurrentTool());
                    if (index >= 0) {
                        toolColor = Color.getHSBColor(index * 1.0f / canvasUI.getGraphEditorUI().getTools().size(),
                                ThemeUtils.getCurrentStyle().getNodeFillSaturation(),
                                ThemeUtils.getCurrentStyle().getNodeFillBrightness());
                    }
                }

                g.setPaint(canvasUI.getBackground());
                g.fillRect(visibleRect.x, visibleRect.y, visibleRect.width, 16);
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
                g.setPaint(toolColor);
                g.drawLine(visibleRect.x + 4, visibleRect.y + 8, visibleRect.x + visibleRect.width - 8, visibleRect.y + 8);

                canvasUI.getToolManager().getCurrentTool().paintTooltip(canvasUI, new Point(visibleRect.x, visibleRect.y + 16), 0, false, g);
            }
        } else {
            if (canvasUI.isMouseIsEntered() && canvasUI.getLastMousePosition() != null && !canvasUI.getToolManager().hasDefaultTool()
                    && canvasUI.getSettings().isShowToolInfo() && !(canvasUI.getToolManager().getCurrentTool() instanceof JIPipeDefaultGraphEditorTool)) {
                canvasUI.getToolManager().getCurrentTool().paintTooltip(canvasUI, canvasUI.getLastMousePosition(), canvasUI.getSettings().getToolInfoDistance(), true, g);
            }
        }


        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }
}
