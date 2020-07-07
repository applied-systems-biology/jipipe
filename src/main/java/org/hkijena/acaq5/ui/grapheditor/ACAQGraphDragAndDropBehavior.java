/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.ui.grapheditor;

import java.awt.dnd.DropTargetListener;

/**
 * Provides Drag & Drop behavior for {@link ACAQGraphCanvasUI}
 */
public interface ACAQGraphDragAndDropBehavior extends DropTargetListener {
    /**
     * Returns the canvas the contains the algorithm nodes
     *
     * @return canvas the contains the algorithm nodes
     */
    ACAQGraphCanvasUI getCanvas();

    /**
     * Sets the canvas that contains the algorithm nodes
     *
     * @param canvas the canvas
     */
    void setCanvas(ACAQGraphCanvasUI canvas);
}
