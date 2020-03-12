package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.extension.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;

public class ACAQGraphWrapperAlgorithmExporter extends ACAQUIPanel {

    private GraphWrapperAlgorithmDeclaration algorithmDeclaration;

    public ACAQGraphWrapperAlgorithmExporter(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph wrappedGraph) {
        super(workbenchUI);
        algorithmDeclaration = new GraphWrapperAlgorithmDeclaration();
        algorithmDeclaration.setGraph(wrappedGraph);
        initialize();
    }

    private void initialize() {

    }

}
