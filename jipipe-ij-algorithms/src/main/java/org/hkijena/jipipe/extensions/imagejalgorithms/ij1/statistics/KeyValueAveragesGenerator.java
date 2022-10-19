package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics;

import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
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

@JIPipeDocumentation(name = "Key/Value statistics 5D (fast averages)", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. " +
        "Buckets the pixels of the 'Value' image based on the pixel in the 'Key' and calculates the number of values, their sum, and the average value.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Key", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = dataBatch.getInputData("Key", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = dataBatch.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();

        ROIListData roiInput = null;
        ImagePlus maskInput = null;

        switch (sourceArea) {
            case InsideRoi:
            case OutsideRoi:
                roiInput = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                break;
            case InsideMask:
            case OutsideMask:
                maskInput = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                break;
        }

        ROIListData finalRoiInput = roiInput;
        ImagePlus finalMaskInput = maskInput;

        List<Float> allValues = new ArrayList<>();
        List<Float> allKeys = new ArrayList<>();
        TDoubleObjectHashMap<TFloatList> bucketedValues = new TDoubleObjectHashMap<>();

        if (!ImageJUtils.imagesHaveSameSize(keyImage, valueImage)) {
            throw new UserFriendlyRuntimeException("Input images do not have the same size!",
                    "Input images do not have the same size!",
                    getDisplayName(),
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels.",
                    "Please check the input images.");
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
                if(mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
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
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
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
            if(keyColumnName.isEnabled())
                outputTable.setLastValue(key, keyColumnName.getContent());
            if(countColumnName.isEnabled())
                outputTable.setLastValue(count, countColumnName.getContent());
            if(sumColumnName.isEnabled())
                outputTable.setLastValue(sum, sumColumnName.getContent());
            if(meanColumnName.isEnabled())
                outputTable.setLastValue(sum / count, meanColumnName.getContent());

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastProgressUpdate) > 3000) {
                progressInfo.resolveAndLog(Math.round((1.0 * i / sortedKeys.length) * 100) + "%", i, sortedKeys.length);
                lastProgressUpdate = currentTime;
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    @JIPipeDocumentation(name = "Extract values from ...", description = "Determines from which image areas the pixel values used for extracting the values")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @JIPipeDocumentation(name = "Column name: key", description = "Column name for the key")
    @JIPipeParameter("key-column-name")
    public OptionalStringParameter getKeyColumnName() {
        return keyColumnName;
    }

    @JIPipeParameter("key-column-name")
    public void setKeyColumnName(OptionalStringParameter keyColumnName) {
        this.keyColumnName = keyColumnName;
    }

    @JIPipeDocumentation(name = "Column name: count", description = "Column name for the count of values")
    @JIPipeParameter("count-column-name")
    public OptionalStringParameter getCountColumnName() {
        return countColumnName;
    }

    @JIPipeParameter("count-column-name")
    public void setCountColumnName(OptionalStringParameter countColumnName) {
        this.countColumnName = countColumnName;
    }

    @JIPipeDocumentation(name = "Column name: sum", description = "Column name for the sum of values")
    @JIPipeParameter("sum-column-name")
    public OptionalStringParameter getSumColumnName() {
        return sumColumnName;
    }

    @JIPipeParameter("sum-column-name")
    public void setSumColumnName(OptionalStringParameter sumColumnName) {
        this.sumColumnName = sumColumnName;
    }

    @JIPipeDocumentation(name = "Column name: mean", description = "Column name for the mean values")
    @JIPipeParameter("mean-column-name")
    public OptionalStringParameter getMeanColumnName() {
        return meanColumnName;
    }

    @JIPipeParameter("mean-column-name")
    public void setMeanColumnName(OptionalStringParameter meanColumnName) {
        this.meanColumnName = meanColumnName;
    }
}
