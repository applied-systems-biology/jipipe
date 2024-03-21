/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d;

import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import javax.swing.*;
import java.awt.*;

/**
 * Tool for the {@link ImageViewerPanelCanvas2D}
 */
public interface ImageViewerPanelCanvas2DTool {
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
    void onToolActivate(ImageViewerPanelCanvas2D canvas);

    /**
     * Executed on deactivation of this tool
     *
     * @param canvas the canvas
     */
    void onToolDeactivate(ImageViewerPanelCanvas2D canvas);

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
    default boolean toolIsActive(ImageViewerPanelCanvas2D canvas) {
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
    default void addToggleButton(JToggleButton toggleButton, ImageViewerPanelCanvas2D canvas) {
        toggleButton.setSelected(toolIsActive(canvas));
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected())
                canvas.setTool(this);
            else
                canvas.setTool(null);
        });
        canvas.getToolChangedEventEmitter().subscribeLambda((emitter, event) -> {
            toggleButton.setSelected(toolIsActive(canvas));
        });
    }
}
