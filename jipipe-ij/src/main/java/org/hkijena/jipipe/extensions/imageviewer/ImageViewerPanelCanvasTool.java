package org.hkijena.jipipe.extensions.imageviewer;

import java.awt.*;

/**
 * Tool for the {@link ImageViewerPanelCanvas}
 */
public interface ImageViewerPanelCanvasTool {
    /**
     * Cursor for the tool
     * @return the cursor
     */
    Cursor getToolCursor();

    /**
     * Executed on activation of this tool
     * @param canvas the canvas
     */
    void onToolActivate(ImageViewerPanelCanvas canvas);

    /**
     * Executed on deactivation of this tool
     * @param canvas the canvas
     */
    void onToolDeactivate(ImageViewerPanelCanvas canvas);

    /**
     * Returns true if left mouse dragging should be allowed
     * @return if left mouse dragging should be allowed
     */
    default boolean toolAllowLeftMouseDrag() {
        return false;
    }

    /**
     * Returns true if the tool is active
     * @param canvas the canvas
     * @return if the tool is active
     */
    default boolean toolIsActive(ImageViewerPanelCanvas canvas) {
        return canvas.getTool() == this;
    }

    /**
     * Name of the tool
     * @return the name
     */
    String getToolName();
}
