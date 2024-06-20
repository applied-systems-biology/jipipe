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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.statistics;

import com.google.common.primitives.Floats;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Key/Value Histogram 5D", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. The values assigned to each key are collected and integrated, thus allowing to generate histograms. Allows the generation of normalized and cumulative histograms.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Key", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Value", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Histogram", create = true)
public class KeyValueHistogramGenerator extends JIPipeIteratingAlgorithm {
    private String outputKeyColumn = "key";

    private String outputValueColumn = "value";
    private JIPipeExpressionParameter integrationFunction = new JIPipeExpressionParameter("SUM(values)");
    private boolean cumulative = false;
    private boolean normalize = false;

    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;

    public KeyValueHistogramGenerator(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public KeyValueHistogramGenerator(KeyValueHistogramGenerator other) {
        super(other);
        this.integrationFunction = new JIPipeExpressionParameter(other.integrationFunction);
        this.cumulative = other.cumulative;
        this.normalize = other.normalize;
        this.outputKeyColumn = other.outputKeyColumn;
        this.outputValueColumn = other.outputValueColumn;
        this.sourceArea = other.sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = iterationStep.getInputData("Key", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = iterationStep.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();

        ROIListData roiInput = null;
        ImagePlus maskInput = null;

        switch (sourceArea) {
            case InsideRoi:
            case OutsideRoi:
                roiInput = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
                break;
            case InsideMask:
            case OutsideMask:
                maskInput = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                break;
        }

        ROIListData finalRoiInput = roiInput;
        ImagePlus finalMaskInput = maskInput;

        TDoubleObjectHashMap<TFloatList> bucketedValues = new TDoubleObjectHashMap<>();

        if (!ImageJUtils.imagesHaveSameSize(keyImage, valueImage)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        // Bucket the pixel values
        ImageJUtils.forEachIndexedZCTSlice(keyImage, (keyIp, index) -> {
            ImageProcessor mask = getMask(keyIp.getWidth(),
                    keyIp.getHeight(),
                    finalRoiInput,
                    finalMaskInput,
                    index
            );

            ImageProcessor valueIp = ImageJUtils.getSliceZero(valueImage, index);
            byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
            float[] keyPixels = (float[]) keyIp.getPixels();
            float[] valuePixels = (float[]) valueIp.getPixels();
            for (int i = 0; i < keyPixels.length; i++) {
                if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                    float key = keyPixels[i];
                    float value = valuePixels[i];
                    TFloatList list = bucketedValues.get(key);
                    if (list == null) {
                        list = new TFloatArrayList();
                        bucketedValues.put(key, list);
                    }
                    list.add(value);
                }
            }
        }, progressInfo.resolve("Collect pixels"));

        // Setup values
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        // Integrate buckets
        progressInfo.log("Integrating " + bucketedValues.size() + " buckets ...");
        TDoubleDoubleMap integratedValues = new TDoubleDoubleHashMap();
        for (double key : bucketedValues.keys()) {
            if (progressInfo.isCancelled())
                return;
            TFloatList list = bucketedValues.get(key);
            List<Float> asList = Floats.asList(list.toArray());
            variables.set("values", asList);
            double integrated = integrationFunction.evaluateToDouble(variables);
            integratedValues.put(key, integrated);
        }

        // Get sorted keys
        double[] sortedKeys = integratedValues.keys();
        Arrays.sort(sortedKeys);

        // Cumulative if enabled
        if (cumulative) {
            TDoubleDoubleMap cumulativeIntegratedValues = new TDoubleDoubleHashMap();
            double previousValue = 0;
            for (double key : sortedKeys) {
                if (progressInfo.isCancelled())
                    return;
                double value = integratedValues.get(key) + previousValue;
                previousValue = value;
                cumulativeIntegratedValues.put(key, value);
            }
            integratedValues = cumulativeIntegratedValues;
        }

        // Normalize if enabled
        if (normalize) {
            double max = Double.NEGATIVE_INFINITY;
            for (double value : integratedValues.values()) {
                max = Math.max(max, value);
            }
            for (double key : integratedValues.keys()) {
                integratedValues.put(key, integratedValues.get(key) / max);
            }
        }

        // Convert to table
        ResultsTableData outputTable = new ResultsTableData();
        int outputKeyColumnIndex = outputTable.addNumericColumn(outputKeyColumn);
        int outputValueColumnIndex = outputTable.addNumericColumn(outputValueColumn);
        for (double key : sortedKeys) {
            double value = integratedValues.get(key);
            int row = outputTable.addRow();
            outputTable.setValueAt(key, row, outputKeyColumnIndex);
            outputTable.setValueAt(value, row, outputValueColumnIndex);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    @SetJIPipeDocumentation(name = "Extract values from ...", description = "Determines from which image areas the pixel values used for extracting the values")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @SetJIPipeDocumentation(name = "Output column (keys)", description = "The table column where the keys will be written to")
    @JIPipeParameter(value = "output-key-column", uiOrder = 100)
    @StringParameterSettings(monospace = true)
    public String getOutputKeyColumn() {
        return outputKeyColumn;
    }

    @JIPipeParameter("output-key-column")
    public void setOutputKeyColumn(String outputKeyColumn) {
        this.outputKeyColumn = outputKeyColumn;
    }

    @SetJIPipeDocumentation(name = "Output column (integrated values)", description = "The table column where the integrated values will be written to")
    @JIPipeParameter(value = "output-value-column", uiOrder = 110)
    @StringParameterSettings(monospace = true)
    public String getOutputValueColumn() {
        return outputValueColumn;
    }

    @JIPipeParameter("output-value-column")
    public void setOutputValueColumn(String outputValueColumn) {
        this.outputValueColumn = outputValueColumn;
    }

    @SetJIPipeDocumentation(name = "Integration function", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function")
    @JIPipeExpressionParameterVariable(key = "values", name = "Values", description = "The values to be integrated")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getIntegrationFunction() {
        return integrationFunction;
    }

    @JIPipeParameter("integration-function")
    public void setIntegrationFunction(JIPipeExpressionParameter integrationFunction) {
        this.integrationFunction = integrationFunction;
    }

    @SetJIPipeDocumentation(name = "Cumulative", description = "If enabled, the histogram will be cumulative")
    @JIPipeParameter("cumulative")
    public boolean isCumulative() {
        return cumulative;
    }

    @JIPipeParameter("cumulative")
    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }

    @SetJIPipeDocumentation(name = "Normalize", description = "If enabled, normalizes the values")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }
}
