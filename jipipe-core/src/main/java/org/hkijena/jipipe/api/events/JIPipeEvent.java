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
