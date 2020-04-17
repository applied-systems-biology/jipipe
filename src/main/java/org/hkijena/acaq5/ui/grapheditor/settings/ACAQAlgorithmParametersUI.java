package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.parameters.ACAQParameterAccessUI;

/**
 * Panel that shows the parameters of an algorithm
 */
public class ACAQAlgorithmParametersUI extends ACAQParameterAccessUI {

    private ACAQAlgorithm algorithm;

    /**
     * Creates a new parameter editor
     *
     * @param workbenchUI        The workbench
     * @param algorithm          The target algorithm
     * @param documentation      Optional default documentation
     * @param documentationBelow If true, show documentation below
     * @param withDocumentation  If false, disable documentation
     */
    public ACAQAlgorithmParametersUI(ACAQProjectWorkbench workbenchUI, ACAQAlgorithm algorithm, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
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
