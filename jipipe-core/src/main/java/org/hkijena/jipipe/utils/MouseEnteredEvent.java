package org.hkijena.jipipe.utils;

import java.awt.Component;
import java.awt.event.MouseEvent;

public class MouseEnteredEvent extends MouseEvent {
    public MouseEnteredEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger, button);
    }

    public MouseEnteredEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
    }

    public MouseEnteredEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, button);
    }
}
