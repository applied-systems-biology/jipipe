package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.api.grapheditortool.JIPipeDefaultGraphEditorTool;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

import java.awt.*;

public class JIPipeDesktopGraphCanvasToolInfoOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasToolInfoOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {
        // Draw cursor info
        if (canvasUI.isMouseIsEntered() && canvasUI.getLastMousePosition() != null && !canvasUI.getToolManager().hasDefaultTool()
                && canvasUI.getSettings().isShowToolInfo() && !(canvasUI.getToolManager().getCurrentTool() instanceof JIPipeDefaultGraphEditorTool)) {
            canvasUI.getToolManager().getCurrentTool().paintMouse(canvasUI, canvasUI.getLastMousePosition(), canvasUI.getSettings().getToolInfoDistance(), g);
        }
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }
}
