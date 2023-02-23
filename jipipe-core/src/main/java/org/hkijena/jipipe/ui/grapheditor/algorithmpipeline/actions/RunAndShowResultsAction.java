package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions;

import org.hkijena.jipipe.ui.grapheditor.general.actions.JIPipeNodeUIAction;

public class RunAndShowResultsAction implements JIPipeNodeUIAction {
    private final boolean storeIntermediateResults;

    public RunAndShowResultsAction(boolean storeIntermediateResults) {
        this.storeIntermediateResults = storeIntermediateResults;
    }

    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }
}
