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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convert;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@JIPipeDocumentation(name = "Get pixels as matrix", description = "Extracts the pixel values of an image and puts them into a table in form of a matrix.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlus2DData.class, progressInfo);
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

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
