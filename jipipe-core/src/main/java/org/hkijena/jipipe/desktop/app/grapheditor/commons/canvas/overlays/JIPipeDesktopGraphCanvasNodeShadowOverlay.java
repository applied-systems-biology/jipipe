package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;

public class JIPipeDesktopGraphCanvasNodeShadowOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasNodeShadowOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {

    }

    @Override
    public void paintComponent(Graphics2D g) {
        boolean finalDrawShadows = canvasUI.getSettings().isDrawNodeShadows();
        for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
            // Draw shadow
            if (finalDrawShadows) {

                // Set render settings (LQ)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                if (ui.isDrawShadow()) {
                    JIPipeDesktopGraphCanvasResources.DROP_SHADOW_BORDER.paint(g, ui.getX() - 3, ui.getY() - 3, ui.getWidth() + 8, ui.getHeight() + 8);
                }
                if (ui.getNode().isBookmarked()) {
                    JIPipeDesktopGraphCanvasResources.BOOKMARK_SHADOW_BORDER.paint(g, ui.getX() - 12, ui.getY() - 12, ui.getWidth() + 24, ui.getHeight() + 24);
                }
            }
        }
    }
}
