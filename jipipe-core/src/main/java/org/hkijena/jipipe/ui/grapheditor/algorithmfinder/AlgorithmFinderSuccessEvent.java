/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.algorithmfinder;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;

/**
 * Generated when the algorithm finder successfully found a connection
 */
public class AlgorithmFinderSuccessEvent {
    private JIPipeDataSlot outputSlot;
    private JIPipeDataSlot inputSlot;

    /**
     * @param outputSlot The output slot
     * @param inputSlot  The target slot
     */
    public AlgorithmFinderSuccessEvent(JIPipeDataSlot outputSlot, JIPipeDataSlot inputSlot) {
        this.outputSlot = outputSlot;
        this.inputSlot = inputSlot;
    }

    public JIPipeDataSlot getOutputSlot() {
        return outputSlot;
    }

    public JIPipeDataSlot getInputSlot() {
        return inputSlot;
    }
}
