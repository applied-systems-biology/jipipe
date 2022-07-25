package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions;

import org.hkijena.jipipe.ui.grapheditor.general.actions.JIPipeNodeUIAction;

public class UpdateCacheAction implements JIPipeNodeUIAction {
    private final boolean storeIntermediateResults;

    public UpdateCacheAction(boolean storeIntermediateResults) {
        this.storeIntermediateResults = storeIntermediateResults;
    }

    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }
}
