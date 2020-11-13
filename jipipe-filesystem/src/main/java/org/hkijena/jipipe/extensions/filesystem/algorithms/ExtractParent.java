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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;

/**
 * Applies subfolder navigation to each input folder
 */
@JIPipeDocumentation(name = "Parent", description = "Extracts the parent folder of each path")
@JIPipeOrganization(menuPath = "Extract", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Parent", autoCreate = true)

// Traits
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        PathData inputFolder = dataBatch.getInputData(getFirstInputSlot(), PathData.class);
        Path result = inputFolder.getPath();
        for (int i = 0; i < order; i++) {
            result = result.getParent();
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(result));
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Order").checkIfWithin(this, order, 0, Double.POSITIVE_INFINITY, true, false);
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
