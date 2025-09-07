package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

import java.awt.*;

public class JIPipeDesktopGraphCanvasSelectionBoxOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasSelectionBoxOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {
        // Paint selection box
        canvasUI.getSelectionBoxManager().paint(g);
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }
}
