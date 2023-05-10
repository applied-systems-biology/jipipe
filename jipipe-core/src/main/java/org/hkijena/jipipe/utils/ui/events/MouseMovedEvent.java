package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import java.awt.*;
import java.awt.event.MouseEvent;

public class MouseMovedEvent extends MouseEvent implements JIPipeEvent {
    private JIPipeEventEmitter<?,?> emitter;
    public MouseMovedEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger, button);
    }

    public MouseMovedEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
    }

    public MouseMovedEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, button);
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
