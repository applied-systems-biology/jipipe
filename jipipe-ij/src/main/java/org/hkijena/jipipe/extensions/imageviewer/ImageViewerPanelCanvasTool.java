package org.hkijena.jipipe.extensions.imageviewer;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import javax.swing.*;
import java.awt.*;

/**
 * Tool for the {@link ImageViewerPanelCanvas}
 */
public interface ImageViewerPanelCanvasTool {
    /**
     * Cursor for the tool
     *
     * @return the cursor
     */
    Cursor getToolCursor();

    /**
     * Executed on activation of this tool
     *
     * @param canvas the canvas
     */
    void onToolActivate(ImageViewerPanelCanvas canvas);

    /**
     * Executed on deactivation of this tool
     *
     * @param canvas the canvas
     */
    void onToolDeactivate(ImageViewerPanelCanvas canvas);

    /**
     * Returns true if left mouse dragging should be allowed
     *
     * @return if left mouse dragging should be allowed
     */
    default boolean toolAllowLeftMouseDrag() {
        return false;
    }

    /**
     * Returns true if the tool is active
     *
     * @param canvas the canvas
     * @return if the tool is active
     */
    default boolean toolIsActive(ImageViewerPanelCanvas canvas) {
        return canvas.getTool() == this;
    }

    /**
     * Called after the image has been drawn after the image has been drawn
     *
     * @param graphics2D the graphics
     * @param renderArea the render area
     * @param sliceIndex the index of the slice
     */
    default void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {

    }

    /**
     * Name of the tool
     *
     * @return the name
     */
    String getToolName();

    /**
     * Adds events to {@link JToggleButton} that acts as control for the tool
     *
     * @param toggleButton the toggle button
     */
    default void addToggleButton(JToggleButton toggleButton, ImageViewerPanelCanvas canvas) {
        toggleButton.setSelected(toolIsActive(canvas));
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected())
                canvas.setTool(this);
            else
                canvas.setTool(null);
        });
        canvas.getEventBus().register(new Object() {
            @Subscribe
            public void onToolChanged(ImageViewerPanelCanvas.ToolChangedEvent event) {
                toggleButton.setSelected(toolIsActive(canvas));
            }
        });
    }
}
