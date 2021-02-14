package org.hkijena.jipipe.ui.grapheditor.actions;

public class RunAndShowResultsAction implements JIPipeNodeUIAction {
    private final boolean storeIntermediateResults;

    public RunAndShowResultsAction(boolean storeIntermediateResults) {
        this.storeIntermediateResults = storeIntermediateResults;
    }

    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }
}
