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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.statistics;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.pipelinej.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.pipelinej.utils.StringUtils;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@ACAQDocumentation(name = "Get pixel values (Greyscale)", description = "Extracts the greyscale values of an image and puts them into a table. " +
        "It generates following output columns: <pre>value</pre>")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Analysis, menuPath = "Statistics")
@AlgorithmInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class GreyscalePixelsGenerator extends ACAQSimpleIteratingAlgorithm {

    private boolean applyPerSlice = false;
    private String sliceAnnotation = "Image index";

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public GreyscalePixelsGenerator(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress,
                                Supplier<Boolean> isCancelled) {
        if (applyPerSlice) {
            ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
            ImageJUtils.forEachIndexedSlice(inputData.getImage(), (imp, index) -> {
                TDoubleList pixels = new TDoubleArrayList(imp.getPixelCount());
                getPixels(imp, pixels);
                ResultsTableData resultsTable = toResultsTable(pixels);
                if (!StringUtils.isNullOrEmpty(sliceAnnotation)) {
                    dataInterface.addOutputData(getFirstOutputSlot(), resultsTable,
                            Collections.singletonList(new ACAQAnnotation(sliceAnnotation, "slice=" + index)));
                } else {
                    dataInterface.addOutputData(getFirstOutputSlot(), resultsTable);
                }
            });
        } else {
            ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
            final TDoubleList pixels = new TDoubleArrayList();
            ImageJUtils.forEachSlice(inputData.getImage(), imp -> getPixels(imp, pixels));
            ResultsTableData resultsTable = toResultsTable(pixels);
            dataInterface.addOutputData(getFirstOutputSlot(), resultsTable);
        }
    }

    private ResultsTableData toResultsTable(TDoubleList pixels) {
        ResultsTable resultsTable = new ResultsTable(pixels.size());
        for (int i = 0; i < pixels.size(); ++i) {
            resultsTable.setValue("value", i, pixels.get(i));
        }
        return new ResultsTableData(resultsTable);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQDocumentation(name = "Apply per slice", description = "If higher dimensional data is provided, generate a table for each slice. If disabled, " +
            "a table is generated for the whole image.")
    @ACAQParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @ACAQParameter("apply-per-slice")
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

    @ACAQDocumentation(name = "Apply per slice annotation", description = "Optional annotation type that generated for each slice output. " +
            "It contains the string 'slice=[Number]'.")
    @ACAQParameter("slice-annotation")
    public String getSliceAnnotation() {
        return sliceAnnotation;
    }

    @ACAQParameter("slice-annotation")
    public void setSliceAnnotation(String sliceAnnotation) {
        this.sliceAnnotation = sliceAnnotation;
    }
}
