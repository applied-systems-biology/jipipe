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

package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Event emitted when a {@link Microservice}'s state changes.
 */
public class MicroserviceStateChangeEvent extends AbstractJIPipeEvent {
    private final Microservice microservice;
    private final MicroserviceState oldState;
    private final MicroserviceState newState;

    public MicroserviceStateChangeEvent(Object source, Microservice microservice,
                                        MicroserviceState oldState, MicroserviceState newState) {
        super(source);
        this.microservice = microservice;
        this.oldState = oldState;
        this.newState = newState;
    }

    public Microservice getMicroservice() {
        return microservice;
    }

    public MicroserviceState getOldState() {
        return oldState;
    }

    public MicroserviceState getNewState() {
        return newState;
    }
}
