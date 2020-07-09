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

import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;

/**
 * An event around {@link JIPipeNodeUI}
 */
public class AlgorithmEvent {
    private JIPipeNodeUI ui;

    /**
     * Creates a new event
     *
     * @param ui the algorithm
     */
    public AlgorithmEvent(JIPipeNodeUI ui) {
        this.ui = ui;
    }

    public JIPipeNodeUI getUi() {
        return ui;
    }
}
