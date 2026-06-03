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

package org.hkijena.jipipe.api.servers;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Event emitted when a server instance's state changes.
 */
public class JIPipeServerEvent extends AbstractJIPipeEvent {
    private final JIPipeServerInstance<?> instance;
    private final JIPipeServerState oldState;
    private final JIPipeServerState newState;

    /**
     * @param source   the event source (typically the service component)
     * @param instance the server instance that changed state
     * @param oldState the previous state
     * @param newState the new state
     */
    public JIPipeServerEvent(Object source, JIPipeServerInstance<?> instance,
                             JIPipeServerState oldState, JIPipeServerState newState) {
        super(source);
        this.instance = instance;
        this.oldState = oldState;
        this.newState = newState;
    }

    public JIPipeServerInstance<?> getInstance() {
        return instance;
    }

    public JIPipeServerState getOldState() {
        return oldState;
    }

    public JIPipeServerState getNewState() {
        return newState;
    }
}
