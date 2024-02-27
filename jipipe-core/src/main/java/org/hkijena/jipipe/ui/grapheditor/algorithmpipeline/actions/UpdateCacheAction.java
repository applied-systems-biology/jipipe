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

package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions;

import org.hkijena.jipipe.ui.grapheditor.general.actions.JIPipeNodeUIAction;

public class UpdateCacheAction implements JIPipeNodeUIAction {
    private final boolean storeIntermediateResults;

    private final boolean onlyPredecessors;

    public UpdateCacheAction(boolean storeIntermediateResults, boolean onlyPredecessors) {
        this.storeIntermediateResults = storeIntermediateResults;
        this.onlyPredecessors = onlyPredecessors;
    }

    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }

    public boolean isOnlyPredecessors() {
        return onlyPredecessors;
    }
}
