package org.hkijena.jipipe.api.nodes.iterationstep;

public class JIPipeMutableIterationContext implements JIPipeIterationContext {
    private int currentIterationStepIndex;
    private int numIterationSteps;

    public JIPipeMutableIterationContext() {
    }

    public JIPipeMutableIterationContext(int currentIterationStepIndex, int numIterationSteps) {
        this.currentIterationStepIndex = currentIterationStepIndex;
        this.numIterationSteps = numIterationSteps;
    }

    public JIPipeMutableIterationContext(JIPipeMutableIterationContext other) {
        this.currentIterationStepIndex = other.currentIterationStepIndex;
        this.numIterationSteps = other.numIterationSteps;
    }

    @Override
    public int getCurrentIterationStepIndex() {
        return currentIterationStepIndex;
    }

    public void setCurrentIterationStepIndex(int currentIterationStepIndex) {
        this.currentIterationStepIndex = currentIterationStepIndex;
    }

    @Override
    public int getNumIterationSteps() {
        return numIterationSteps;
    }

    public void setNumIterationSteps(int numIterationSteps) {
        this.numIterationSteps = numIterationSteps;
    }
}
