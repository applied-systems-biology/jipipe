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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.StackMaker;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

/**
 * Implementation of {@link ij.plugin.MontageMaker}
 */
@JIPipeDocumentation(name = "Montage to stack", description = "Slices an image montage into a stack.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class MontageToStackAlgorithm extends JIPipeIteratingAlgorithm {

    private int rows = 1;
    private int columns = 1;
    private int borderWidth = 0;

    public MontageToStackAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MontageToStackAlgorithm(MontageToStackAlgorithm other) {
        super(other);
        this.rows = other.rows;
        this.columns = other.columns;
        this.borderWidth = other.borderWidth;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        ImagePlus imp = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();
        ImageStack stack = new StackMaker().makeStack(imp.getProcessor(), rows, columns, borderWidth);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Montage to Stack", stack)));
    }

    @JIPipeDocumentation(name = "Rows", description = "The number of rows.")
    @JIPipeParameter(value = "rows", uiOrder = 10)
    public int getRows() {
        return rows;
    }

    @JIPipeParameter("rows")
    public void setRows(int rows) {
        this.rows = rows;
    }

    @JIPipeDocumentation(name = "Columns", description = "The number of columns.")
    @JIPipeParameter(value = "columns", uiOrder = 11)
    public int getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(int columns) {
        this.columns = columns;
    }

    @JIPipeDocumentation(name = "Border width", description = "Distance between each tile")
    @JIPipeParameter("border-width")
    public int getBorderWidth() {
        return borderWidth;
    }

    @JIPipeParameter("border-width")
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
    }
}
