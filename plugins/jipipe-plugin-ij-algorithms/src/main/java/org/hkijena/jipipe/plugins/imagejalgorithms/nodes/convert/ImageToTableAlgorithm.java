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

import gnu.trove.list.TDoubleList;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collections;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@SetJIPipeDocumentation(name = "Get pixels as table", description = "Extracts the pixel values of an image and puts them into a table. " +
        "The table always includes columns <code>x</code>, <code>y</code>, <code>z</code>, <code>c</code>, and <code>t</code>. For greyscale images, the value is stored into a column <code>value</code>. " +
        "For color images, column names depend on the color space.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ImageToTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean applyPerSlice = false;
    private String sliceAnnotation = "Image index";

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public ImageToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageToTableAlgorithm(ImageToTableAlgorithm other) {
        super(other);
        this.applyPerSlice = other.applyPerSlice;
        this.sliceAnnotation = other.sliceAnnotation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (applyPerSlice) {
            ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
            ImageJUtils.forEachIndexedZCTSlice(inputData.getImage(), (imp, index) -> {
                ResultsTableData resultsTable = new ResultsTableData();
                prepareResultsTable(inputData, resultsTable);
                resultsTable.addRows(imp.getWidth() * imp.getHeight());
                writePixelsToTable(resultsTable, imp, inputData.getColorSpace(), index, 0);
                if (!StringUtils.isNullOrEmpty(sliceAnnotation)) {
                    iterationStep.addOutputData(getFirstOutputSlot(), resultsTable,
                            Collections.singletonList(new JIPipeTextAnnotation(sliceAnnotation, "slice=" + index)), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                } else {
                    iterationStep.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
                }
            }, progressInfo);
        } else {
            ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
            ResultsTableData resultsTable = new ResultsTableData();
            prepareResultsTable(inputData, resultsTable);
            resultsTable.addRows(inputData.getWidth() * inputData.getHeight() * inputData.getNFrames() * inputData.getNChannels() * inputData.getNSlices());

            int[] counter = new int[1];
            ImageJUtils.forEachIndexedZCTSlice(inputData.getImage(), (imp, index) -> {
                writePixelsToTable(resultsTable, imp, inputData.getColorSpace(), index, counter[0]);
                counter[0] += imp.getWidth() * imp.getHeight();
            }, progressInfo);

            iterationStep.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
        }
    }

    private void writePixelsToTable(ResultsTableData target, ImageProcessor imp, ColorSpace colorSpace, ImageSliceIndex sliceIndex, int startIndex) {
        final int nPixels = imp.getWidth() * imp.getHeight();
        final boolean isGreyscale = imp.isGrayscale();
        final int[] channelBuffer = imp.isGrayscale() ? new int[0] : new int[colorSpace.getNChannels()];
        for (int i = 0; i < nPixels; i++) {
            final int row = i + startIndex;
            final int x = i % imp.getWidth();
            final int y = i / imp.getWidth();
            final int z = sliceIndex.getZ();
            final int c = sliceIndex.getC();
            final int t = sliceIndex.getT();

            // For performance reasons, use predefined order
            target.setValueAt(x, row, 0);
            target.setValueAt(y, row, 1);
            target.setValueAt(z, row, 2);
            target.setValueAt(c, row, 3);
            target.setValueAt(t, row, 4);
            if (isGreyscale) {
                target.setValueAt(imp.getf(i), row, 5);
            } else {
                colorSpace.decomposePixel(imp.get(i), channelBuffer);
                for (int channel = 0; channel < channelBuffer.length; channel++) {
                    target.setValueAt(channelBuffer[channel], row, 5 + channel);
                }
            }
        }
    }

    private void prepareResultsTable(ImagePlusData inputData, ResultsTableData resultsTable) {
        resultsTable.addNumericColumn("x");
        resultsTable.addNumericColumn("y");
        resultsTable.addNumericColumn("z");
        resultsTable.addNumericColumn("c");
        resultsTable.addNumericColumn("t");
        if (inputData.isGrayscale()) {
            resultsTable.addNumericColumn("value");
        } else {
            for (int c = 0; c < inputData.getColorSpace().getNChannels(); c++) {
                resultsTable.addNumericColumn(inputData.getColorSpace().getChannelShortName(c));
            }
        }
    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If higher dimensional data is provided, generate a table for each slice. If disabled, " +
            "a table is generated for the whole image.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @SetJIPipeDocumentation(name = "Apply per slice annotation", description = "Optional annotation type that generated for each slice output. " +
            "It contains the string 'slice=[Number]'.")
    @JIPipeParameter("slice-annotation")
    public String getSliceAnnotation() {
        return sliceAnnotation;
    }

    @JIPipeParameter("slice-annotation")
    public void setSliceAnnotation(String sliceAnnotation) {
        this.sliceAnnotation = sliceAnnotation;
    }
}
