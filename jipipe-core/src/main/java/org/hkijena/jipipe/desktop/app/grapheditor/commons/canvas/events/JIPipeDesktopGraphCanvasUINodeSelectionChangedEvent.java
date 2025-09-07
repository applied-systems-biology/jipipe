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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

/**
 * Triggered when An {@link JIPipeDesktopGraphCanvasUI} selection was changed
 */
public class JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent extends AbstractJIPipeEvent {
    private JIPipeDesktopGraphCanvasUI canvasUI;

    /**
     * @param canvasUI the canvas that triggered the event
     */
    public JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent(JIPipeDesktopGraphCanvasUI canvasUI) {
        super(canvasUI);
        this.canvasUI = canvasUI;
    }

    public JIPipeDesktopGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }
}
