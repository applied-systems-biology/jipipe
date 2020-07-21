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

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class InvertSelectionNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        canvasUI.invertSelection();
    }

    @Override
    public String getName() {
        return "Invert selection";
    }

    @Override
    public String getDescription() {
        return "Inverts the selection";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("invert.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
