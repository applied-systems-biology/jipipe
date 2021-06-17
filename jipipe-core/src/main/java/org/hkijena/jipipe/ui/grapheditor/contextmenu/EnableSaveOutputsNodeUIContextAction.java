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

package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class EnableSaveOutputsNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        for (JIPipeNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                    if (!outputSlot.getInfo().isSaveOutputs())
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
                    outputSlot.getInfo().setSaveOutputs(true);
                }
                ui.refreshSlots();
            }
        }
    }

    @Override
    public String getName() {
        return "Enable save outputs";
    }

    @Override
    public String getDescription() {
        return "Enables automated output saving of the selected algorithms.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/save.png");
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
