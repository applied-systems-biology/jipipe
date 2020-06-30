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

package org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.history.CutCompartmentGraphHistorySnapshot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class GraphCompartmentCutAlgorithmUIAction extends GraphCompartmentCopyAlgorithmUIAction {

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        super.run(canvasUI, selection);
        ACAQProject project = ((ACAQProjectWorkbench) canvasUI.getWorkbench()).getProject();
        for (ACAQAlgorithmUI ui : selection) {
            ACAQProjectCompartment compartment = (ACAQProjectCompartment) ui.getAlgorithm();
            canvasUI.getGraphHistory().addSnapshotBefore(new CutCompartmentGraphHistorySnapshot(project, compartment));
            project.removeCompartment(compartment);
        }
    }

    @Override
    public String getName() {
        return "Cut";
    }

    @Override
    public String getDescription() {
        return "Cuts the selection into the clipboard";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("cut.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }
}
