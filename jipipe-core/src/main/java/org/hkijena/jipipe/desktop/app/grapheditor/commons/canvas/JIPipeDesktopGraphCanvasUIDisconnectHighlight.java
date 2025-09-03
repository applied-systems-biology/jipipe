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

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;

import java.util.Set;

public class JIPipeDesktopGraphCanvasUIDisconnectHighlight {
    private final JIPipeDesktopGraphNodeUISlotActiveArea target;
    private final Set<JIPipeDataSlot> sources;

    public JIPipeDesktopGraphCanvasUIDisconnectHighlight(JIPipeDesktopGraphNodeUISlotActiveArea target, Set<JIPipeDataSlot> sources) {
        this.target = target;
        this.sources = sources;
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea getTarget() {
        return target;
    }

    public Set<JIPipeDataSlot> getSources() {
        return sources;
    }
}
