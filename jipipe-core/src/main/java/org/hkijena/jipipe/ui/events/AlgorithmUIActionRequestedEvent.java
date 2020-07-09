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

package org.hkijena.jipipe.ui.events;

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;

/**
 * An action that is requested by an {@link JIPipeNodeUI} and passed down to a {@link JIPipeGraphEditorUI}
 */
public class AlgorithmUIActionRequestedEvent {
    private final JIPipeNodeUI ui;
    private final Object action;

    /**
     * Initializes a new instance
     *
     * @param ui     the requesting UI
     * @param action the action parameter
     */
    public AlgorithmUIActionRequestedEvent(JIPipeNodeUI ui, Object action) {
        this.ui = ui;
        this.action = action;
    }

    public JIPipeNodeUI getUi() {
        return ui;
    }

    public Object getAction() {
        return action;
    }
}
