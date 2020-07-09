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

import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;

/**
 * An event around {@link ACAQNodeUI}
 */
public class AlgorithmEvent {
    private ACAQNodeUI ui;

    /**
     * Creates a new event
     *
     * @param ui the algorithm
     */
    public AlgorithmEvent(ACAQNodeUI ui) {
        this.ui = ui;
    }

    public ACAQNodeUI getUi() {
        return ui;
    }
}
