package org.hkijena.jipipe.api.nodes.iterationstep;

public class JIPipeMutableIterationContext implements JIPipeIterationContext {
    private int currentIterationStepIndex;

    public JIPipeMutableIterationContext() {
    }

    public JIPipeMutableIterationContext(int currentIterationStepIndex) {
        this.currentIterationStepIndex = currentIterationStepIndex;
    }

    public JIPipeMutableIterationContext(JIPipeMutableIterationContext other) {
        this.currentIterationStepIndex = other.currentIterationStepIndex;
    }

    @Override
    public int getCurrentIterationStepIndex() {
        return currentIterationStepIndex;
    }

    public void setCurrentIterationStepIndex(int currentIterationStepIndex) {
        this.currentIterationStepIndex = currentIterationStepIndex;
    }
}
