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
import org.hkijena.acaq5.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.REQUEST_RUN_AND_SHOW_RESULTS;

public class RunAndShowResultsAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQAlgorithmUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        ACAQAlgorithmUI ui = selection.iterator().next();
        ui.getEventBus().post(new AlgorithmUIActionRequestedEvent(ui, REQUEST_RUN_AND_SHOW_RESULTS));
    }

    @Override
    public String getName() {
        return "Run & show results";
    }

    @Override
    public String getDescription() {
        return "Runs the pipeline up until this algorithm and shows the results.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("play.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }
}
