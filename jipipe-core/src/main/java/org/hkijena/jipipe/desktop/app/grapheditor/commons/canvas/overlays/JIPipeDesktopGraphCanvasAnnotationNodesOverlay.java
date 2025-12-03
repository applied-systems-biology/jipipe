package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Responsible for drawing {@link JIPipeGraphCanvasNote}, as those come with a custom rendering that ensures that they are always rendered below all other nodes
 */
public class JIPipeDesktopGraphCanvasAnnotationNodesOverlay implements JIPipeDesktopGraphCanvasOverlay {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasAnnotationNodesOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {

    }

    @Override
    public void paintComponent(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();
        for (int i = canvasUI.getComponentCount() - 1; i >= 0; i--) {
            Component component = canvasUI.getComponent(i);
            // Draw annotation
            if (component instanceof JIPipeDesktopAnnotationGraphNodeUI annotationGraphNodeUI) {

                // Set render settings (HQ)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                g.translate(annotationGraphNodeUI.getX(), annotationGraphNodeUI.getY());
                JIPipeGraphCanvasNote node = (JIPipeGraphCanvasNote) (annotationGraphNodeUI).getNode();
                if (node.isDrawWithAntialiasing()) {
                    try {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        node.paintNode(g, annotationGraphNodeUI, canvasUI.getZoom());
                    } finally {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    }
                } else {
                    node.paintNode(g, annotationGraphNodeUI, canvasUI.getZoom());
                }
                g.setTransform(originalTransform);
            }

        }
    }
}
