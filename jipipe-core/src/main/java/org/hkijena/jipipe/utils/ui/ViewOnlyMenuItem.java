package org.hkijena.jipipe.utils.ui;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class ViewOnlyMenuItem extends JMenuItem {
    public ViewOnlyMenuItem() {
    }

    public ViewOnlyMenuItem(Icon icon) {
        super(icon);
    }

    public ViewOnlyMenuItem(String text) {
        super(text);
    }

    public ViewOnlyMenuItem(Action a) {
        super(a);
    }

    public ViewOnlyMenuItem(String text, Icon icon) {
        super(text, icon);
    }

    @Override
    protected void processMouseEvent(MouseEvent evt) {
        if (evt.getID() == MouseEvent.MOUSE_RELEASED && contains(evt.getPoint())) {
            doClick();
            setArmed(true);
        } else
            super.processMouseEvent(evt);
    }
}
