package org.hkijena.jipipe.api.events;

public interface JIPipeEventListener<T extends JIPipeEvent> {
    void on(T event);
}
