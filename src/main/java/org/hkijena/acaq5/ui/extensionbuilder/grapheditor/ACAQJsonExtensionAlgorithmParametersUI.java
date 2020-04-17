package org.hkijena.acaq5.ui.extensionbuilder.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.parameters.ACAQParameterAccessUI;

/**
 * UI for an algorithm in a {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class ACAQJsonExtensionAlgorithmParametersUI extends ACAQParameterAccessUI {

    private ACAQAlgorithm algorithm;

    /**
     * Creates new instance
     *
     * @param workbenchUI        The workbench UI
     * @param algorithm          The algorithm
     * @param documentation      The documentation
     * @param documentationBelow If the documentation should be displayed below
     * @param withDocumentation  If documentation should be shown
     */
    public ACAQJsonExtensionAlgorithmParametersUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQAlgorithm algorithm, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
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
