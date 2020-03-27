package org.hkijena.acaq5.ui.extensionbuilder.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;

public class ACAQJsonExtensionAlgorithmParametersUI extends ACAQParameterAccessUI {

    private ACAQAlgorithm algorithm;

    public ACAQJsonExtensionAlgorithmParametersUI(ACAQJsonExtensionUI workbenchUI, ACAQAlgorithm algorithm, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        super(workbenchUI, algorithm, documentation, documentationBelow, withDocumentation);
        this.algorithm = algorithm;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
