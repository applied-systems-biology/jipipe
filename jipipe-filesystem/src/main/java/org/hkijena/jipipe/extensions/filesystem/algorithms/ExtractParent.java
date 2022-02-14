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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Get parent directory", description = "Extracts the parent folder of each path")
@JIPipeNode(menuPath = "Extract", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Parent", autoCreate = true)


public class ExtractParent extends JIPipeSimpleIteratingAlgorithm {

    private int order = 1;

    /**
     * @param info Algorithm info
     */
    public ExtractParent(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ExtractParent(ExtractParent other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        PathData inputFolder = dataBatch.getInputData(getFirstInputSlot(), PathData.class, progressInfo);
        Path result = inputFolder.toPath();
        for (int i = 0; i < order; i++) {
            result = result.getParent();
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(result), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        report.resolve("Order").checkIfWithin(this, order, 0, Double.POSITIVE_INFINITY, true, false);
    }

    @JIPipeDocumentation(name = "Select N-th parent", description = "Determines which N-th parent is chosen. For example the 2nd parent of /a/b/c is 'a'. " +
            "If N=0, the path is not changed.")
    @JIPipeParameter("order")
    public int getOrder() {
        return order;
    }

    @JIPipeParameter("order")
    public void setOrder(int order) {
        this.order = order;
    }
}
