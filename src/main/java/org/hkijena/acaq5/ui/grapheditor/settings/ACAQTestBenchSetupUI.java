package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;

public class ACAQTestBenchSetupUI extends ACAQUIPanel {

    private ACAQAlgorithm algorithm;

    public ACAQTestBenchSetupUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithm algorithm) {
        super(workbenchUI);
        this.algorithm = algorithm;
    }
}
