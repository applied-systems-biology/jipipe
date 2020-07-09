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

package org.hkijena.pipelinej.api.events;

import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;

/**
 * Triggered by an {@link ACAQParameterCollection} if the list of available parameters is changed
 */
public class ParameterStructureChangedEvent {
    private ACAQParameterCollection source;

    /**
     * @param source event source
     */
    public ParameterStructureChangedEvent(ACAQParameterCollection source) {
        this.source = source;
    }

    public ACAQParameterCollection getSource() {
        return source;
    }
}
