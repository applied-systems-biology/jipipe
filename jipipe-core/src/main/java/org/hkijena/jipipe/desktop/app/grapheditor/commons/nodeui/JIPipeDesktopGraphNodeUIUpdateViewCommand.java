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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;

public class JIPipeDesktopGraphNodeUIUpdateViewCommand implements JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand {
    private final boolean updateAssets;
    private final boolean updateSlots;
    private final boolean updateSize;

    public JIPipeDesktopGraphNodeUIUpdateViewCommand(boolean updateAssets, boolean updateSlots, boolean updateSize) {
        this.updateAssets = updateAssets;
        this.updateSlots = updateSlots;
        this.updateSize = updateSize;
    }

    public boolean isUpdateAssets() {
        return updateAssets;
    }

    public boolean isUpdateSlots() {
        return updateSlots;
    }

    public boolean isUpdateSize() {
        return updateSize;
    }
}
