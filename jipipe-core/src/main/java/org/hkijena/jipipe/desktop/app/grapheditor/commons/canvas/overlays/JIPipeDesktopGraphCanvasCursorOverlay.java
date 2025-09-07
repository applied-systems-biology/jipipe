package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphCanvasCursorOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasCursorOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {
        // Draw cursor over the components
        Point cursor = canvasUI.getGraphEditorCursor();
        if (cursor != null && canvasUI.isRenderCursor()) {
            ImageIcon cursorImage = canvasUI.getResources().getCursorImage();
            g.drawImage(cursorImage.getImage(),
                    cursor.x - cursorImage.getIconWidth() / 2,
                    cursor.y - cursorImage.getIconHeight() / 2,
                    null);
        }
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }
}
