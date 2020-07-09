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

package org.hkijena.pipelinej.ui.events;

import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;

/**
 * Triggered when An {@link ACAQGraphCanvasUI} selection was changed
 */
public class AlgorithmSelectionChangedEvent {
    private ACAQGraphCanvasUI canvasUI;

    /**
     * @param canvasUI the canvas that triggered the event
     */
    public AlgorithmSelectionChangedEvent(ACAQGraphCanvasUI canvasUI) {

        this.canvasUI = canvasUI;
    }

    public ACAQGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }
}
