package org.hkijena.jipipe.api.nodes.iterationstep;

public interface JIPipeIterationContext {
    /**
     * Index of the currently handled iteration step
     * @return the index of the current step
     */
    int getCurrentIterationStepIndex();

    /**
     * Number of available iteration steps
     * @return the number of iteration steps
     */
    int getNumIterationSteps();
}