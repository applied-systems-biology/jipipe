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
