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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TByteByteMap;
import gnu.trove.map.TFloatFloatMap;
import gnu.trove.map.TShortShortMap;
import gnu.trove.map.hash.TByteByteHashMap;
import gnu.trove.map.hash.TFloatFloatHashMap;
import gnu.trove.map.hash.TShortShortHashMap;
import gnu.trove.set.TFloatSet;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.util.SortOrder;

@SetJIPipeDocumentation(name = "Merge labels (min thickness) 2D", description = "Merges labels until the thickness of the label reaches a minimum threshold. If the image has multiple slices, the algorithm is applied per slice.")
@ConfigureJIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
public class MergeLabelsToThicknessAlgorithm extends JIPipeIteratingAlgorithm {

    private int minThickness = 10;
    private boolean excludeZero = true;
    private SortOrder sortOrder = SortOrder.Ascending;
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;

    public MergeLabelsToThicknessAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public MergeLabelsToThicknessAlgorithm(MergeLabelsToThicknessAlgorithm other) {
        super(other);
        this.minThickness = other.minThickness;
        this.sourceArea = other.sourceArea;
        this.excludeZero = other.excludeZero;
        this.sortOrder = other.sortOrder;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
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

        if (image.getBitDepth() == 8) {
            ByteProcessor tempProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
            ImageJUtils.forEachIndexedZCTSliceWithProgress(image, (ip, index, stackProgress) -> {
                ImageProcessor mask = getMask(ip.getWidth(),
                        ip.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );

                // Collect existing labels
                byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
                byte[] pixels = (byte[]) ip.getPixels();
                TIntSet existingLabels = new TIntHashSet();
                for (int i = 0; i < pixels.length; i++) {
                    if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                        existingLabels.add(Byte.toUnsignedInt(pixels[i]));
                    }
                }
                progressInfo.log("Found " + existingLabels.size() + " labels");

                // Sort the labels
                TIntList sortedExistingLabels = new TIntArrayList(existingLabels.toArray());
                if (sortOrder == SortOrder.Ascending) {
                    sortedExistingLabels.sort();
                } else {
                    sortedExistingLabels.sort();
                    sortedExistingLabels.reverse();
                }

                // Mapping
                int newLabel = 1;
                TByteByteMap mapping = new TByteByteHashMap();

                // Go through each label
                tempProcessor.setColor(0);
                tempProcessor.fill();
                int lastPercentage = 0;
                for (int labelIndex = 0; labelIndex < sortedExistingLabels.size(); labelIndex++) {

                    final int targetLabel = sortedExistingLabels.get(labelIndex);

                    if (excludeZero && targetLabel == 0)
                        continue;

                    // Give progress info
                    int newPercentage = (int) (1.0 * labelIndex / sortedExistingLabels.size() * 100);
                    if (newPercentage != lastPercentage) {
                        stackProgress.log("Processing labels ... " + newPercentage + "%");
                        lastPercentage = newPercentage;
                    }

                    // Write the label into temp
                    byte[] tempPixels = (byte[]) tempProcessor.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        if (Byte.toUnsignedInt(pixels[i]) == targetLabel && (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0)) {
                            tempPixels[i] = (byte) 255;
                        }
                    }

                    // Remap
                    mapping.put((byte) targetLabel, (byte) newLabel);

                    // Apply edt and calculate the maximum
                    EDM edm = new EDM();
                    FloatProcessor edtProcessor = edm.makeFloatEDM(tempProcessor, 0, true);
                    double thickness = edtProcessor.getStats().max * 2;
                    if (thickness >= minThickness) {
                        // Increment the label and reset temp
                        tempProcessor.setColor(0);
                        tempProcessor.fill();
                        ++newLabel;
                    }
                }

                // Remap
                for (int i = 0; i < pixels.length; i++) {
                    if (mapping.containsKey(pixels[i])) {
                        pixels[i] = mapping.get(pixels[i]);
                    }
                }

            }, progressInfo.resolve("Processing"));
        } else if (image.getBitDepth() == 16) {
            ByteProcessor tempProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
            ImageJUtils.forEachIndexedZCTSliceWithProgress(image, (ip, index, stackProgress) -> {
                ImageProcessor mask = getMask(ip.getWidth(),
                        ip.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );

                // Collect existing labels
                byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
                short[] pixels = (short[]) ip.getPixels();
                TIntSet existingLabels = new TIntHashSet();
                for (int i = 0; i < pixels.length; i++) {
                    if (mask == null || Short.toUnsignedInt(maskPixels[i]) > 0) {
                        existingLabels.add(Short.toUnsignedInt(pixels[i]));
                    }
                }
                progressInfo.log("Found " + existingLabels.size() + " labels");

                // Sort the labels
                TIntList sortedExistingLabels = new TIntArrayList(existingLabels.toArray());
                if (sortOrder == SortOrder.Ascending) {
                    sortedExistingLabels.sort();
                } else {
                    sortedExistingLabels.sort();
                    sortedExistingLabels.reverse();
                }

                // Mapping
                int newLabel = 1;
                TShortShortMap mapping = new TShortShortHashMap();

                // Go through each label
                tempProcessor.setColor(0);
                tempProcessor.fill();
                int lastPercentage = 0;
                for (int labelIndex = 0; labelIndex < sortedExistingLabels.size(); labelIndex++) {

                    final int targetLabel = sortedExistingLabels.get(labelIndex);

                    if (excludeZero && targetLabel == 0)
                        continue;

                    // Give progress info
                    int newPercentage = (int) (1.0 * labelIndex / sortedExistingLabels.size() * 100);
                    if (newPercentage != lastPercentage) {
                        stackProgress.log("Processing labels ... " + newPercentage + "%");
                        lastPercentage = newPercentage;
                    }

                    // Write the label into temp
                    byte[] tempPixels = (byte[]) tempProcessor.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        if (Short.toUnsignedInt(pixels[i]) == targetLabel && (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0)) {
                            tempPixels[i] = (byte) 255;
                        }
                    }

                    // Remap
                    mapping.put((short) targetLabel, (short) newLabel);

                    // Apply edt and calculate the maximum
                    EDM edm = new EDM();
                    FloatProcessor edtProcessor = edm.makeFloatEDM(tempProcessor, 0, true);
                    double thickness = edtProcessor.getStats().max * 2;
                    if (thickness >= minThickness) {
                        // Increment the label and reset temp
                        tempProcessor.setColor(0);
                        tempProcessor.fill();
                        ++newLabel;
                    }
                }

                // Remap
                for (int i = 0; i < pixels.length; i++) {
                    if (mapping.containsKey(pixels[i])) {
                        pixels[i] = mapping.get(pixels[i]);
                    }
                }

            }, progressInfo.resolve("Processing"));
        } else if (image.getBitDepth() == 32) {
            ByteProcessor tempProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
            ImageJUtils.forEachIndexedZCTSliceWithProgress(image, (ip, index, stackProgress) -> {
                ImageProcessor mask = getMask(ip.getWidth(),
                        ip.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );

                // Collect existing labels
                byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
                float[] pixels = (float[]) ip.getPixels();
                TFloatSet existingLabels = new TFloatHashSet();
                for (int i = 0; i < pixels.length; i++) {
                    if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                        existingLabels.add(pixels[i]);
                    }
                }
                progressInfo.log("Found " + existingLabels.size() + " labels");

                // Sort the labels
                TFloatList sortedExistingLabels = new TFloatArrayList(existingLabels.toArray());
                if (sortOrder == SortOrder.Ascending) {
                    sortedExistingLabels.sort();
                } else {
                    sortedExistingLabels.sort();
                    sortedExistingLabels.reverse();
                }

                // Mapping
                int newLabel = 1;
                TFloatFloatMap mapping = new TFloatFloatHashMap();

                // Go through each label
                tempProcessor.setColor(0);
                tempProcessor.fill();
                int lastPercentage = 0;
                for (int labelIndex = 0; labelIndex < sortedExistingLabels.size(); labelIndex++) {

                    final float targetLabel = sortedExistingLabels.get(labelIndex);

                    if (excludeZero && targetLabel == 0)
                        continue;

                    // Give progress info
                    int newPercentage = (int) (1.0 * labelIndex / sortedExistingLabels.size() * 100);
                    if (newPercentage != lastPercentage) {
                        stackProgress.log("Processing labels ... " + newPercentage + "%");
                        lastPercentage = newPercentage;
                    }

                    // Write the label into temp
                    byte[] tempPixels = (byte[]) tempProcessor.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        if (pixels[i] == targetLabel && (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0)) {
                            tempPixels[i] = (byte) 255;
                        }
                    }

                    // Remap
                    mapping.put(targetLabel, newLabel);

                    // Apply edt and calculate the maximum
                    EDM edm = new EDM();
                    FloatProcessor edtProcessor = edm.makeFloatEDM(tempProcessor, 0, true);
                    if (edtProcessor == null) {
                        stackProgress.log("EDT could not be calculated for label ");
                        continue;
                    }
                    double thickness = edtProcessor.getStats().max * 2;
                    if (thickness >= minThickness) {
                        // Increment the label and reset temp
                        tempProcessor.setColor(0);
                        tempProcessor.fill();
                        ++newLabel;
                    }
                }

                // Remap
                for (int i = 0; i < pixels.length; i++) {
                    if (mapping.containsKey(pixels[i])) {
                        pixels[i] = mapping.get(pixels[i]);
                    }
                }

            }, progressInfo.resolve("Processing"));
        } else {
            throw new UnsupportedOperationException("Unsupported bit depth: " + image.getBitDepth());
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(image), progressInfo);
    }

    private ImageProcessor getMask(int width, int height, ROI2DListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    @SetJIPipeDocumentation(name = "Extract values from ...", description = "Determines from which image areas the pixel values are extracted")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @SetJIPipeDocumentation(name = "Minimum thickness", description = "The minimum thickness of the output labels (according to Euclidean Distance Transform)")
    @JIPipeParameter(value = "minimum-thickness", important = true)
    public int getMinThickness() {
        return minThickness;
    }

    @JIPipeParameter("minimum-thickness")
    public void setMinThickness(int minThickness) {
        this.minThickness = minThickness;
    }

    @SetJIPipeDocumentation(name = "Exclude zero", description = "If enabled, do not apply the algorithm to the zero label.")
    @JIPipeParameter("exclude-zero")
    public boolean isExcludeZero() {
        return excludeZero;
    }

    @JIPipeParameter("exclude-zero")
    public void setExcludeZero(boolean excludeZero) {
        this.excludeZero = excludeZero;
    }

    @SetJIPipeDocumentation(name = "Sort order", description = "Determines from which direction the merging is applied (from lowest/ascending or highest/descending)")
    @JIPipeParameter("sort-order")
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @JIPipeParameter("sort-order")
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
