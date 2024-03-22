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

package org.hkijena.jipipe.desktop.commons.events;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

import java.awt.*;

/**
 * Triggered when a JIPipe window was closed
 */
public class WindowClosedEvent extends AbstractJIPipeEvent {
    private final Window window;

    public WindowClosedEvent(Window window) {
        super(window);
        this.window = window;
    }

    public Window getWindow() {
        return window;
    }
}
