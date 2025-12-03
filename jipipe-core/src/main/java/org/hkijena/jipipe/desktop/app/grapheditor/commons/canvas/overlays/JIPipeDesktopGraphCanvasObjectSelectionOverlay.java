package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;

public class JIPipeDesktopGraphCanvasObjectSelectionOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasObjectSelectionOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {
        // Draw node selections and lock
        g.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_SELECTION);
        for (JIPipeDesktopGraphInteractiveObjectUI ui : canvasUI.getSelectionManager().getSelection()) {
            if (ui instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                Rectangle bounds = nodeUI.getBounds();
                bounds.x -= 4;
                bounds.y -= 4;
                bounds.width += 8;
                bounds.height += 8;
                g.setColor(nodeUI.getNodeBorderColor());
                g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

                // Lock icon
                if (nodeUI.getNode().isUiLocked()) {
                    g.fillRect(bounds.x - 1, bounds.y - 1, 22, 22);
                    g.drawImage(canvasUI.getResources().getLockIcon().getImage(), bounds.x + 2, bounds.y + 2, 16, 16, null);
                }

                // Layer Z (annotations)
                if (nodeUI.getNode() instanceof JIPipeGraphCanvasNote) {
                    int zLayer = ((JIPipeGraphCanvasNote) nodeUI.getNode()).getzOrder();
                    g.setFont(JIPipeDesktopGraphCanvasResources.GRAPH_TOOL_CURSOR_FONT);
                    FontMetrics fontMetrics = g.getFontMetrics();
                    String text = "z " + zLayer;
                    int rawStringWidth = fontMetrics.stringWidth(text);
                    int indicatorWidth = rawStringWidth + 8;
                    int xStart = bounds.x + bounds.width + 4 - indicatorWidth - 1 - 8 - 4;
                    int yStart = bounds.y + bounds.height - 22 - 8;

                    g.fillRoundRect(xStart, yStart, indicatorWidth, 22, 4, 4);
                    g.setColor(Color.WHITE);

                    g.drawString(text, xStart + indicatorWidth / 2 - rawStringWidth / 2, yStart + (fontMetrics.getAscent() - fontMetrics.getLeading()) + 22 / 2 - fontMetrics.getHeight() / 2);
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }
}
