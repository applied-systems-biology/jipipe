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

package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

/**
 * Generated when an algorithm is selected
 */
public class AlgorithmSelectedEvent extends AlgorithmEvent {
    private boolean addToSelection;

    /**
     * @param ui             the algorithm UI
     * @param addToSelection if the algorithm should be added to the selection
     */
    public AlgorithmSelectedEvent(ACAQAlgorithmUI ui, boolean addToSelection) {
        super(ui);
        this.addToSelection = addToSelection;
    }

    public boolean isAddToSelection() {
        return addToSelection;
    }
}
