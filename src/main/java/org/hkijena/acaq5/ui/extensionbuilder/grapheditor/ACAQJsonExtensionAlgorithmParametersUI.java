package org.hkijena.acaq5.ui.extensionbuilder.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;

/**
 * UI for an algorithm in a {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class ACAQJsonExtensionAlgorithmParametersUI extends ACAQParameterAccessUI {

    private ACAQAlgorithm algorithm;

    /**
     * Creates new instance
     * @param workbenchUI The workbench UI
     * @param algorithm The algorithm
     * @param documentation The documentation
     * @param documentationBelow If the documentation should be displayed below
     * @param withDocumentation If documentation should be shown
     */
    public ACAQJsonExtensionAlgorithmParametersUI(ACAQJsonExtensionUI workbenchUI, ACAQAlgorithm algorithm, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        super(workbenchUI, algorithm, documentation, documentationBelow, withDocumentation);
        this.algorithm = algorithm;
    }

    /**
     * @return The algorithm
     */
    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
