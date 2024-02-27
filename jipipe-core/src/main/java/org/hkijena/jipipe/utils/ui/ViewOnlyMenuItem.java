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
