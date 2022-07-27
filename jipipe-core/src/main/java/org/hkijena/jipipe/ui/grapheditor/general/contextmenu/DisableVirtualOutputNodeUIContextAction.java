/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class DisableVirtualOutputNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        for (JIPipeNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                    if (outputSlot.getInfo().isVirtual())
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        for (JIPipeNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                    outputSlot.getInfo().setVirtual(false);
                }
                ui.refreshSlots();
            }
        }
    }

    @Override
    public String getName() {
        return "Store in memory";
    }

    @Override
    public String getDescription() {
        return "Makes that data generated by this slot is stored in memory during a full analysis. This raises the requirements on system memory significantly if the data is very large.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("devices/media-memory.png");
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}