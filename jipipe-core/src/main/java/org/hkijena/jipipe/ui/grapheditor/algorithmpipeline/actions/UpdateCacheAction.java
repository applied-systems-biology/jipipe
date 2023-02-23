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
