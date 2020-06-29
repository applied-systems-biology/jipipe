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

package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class DisableAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQAlgorithmUI> selection) {
        for (ACAQAlgorithmUI ui : selection) {
            if(ui.getAlgorithm() instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) ui.getAlgorithm();
                if(algorithm.isEnabled())
                    return true;
            }
        }
        return false;
    }

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        for (ACAQAlgorithmUI ui : selection) {
            if(ui.getAlgorithm() instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) ui.getAlgorithm();
                algorithm.setEnabled(false);
            }
        }
    }

    @Override
    public String getName() {
        return "Disable";
    }

    @Override
    public String getDescription() {
        return "Disables the selected algorithms";
    }

    @Override
    public Icon getIcon() {
        return  UIUtils.getIconFromResources("block.png");
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
