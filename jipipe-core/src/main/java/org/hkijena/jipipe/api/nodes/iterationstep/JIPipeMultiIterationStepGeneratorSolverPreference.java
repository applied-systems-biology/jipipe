package org.hkijena.jipipe.api.nodes.iterationstep;

public enum JIPipeMultiIterationStepGeneratorSolverPreference {
    Auto("Auto", true, true),
    PreferSingleCompositeDictionary("Prefer dictionary solvers (single/multi)", true, true),
    PreferCompositeDictionary("Prefer composite dictionary solver", false, true),
    ForceFlowGraph("Force flow graph", false, false),;

    private final String label;
    private final boolean allowSingleDictionary;
    private final boolean allowCompositeDictionary;

    JIPipeMultiIterationStepGeneratorSolverPreference(String label, boolean allowSingleDictionary, boolean allowCompositeDictionary) {
        this.label = label;
        this.allowSingleDictionary = allowSingleDictionary;
        this.allowCompositeDictionary = allowCompositeDictionary;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAllowSingleDictionary() {
        return allowSingleDictionary;
    }

    public boolean isAllowCompositeDictionary() {
        return allowCompositeDictionary;
    }


    @Override
    public String toString() {
        return label;
    }
}
