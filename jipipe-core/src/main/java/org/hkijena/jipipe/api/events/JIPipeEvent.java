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

package org.hkijena.jipipe.api.events;

/**
 * Contains information about an event emitted by {@link JIPipeEventEmitter}
 */
public interface JIPipeEvent {

    /**
     * The source of the event (set during the creation of the event)
     *
     * @return the source object
     */
    Object getSource();

    /**
     * The first emitter that published the event
     * Will not change if an event is re-emitted
     *
     * @return the first emitter
     */
    JIPipeEventEmitter<?, ?> getEmitter();

    /**
     * Sets the first emitter.
     * Please use this function only if you know what you do.
     *
     * @param emitter the new emitter
     */
    void setEmitter(JIPipeEventEmitter<?, ?> emitter);
}
