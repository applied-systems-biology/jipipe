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

import org.hkijena.acaq5.api.grouping.JsonAlgorithm;
import org.hkijena.acaq5.api.history.GraphChangedHistorySnapshot;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class JsonAlgorithmToGroupAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQAlgorithmUI> selection) {
        return selection.stream().map(ACAQAlgorithmUI::getAlgorithm).anyMatch(a -> a instanceof JsonAlgorithm);
    }

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        canvasUI.getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(canvasUI.getAlgorithmGraph(), "Convert to group"));
        for (ACAQAlgorithmUI ui : selection) {
            if(ui.getAlgorithm() instanceof JsonAlgorithm) {
                JsonAlgorithm.unpackToNodeGroup((JsonAlgorithm) ui.getAlgorithm());
            }
        }
    }

    @Override
    public String getName() {
        return "Convert to group";
    }

    @Override
    public String getDescription() {
        return "Converts selected JSON algorithms into distinct group nodes";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("archive-extract.png");
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
