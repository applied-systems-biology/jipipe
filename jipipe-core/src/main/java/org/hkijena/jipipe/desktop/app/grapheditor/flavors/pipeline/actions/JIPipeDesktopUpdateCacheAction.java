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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.actions.JIPipeDesktopNodeUIAction;

public class JIPipeDesktopUpdateCacheAction implements JIPipeDesktopNodeUIAction {
    private final boolean storeIntermediateResults;

    private final boolean onlyPredecessors;
    private final boolean allowChangePanels;

    public JIPipeDesktopUpdateCacheAction(boolean storeIntermediateResults, boolean onlyPredecessors, boolean allowChangePanels) {
        this.storeIntermediateResults = storeIntermediateResults;
        this.onlyPredecessors = onlyPredecessors;
        this.allowChangePanels = allowChangePanels;
    }

    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }

    public boolean isOnlyPredecessors() {
        return onlyPredecessors;
    }

    public boolean isAllowChangePanels() {
        return allowChangePanels;
    }
}
