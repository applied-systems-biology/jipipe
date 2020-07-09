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

package org.hkijena.pipelinej.ui.grapheditor.contextmenu;

import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithm;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class DisablePassThroughAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        for (ACAQNodeUI ui : selection) {
            if (ui.getNode() instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) ui.getNode();
                if (algorithm.isPassThrough())
                    return true;
            }
        }
        return false;
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        for (ACAQNodeUI ui : selection) {
            if (ui.getNode() instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) ui.getNode();
                algorithm.setPassThrough(false);
            }
        }
    }

    @Override
    public String getName() {
        return "Disable pass-through";
    }

    @Override
    public String getDescription() {
        return "Sets the selected algorithms to process input to output.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("pass-through.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
