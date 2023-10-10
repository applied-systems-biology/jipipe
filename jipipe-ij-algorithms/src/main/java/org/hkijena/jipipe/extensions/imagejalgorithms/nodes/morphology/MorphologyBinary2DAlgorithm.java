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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.Prefs;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Morphological operation (binary) 2D", description = "Applies a morphological operation to binary images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeHidden
public class MorphologyBinary2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Operation operation = Operation.Dilate;
    private int iterations = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MorphologyBinary2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MorphologyBinary2DAlgorithm(MorphologyBinary2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.iterations = other.iterations;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ip -> {
            int fg = Prefs.blackBackground ? 255 : 0;
            int foreground = ip.isInvertedLut() ? 255 - fg : fg;
            int background = 255 - foreground;
            switch (operation) {
                case Dilate:
                    ((ByteProcessor) ip).dilate(iterations, background);
                    break;
                case Erode:
                    ((ByteProcessor) ip).erode(iterations, background);
                    break;
                case Open:
                    ((ByteProcessor) ip).erode(iterations, background);
                    ((ByteProcessor) ip).dilate(iterations, background);
                    break;
                case Close:
                    ((ByteProcessor) ip).dilate(iterations, background);
                    ((ByteProcessor) ip).erode(iterations, background);
                    break;
            }
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Operation", description = "The morphological operation")
    @JIPipeParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;

    }

    @JIPipeDocumentation(name = "Iterations", description = "How many times the operation is applied")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;

    }

    /**
     * Available transformation functions
     */
    public enum Operation {
        Erode, Dilate, Open, Close
    }
}
