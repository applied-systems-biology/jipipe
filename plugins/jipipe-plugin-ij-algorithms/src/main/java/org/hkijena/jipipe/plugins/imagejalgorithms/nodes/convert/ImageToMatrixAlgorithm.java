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

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@SetJIPipeDocumentation(name = "Get pixels as matrix", description = "Extracts the pixel values of an image and puts them into a table in form of a matrix.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ImagePlus2DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ImageToMatrixAlgorithm extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public ImageToMatrixAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageToMatrixAlgorithm(ImageToMatrixAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlus2DData.class, progressInfo);
        ResultsTableData outputData = new ResultsTableData();

        ImageProcessor ip = inputData.getImage().getProcessor();
        boolean isColor = ip instanceof ColorProcessor;

        // Create columns
        for (int i = 0; i < ip.getWidth(); i++) {
            outputData.addColumn("X" + i, isColor);
        }

        // Create rows
        for (int y = 0; y < ip.getHeight(); y++) {
            outputData.addRow();
            for (int x = 0; x < ip.getWidth(); x++) {
                if (isColor) {
                    int pixel = ip.get(x, y);
                    int asRGB = inputData.getColorSpace().convertToRGB(pixel);
                    String hex = ColorUtils.colorToHexString(new Color(asRGB));
                    outputData.setValueAt(hex, y, x);
                } else {
                    outputData.setValueAt(ip.getf(x, y), y, x);
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
