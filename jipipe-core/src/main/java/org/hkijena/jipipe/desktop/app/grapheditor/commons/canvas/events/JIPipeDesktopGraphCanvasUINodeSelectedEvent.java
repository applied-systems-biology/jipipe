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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

/**
 * Generated when an algorithm is selected
 */
public class JIPipeDesktopGraphCanvasUINodeSelectedEvent extends AbstractJIPipeEvent {

    private final JIPipeDesktopGraphNodeUI nodeUI;
    private boolean addToSelection;

    /**
     * @param nodeUI         the algorithm UI
     * @param addToSelection if the algorithm should be added to the selection
     */
    public JIPipeDesktopGraphCanvasUINodeSelectedEvent(JIPipeDesktopGraphNodeUI nodeUI, boolean addToSelection) {
        super(nodeUI);
        this.nodeUI = nodeUI;
        this.addToSelection = addToSelection;
    }

    public JIPipeDesktopGraphNodeUI getNodeUI() {
        return nodeUI;
    }

    public boolean isAddToSelection() {
        return addToSelection;
    }
}
