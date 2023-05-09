package org.hkijena.jipipe.api.events;

public class AbstractJIPipeEvent implements JIPipeEvent {
    private final Object source;

    private JIPipeEventEmitter<?, ?> emitter;

    public AbstractJIPipeEvent(Object source) {
        this.source = source;
    }

    @Override
    public Object getSource() {
        return source;
    }

    @Override
    public JIPipeEventEmitter<?, ?> getEmitter() {
        return emitter;
    }

    @Override
    public void setEmitter(JIPipeEventEmitter<?, ?> emitter) {
        this.emitter = emitter;
    }
}
