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

package org.hkijena.jipipe.ui.algorithmfinder;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Generated when the algorithm finder successfully found a connection
 */
public class AlgorithmFinderSuccessEvent extends AbstractJIPipeEvent {
    private JIPipeDataSlot outputSlot;
    private JIPipeDataSlot inputSlot;

    /**
     * @param source the event source
     * @param outputSlot The output slot
     * @param inputSlot  The target slot
     */
    public AlgorithmFinderSuccessEvent(Object source, JIPipeDataSlot outputSlot, JIPipeDataSlot inputSlot) {
        super(source);
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
