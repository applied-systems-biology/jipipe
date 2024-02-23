package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.statistics;

import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Key/Value threshold statistics 5D (fast averages)", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. " +
        "The set of value pixels is partitioned into two sets based on whether the key is lower, or equal/higher than the currently processed key. " +
        "This is a fast version of 'Key/Value threshold statistics 5D' that only produces the number of pixels in each partition, the sum of values, as well as the average.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Key", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class KeyValueThresholdPartitionAveragesGenerator extends JIPipeIteratingAlgorithm {
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private OptionalStringParameter keyColumnName = new OptionalStringParameter("key", true);
    private OptionalStringParameter class0CountColumnName = new OptionalStringParameter("class0.count", true);
    private OptionalStringParameter class0SumColumnName = new OptionalStringParameter("class0.sum", true);
    private OptionalStringParameter class0MeanColumnName = new OptionalStringParameter("class0.mean", true);
    private OptionalStringParameter class1CountColumnName = new OptionalStringParameter("class0.count", true);
    private OptionalStringParameter class1SumColumnName = new OptionalStringParameter("class0.sum", true);
    private OptionalStringParameter class1MeanColumnName = new OptionalStringParameter("class0.mean", true);

    public KeyValueThresholdPartitionAveragesGenerator(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public KeyValueThresholdPartitionAveragesGenerator(KeyValueThresholdPartitionAveragesGenerator other) {
        super(other);
        this.sourceArea = other.sourceArea;
        this.keyColumnName = new OptionalStringParameter(other.keyColumnName);
        this.class0CountColumnName = new OptionalStringParameter(other.class0CountColumnName);
        this.class0SumColumnName = new OptionalStringParameter(other.class0SumColumnName);
        this.class0MeanColumnName = new OptionalStringParameter(other.class0MeanColumnName);
        this.class1CountColumnName = new OptionalStringParameter(other.class1CountColumnName);
        this.class1SumColumnName = new OptionalStringParameter(other.class1SumColumnName);
        this.class1MeanColumnName = new OptionalStringParameter(other.class1MeanColumnName);
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

        List<Float> allValues = new ArrayList<>();
        List<Float> allKeys = new ArrayList<>();
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
                    allValues.add(value);
                    allKeys.add(key);
                }
            }
        }, progressInfo.resolve("Collect pixels"));

        // Setup values
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.set("all.values", allValues);
        variables.set("all.keys", allKeys);

        // Get sorted keys
        double[] sortedKeys = bucketedValues.keys();
        Arrays.sort(sortedKeys);

        progressInfo.log("Processing " + sortedKeys.length + " keys ...");
        // Convert to table

        ResultsTableData outputTable = new ResultsTableData();

        long lastProgressUpdate = System.currentTimeMillis();

        // Init
        int class1Count = 0;
        double class1Sum = 0;
        int class0Count = 0;
        double class0Sum = 0;

        for (double key : sortedKeys) {
            TFloatList forBucket = bucketedValues.get(key);
            for (int j = 0; j < forBucket.size(); j++) {
                class1Sum += forBucket.get(j);
                ++class1Count;
            }
        }

        assert class1Count == allValues.size();

        for (int i = 0; i < sortedKeys.length; i++) {
            double key = sortedKeys[i];
            TFloatList forBucket = bucketedValues.get(key);

            if (progressInfo.isCancelled())
                return;

            for (int j = 0; j < forBucket.size(); j++) {
                --class1Count;
                class1Sum -= forBucket.get(j);
                ++class0Count;
                class0Sum += forBucket.get(j);
            }

            outputTable.addRow();

            if (keyColumnName.isEnabled())
                outputTable.setLastValue(key, keyColumnName.getContent());
            if (class0CountColumnName.isEnabled())
                outputTable.setLastValue(class0Count, class0CountColumnName.getContent());
            if (class0SumColumnName.isEnabled())
                outputTable.setLastValue(class0Sum, class0SumColumnName.getContent());
            if (class0MeanColumnName.isEnabled())
                outputTable.setLastValue(class0Sum / class0Count, class0MeanColumnName.getContent());
            if (class1CountColumnName.isEnabled())
                outputTable.setLastValue(class1Count, class1CountColumnName.getContent());
            if (class1SumColumnName.isEnabled())
                outputTable.setLastValue(class1Sum, class1SumColumnName.getContent());
            if (class1MeanColumnName.isEnabled())
                outputTable.setLastValue(class1Sum / class1Count, class1MeanColumnName.getContent());

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastProgressUpdate) > 3000) {
                progressInfo.resolveAndLog(Math.round((1.0 * i / sortedKeys.length) * 100) + "%", i, sortedKeys.length);
                lastProgressUpdate = currentTime;
            }
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

    @SetJIPipeDocumentation(name = "Column name: key", description = "Column name for the key")
    @JIPipeParameter("key-column-name")
    public OptionalStringParameter getKeyColumnName() {
        return keyColumnName;
    }

    @JIPipeParameter("key-column-name")
    public void setKeyColumnName(OptionalStringParameter keyColumnName) {
        this.keyColumnName = keyColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: class 0 count", description = "Column name for the count of class 0 values")
    @JIPipeParameter("class0-count-column-name")
    public OptionalStringParameter getClass0CountColumnName() {
        return class0CountColumnName;
    }

    @JIPipeParameter("class0-count-column-name")
    public void setClass0CountColumnName(OptionalStringParameter class0CountColumnName) {
        this.class0CountColumnName = class0CountColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: class 0 sum", description = "Column name for the sum of class 0 values")
    @JIPipeParameter("class0-sum-column-name")
    public OptionalStringParameter getClass0SumColumnName() {
        return class0SumColumnName;
    }

    @JIPipeParameter("class0-sum-column-name")
    public void setClass0SumColumnName(OptionalStringParameter class0SumColumnName) {
        this.class0SumColumnName = class0SumColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: class 0 mean", description = "Column name for the mean of class 0 values")
    @JIPipeParameter("class0-mean-column-name")
    public OptionalStringParameter getClass0MeanColumnName() {
        return class0MeanColumnName;
    }

    @JIPipeParameter("class0-mean-column-name")
    public void setClass0MeanColumnName(OptionalStringParameter class0MeanColumnName) {
        this.class0MeanColumnName = class0MeanColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: class 1 count", description = "Column name for the count of class 1 values")
    @JIPipeParameter("class1-count-column-name")
    public OptionalStringParameter getClass1CountColumnName() {
        return class1CountColumnName;
    }

    @JIPipeParameter("class1-count-column-name")
    public void setClass1CountColumnName(OptionalStringParameter class1CountColumnName) {
        this.class1CountColumnName = class1CountColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: class 1 sum", description = "Column name for the sum of class 1 values")
    @JIPipeParameter("class1-sum-column-name")
    public OptionalStringParameter getClass1SumColumnName() {
        return class1SumColumnName;
    }

    @JIPipeParameter("class1-sum-column-name")
    public void setClass1SumColumnName(OptionalStringParameter class1SumColumnName) {
        this.class1SumColumnName = class1SumColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: class 1 mean", description = "Column name for the mean of class 1 values")
    @JIPipeParameter("class1-mean-column-name")
    public OptionalStringParameter getClass1MeanColumnName() {
        return class1MeanColumnName;
    }

    @JIPipeParameter("class1-mean-column-name")
    public void setClass1MeanColumnName(OptionalStringParameter class1MeanColumnName) {
        this.class1MeanColumnName = class1MeanColumnName;
    }
}
