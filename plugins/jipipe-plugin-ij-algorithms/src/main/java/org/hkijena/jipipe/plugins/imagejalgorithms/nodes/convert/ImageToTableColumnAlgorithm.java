/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convert;

import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Image to table column", description = "Copies the first column of an image into a new or existing column of a table. Opposite operation to 'Table column to image'.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ImagePlus2DGreyscale32FData.class, name = "Image", create = true, description = "The image that contains the value")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Target", optional = true, description = "Optional existing table where the image values are written into.", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ImageToTableColumnAlgorithm extends JIPipeIteratingAlgorithm {

    private String targetColumnName = "Image data";

    public ImageToTableColumnAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImageToTableColumnAlgorithm(ImageToTableColumnAlgorithm other) {
        super(other);
        targetColumnName = other.targetColumnName;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData target = iterationStep.getInputData("Target", ResultsTableData.class, progressInfo);
        FloatProcessor processor = (FloatProcessor) iterationStep.getInputData("Image", ImagePlus2DGreyscale32FData.class, progressInfo).getImage().getProcessor();
        if (target == null) {
            target = new ResultsTableData();
            target.addRows(processor.getHeight());
        } else {
            target = new ResultsTableData(target);
        }
        int rowsToCopy = Math.min(target.getRowCount(), processor.getHeight());
        int columnIndex = target.getOrCreateColumnIndex(targetColumnName, false);
        for (int i = 0; i < rowsToCopy; i++) {
            target.setValueAt(processor.getf(i), i, columnIndex);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Target/Generated column", description = "The table column where the values will be written. Existing values will be overwritten. If the column does not exist, a new one will be generated.")
    @JIPipeParameter(value = "target-column-name", important = true)
    public String getTargetColumnName() {
        return targetColumnName;
    }

    @JIPipeParameter("target-column-name")
    public void setTargetColumnName(String targetColumnName) {
        this.targetColumnName = targetColumnName;
    }
}
