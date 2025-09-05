package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

import java.awt.*;

public class JIPipeDesktopGraphCanvasConnectionHighlightsOverlay implements JIPipeDesktopGraphCanvasOverlay {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasConnectionHighlightsOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {

    }

    @Override
    public void paintComponent(Graphics2D g) {
        canvasUI.getConnectionHighlightManager().paintDisconnectHighlight(g);
        canvasUI.getConnectionHighlightManager().paintConnectHighlight(g);
        canvasUI.getDragManagerConnect().paintCurrentlyDraggedConnection(g);
    }
}
