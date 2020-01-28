package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQAlgorithmGraph;
import org.hkijena.acaq5.extension.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.ui.components.ACAQAlgorithmUI;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmGraphUI extends JPanel {
    private ACAQAlgorithmGraph algorithmGraph;
    private JPanel canvas;

    public ACAQAlgorithmGraphUI(ACAQAlgorithmGraph algorithmGraph) {
        super(null);
        this.algorithmGraph = algorithmGraph;
        initialize();
    }

    private void initialize() {
        setBackground(Color.WHITE);

        ACAQAlgorithmUI algorithmUI = new ACAQAlgorithmUI(new CLAHEImageEnhancer());
        add(algorithmUI);
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }
}
