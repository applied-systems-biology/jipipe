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

import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Key/Value statistics 5D (fast averages)", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. " +
        "Buckets the pixels of the 'Value' image based on the pixel in the 'Key' and calculates the number of values, their sum, and the average value.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Key", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Value", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class KeyValueAveragesGenerator extends JIPipeIteratingAlgorithm {
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private OptionalStringParameter keyColumnName = new OptionalStringParameter("key", true);
    private OptionalStringParameter countColumnName = new OptionalStringParameter("count", true);
    private OptionalStringParameter sumColumnName = new OptionalStringParameter("sum", true);
    private OptionalStringParameter meanColumnName = new OptionalStringParameter("mean", true);

    public KeyValueAveragesGenerator(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public KeyValueAveragesGenerator(KeyValueAveragesGenerator other) {
        super(other);
        this.sourceArea = other.sourceArea;
        this.keyColumnName = new OptionalStringParameter(other.keyColumnName);
        this.countColumnName = new OptionalStringParameter(other.countColumnName);
        this.sumColumnName = new OptionalStringParameter(other.sumColumnName);
        this.meanColumnName = new OptionalStringParameter(other.meanColumnName);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = iterationStep.getInputData("Key", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = iterationStep.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();

        ROI2DListData roiInput = null;
        ImagePlus maskInput = null;

        switch (sourceArea) {
            case InsideRoi:
            case OutsideRoi:
                roiInput = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                break;
            case InsideMask:
            case OutsideMask:
                maskInput = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                break;
        }

        ROI2DListData finalRoiInput = roiInput;
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
        ImageJIterationUtils.forEachIndexedZCTSlice(keyImage, (keyIp, index) -> {
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
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
        variables.set("all.values", allValues);
        variables.set("all.keys", allKeys);

        // Get sorted keys
        double[] sortedKeys = bucketedValues.keys();
        Arrays.sort(sortedKeys);

        progressInfo.log("Processing " + sortedKeys.length + " keys ...");
        // Convert to table

        ResultsTableData outputTable = new ResultsTableData();

        long lastProgressUpdate = System.currentTimeMillis();


        for (int i = 0; i < sortedKeys.length; i++) {
            double key = sortedKeys[i];
            TFloatList forBucket = bucketedValues.get(key);

            if (progressInfo.isCancelled())
                return;

            int count = forBucket.size();
            double sum = 0;
            for (int j = 0; j < forBucket.size(); j++) {
                sum += forBucket.get(j);
            }


            outputTable.addRow();
            if (keyColumnName.isEnabled())
                outputTable.setLastValue(key, keyColumnName.getContent());
            if (countColumnName.isEnabled())
                outputTable.setLastValue(count, countColumnName.getContent());
            if (sumColumnName.isEnabled())
                outputTable.setLastValue(sum, sumColumnName.getContent());
            if (meanColumnName.isEnabled())
                outputTable.setLastValue(sum / count, meanColumnName.getContent());

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastProgressUpdate) > 3000) {
                progressInfo.resolveAndLog(Math.round((1.0 * i / sortedKeys.length) * 100) + "%", i, sortedKeys.length);
                lastProgressUpdate = currentTime;
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    private ImageProcessor getMask(int width, int height, ROI2DListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
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

    @SetJIPipeDocumentation(name = "Column name: count", description = "Column name for the count of values")
    @JIPipeParameter("count-column-name")
    public OptionalStringParameter getCountColumnName() {
        return countColumnName;
    }

    @JIPipeParameter("count-column-name")
    public void setCountColumnName(OptionalStringParameter countColumnName) {
        this.countColumnName = countColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: sum", description = "Column name for the sum of values")
    @JIPipeParameter("sum-column-name")
    public OptionalStringParameter getSumColumnName() {
        return sumColumnName;
    }

    @JIPipeParameter("sum-column-name")
    public void setSumColumnName(OptionalStringParameter sumColumnName) {
        this.sumColumnName = sumColumnName;
    }

    @SetJIPipeDocumentation(name = "Column name: mean", description = "Column name for the mean values")
    @JIPipeParameter("mean-column-name")
    public OptionalStringParameter getMeanColumnName() {
        return meanColumnName;
    }

    @JIPipeParameter("mean-column-name")
    public void setMeanColumnName(OptionalStringParameter meanColumnName) {
        this.meanColumnName = meanColumnName;
    }
}
