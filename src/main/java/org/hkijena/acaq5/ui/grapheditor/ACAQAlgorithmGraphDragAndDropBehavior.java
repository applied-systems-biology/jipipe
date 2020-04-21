package org.hkijena.acaq5.ui.grapheditor;

import java.awt.dnd.DropTargetListener;

/**
 * Provides Drag & Drop behavior for {@link ACAQAlgorithmGraphCanvasUI}
 */
public interface ACAQAlgorithmGraphDragAndDropBehavior extends DropTargetListener {
    /**
     * Returns the canvas the contains the algorithm nodes
     *
     * @return canvas the contains the algorithm nodes
     */
    ACAQAlgorithmGraphCanvasUI getCanvas();

    /**
     * Sets the canvas that contains the algorithm nodes
     *
     * @param canvas the canvas
     */
    void setCanvas(ACAQAlgorithmGraphCanvasUI canvas);
}
