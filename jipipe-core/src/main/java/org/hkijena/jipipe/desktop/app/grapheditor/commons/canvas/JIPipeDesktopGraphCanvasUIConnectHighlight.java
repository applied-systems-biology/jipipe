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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;

public class JIPipeDesktopGraphCanvasUIConnectHighlight {
    private final JIPipeDesktopGraphNodeUISlotActiveArea source;
    private final JIPipeDesktopGraphNodeUISlotActiveArea target;

    public JIPipeDesktopGraphCanvasUIConnectHighlight(JIPipeDesktopGraphNodeUISlotActiveArea source, JIPipeDesktopGraphNodeUISlotActiveArea target) {
        this.source = source;
        this.target = target;
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea getSource() {
        return source;
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea getTarget() {
        return target;
    }
}
