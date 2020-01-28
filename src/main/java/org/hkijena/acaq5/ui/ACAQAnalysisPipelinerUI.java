package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQAlgorithmGraph;

import javax.swing.*;
import java.awt.*;

public class ACAQAnalysisPipelinerUI extends ACAQUIPanel {

    private ACAQAlgorithmGraphUI graphUI;

    public ACAQAnalysisPipelinerUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new ACAQAlgorithmGraphUI(new ACAQAlgorithmGraph());
        add(graphUI, BorderLayout.CENTER);
    }
}
