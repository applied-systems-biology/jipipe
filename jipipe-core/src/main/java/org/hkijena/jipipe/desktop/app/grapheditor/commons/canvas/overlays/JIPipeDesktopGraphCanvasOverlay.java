package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import java.awt.*;

public interface JIPipeDesktopGraphCanvasOverlay {
    void paint(Graphics2D g);

    void paintComponent(Graphics2D g);
}
