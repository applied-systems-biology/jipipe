package org.hkijena.jipipe.ui.grapheditor.actions;

public class UpdateCacheAction implements JIPipeNodeUIAction {
    private final boolean storeIntermediateResults;

    public UpdateCacheAction(boolean storeIntermediateResults) {
        this.storeIntermediateResults = storeIntermediateResults;
    }

    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }
}
