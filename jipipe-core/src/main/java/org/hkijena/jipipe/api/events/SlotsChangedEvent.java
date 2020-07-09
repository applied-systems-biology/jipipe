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

package org.hkijena.jipipe.api.events;

import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;

/**
 * Triggered when a {@link org.hkijena.jipipe.api.data.JIPipeSlotConfiguration} was changed
 */
public class SlotsChangedEvent {
    private final JIPipeSlotConfiguration configuration;

    /**
     * Creates a new instance
     *
     * @param configuration the configuration
     */
    public SlotsChangedEvent(JIPipeSlotConfiguration configuration) {
        this.configuration = configuration;
    }

    public JIPipeSlotConfiguration getConfiguration() {
        return configuration;
    }
}
