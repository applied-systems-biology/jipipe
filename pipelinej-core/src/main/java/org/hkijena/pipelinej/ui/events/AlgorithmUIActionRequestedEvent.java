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

import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphEditorUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;

/**
 * An action that is requested by an {@link ACAQNodeUI} and passed down to a {@link ACAQGraphEditorUI}
 */
public class AlgorithmUIActionRequestedEvent {
    private final ACAQNodeUI ui;
    private final Object action;

    /**
     * Initializes a new instance
     *
     * @param ui     the requesting UI
     * @param action the action parameter
     */
    public AlgorithmUIActionRequestedEvent(ACAQNodeUI ui, Object action) {
        this.ui = ui;
        this.action = action;
    }

    public ACAQNodeUI getUi() {
        return ui;
    }

    public Object getAction() {
        return action;
    }
}
