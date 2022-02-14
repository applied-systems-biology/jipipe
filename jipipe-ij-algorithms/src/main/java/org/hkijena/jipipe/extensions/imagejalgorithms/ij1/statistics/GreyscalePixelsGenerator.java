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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collections;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@JIPipeDocumentation(name = "Get pixel values (Greyscale)", description = "Extracts the greyscale values of an image and puts them into a table. " +
        "It generates following output columns: <pre>value</pre>")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class GreyscalePixelsGenerator extends JIPipeSimpleIteratingAlgorithm {

    private boolean applyPerSlice = false;
    private String sliceAnnotation = "Image index";

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public GreyscalePixelsGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GreyscalePixelsGenerator(GreyscalePixelsGenerator other) {
        super(other);
        this.applyPerSlice = other.applyPerSlice;
        this.sliceAnnotation = other.sliceAnnotation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (applyPerSlice) {
            ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
            ImageJUtils.forEachIndexedSlice(inputData.getImage(), (imp, index) -> {
                TDoubleList pixels = new TDoubleArrayList(imp.getPixelCount());
                getPixels(imp, pixels);
                ResultsTableData resultsTable = toResultsTable(pixels);
                if (!StringUtils.isNullOrEmpty(sliceAnnotation)) {
                    dataBatch.addOutputData(getFirstOutputSlot(), resultsTable,
                            Collections.singletonList(new JIPipeTextAnnotation(sliceAnnotation, "slice=" + index)), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                } else {
                    dataBatch.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
                }
            }, progressInfo);
        } else {
            ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
            final TDoubleList pixels = new TDoubleArrayList();
            ImageJUtils.forEachSlice(inputData.getImage(), imp -> getPixels(imp, pixels), progressInfo);
            ResultsTableData resultsTable = toResultsTable(pixels);
            dataBatch.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
        }
    }

    private ResultsTableData toResultsTable(TDoubleList pixels) {
        ResultsTable resultsTable = new ResultsTable(pixels.size());
        for (int i = 0; i < pixels.size(); ++i) {
            resultsTable.setValue("value", i, pixels.get(i));
        }
        return new ResultsTableData(resultsTable);
    }

    @JIPipeDocumentation(name = "Apply per slice", description = "If higher dimensional data is provided, generate a table for each slice. If disabled, " +
            "a table is generated for the whole image.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    private void getPixels(ImageProcessor processor, TDoubleList result) {
        if (processor instanceof FloatProcessor) {
            for (int i = 0; i < processor.getPixelCount(); ++i) {
                result.add(processor.getf(i));
            }
        } else {
            for (int i = 0; i < processor.getPixelCount(); ++i) {
                result.add(processor.get(i));
            }
        }
    }

    @JIPipeDocumentation(name = "Apply per slice annotation", description = "Optional annotation type that generated for each slice output. " +
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
