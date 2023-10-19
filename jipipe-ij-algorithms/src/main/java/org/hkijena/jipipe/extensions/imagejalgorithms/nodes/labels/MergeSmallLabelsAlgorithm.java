package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import gnu.trove.map.*;
import gnu.trove.map.hash.*;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.utils.ArrayUtils;

import java.util.Arrays;

@JIPipeDocumentation(name = "Merge small labels", description = "Merges labels with a low number of pixels to its neighboring label set (larger or smaller).")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class MergeSmallLabelsAlgorithm extends JIPipeIteratingAlgorithm {

    private int minimumNumberOfPixels = 100;
    private boolean mergeToLargerLabel = false;

    private boolean excludeZero = true;

    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;

    public MergeSmallLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public MergeSmallLabelsAlgorithm(MergeSmallLabelsAlgorithm other) {
        super(other);
        this.minimumNumberOfPixels = other.minimumNumberOfPixels;
        this.mergeToLargerLabel = other.mergeToLargerLabel;
        this.sourceArea = other.sourceArea;
        this.excludeZero = other.excludeZero;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
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

        if (image.getBitDepth() == 8) {
            TByteIntMap counts = new TByteIntHashMap();
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor mask = getMask(ip.getWidth(),
                        ip.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );
                byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
                byte[] pixels = (byte[]) ip.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                        counts.adjustOrPutValue(pixels[i], 1, 1);
                    }
                }
            }, progressInfo.resolve("Count pixels"));

            // Create initial mapping
            byte[] keys = counts.keys();
            TByteByteMap remapping = new TByteByteHashMap();
            for (byte key : keys) {
                remapping.put(key, key);
            }

            // Sort the array
            Arrays.sort(keys);
            if (!mergeToLargerLabel) {
                keys = ArrayUtils.reverse(keys);
            }

            for (int i = 0; i < keys.length; i++) {
                byte key = keys[i];
                int count = counts.get(key);

                if (excludeZero && key == 0) {
                    continue;
                }

                if (count < minimumNumberOfPixels) {
                    Byte targetKey = null;
                    if (i >= keys.length - 1) {
                        // Fallback case
                        for (int j = i - 1; j >= 0; j--) {
                            byte candidateKey = keys[j];
                            if (excludeZero && candidateKey == 0)
                                continue;
                            targetKey = candidateKey;
                            break;
                        }
                    } else {
                        for (int j = i + 1; j < keys.length; j++) {
                            byte candidateKey = keys[j];
                            if (excludeZero && candidateKey == 0)
                                continue;
                            targetKey = candidateKey;
                            break;
                        }
                    }
                    if (targetKey == null) {
                        progressInfo.log("Unable to find target for " + key + " with count " + count);
                        continue;
                    }

                    // Update mapping and counts
                    remapping.put(key, targetKey);
                    counts.adjustValue(targetKey, counts.get(key));
                    counts.put(key, 0);
                }
            }

            // Resolve mapping
            for (byte sourceKey : keys) {
                byte key = sourceKey;
                while (remapping.get(key) != key) {
                    key = remapping.get(key);
                }
                remapping.put(sourceKey, key);
            }

            // Apply remapping
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                byte[] pixels = (byte[]) ip.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = remapping.get(pixels[i]);
                }
            }, progressInfo.resolve("Apply remapping"));
        } else if (image.getBitDepth() == 16) {
            TShortIntMap counts = new TShortIntHashMap();
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor mask = getMask(ip.getWidth(),
                        ip.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );
                byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
                short[] pixels = (short[]) ip.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                        counts.adjustOrPutValue(pixels[i], 1, 1);
                    }
                }
            }, progressInfo.resolve("Count pixels"));

            // Create initial mapping
            short[] keys = counts.keys();
            TShortShortMap remapping = new TShortShortHashMap();
            for (short key : keys) {
                remapping.put(key, key);
            }

            // Sort the array
            Arrays.sort(keys);
            if (!mergeToLargerLabel) {
                keys = ArrayUtils.reverse(keys);
            }

            for (int i = 0; i < keys.length; i++) {
                short key = keys[i];
                int count = counts.get(key);

                if (excludeZero && key == 0) {
                    continue;
                }

                if (count < minimumNumberOfPixels) {
                    Short targetKey = null;
                    if (i >= keys.length - 1) {
                        // Fallback case
                        for (int j = i - 1; j >= 0; j--) {
                            short candidateKey = keys[j];
                            if (excludeZero && candidateKey == 0)
                                continue;
                            targetKey = candidateKey;
                            break;
                        }
                    } else {
                        for (int j = i + 1; j < keys.length; j++) {
                            short candidateKey = keys[j];
                            if (excludeZero && candidateKey == 0)
                                continue;
                            targetKey = candidateKey;
                            break;
                        }
                    }
                    if (targetKey == null) {
                        progressInfo.log("Unable to find target for " + key + " with count " + count);
                        continue;
                    }

                    // Update mapping and counts
                    remapping.put(key, targetKey);
                    counts.adjustValue(targetKey, counts.get(key));
                    counts.put(key, 0);
                }
            }

            // Resolve mapping
            for (short sourceKey : keys) {
                short key = sourceKey;
                while (remapping.get(key) != key) {
                    key = remapping.get(key);
                }
                remapping.put(sourceKey, key);
            }

            // Apply remapping
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                short[] pixels = (short[]) ip.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = remapping.get(pixels[i]);
                }
            }, progressInfo.resolve("Apply remapping"));
        } else if (image.getBitDepth() == 32) {
            TFloatIntMap counts = new TFloatIntHashMap();
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor mask = getMask(ip.getWidth(),
                        ip.getHeight(),
                        finalRoiInput,
                        finalMaskInput,
                        index
                );
                byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
                float[] pixels = (float[]) ip.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                        counts.adjustOrPutValue(pixels[i], 1, 1);
                    }
                }
            }, progressInfo.resolve("Count pixels"));

            progressInfo.log("Calculate remapping");

            // Create initial mapping
            float[] keys = counts.keys();
            TFloatFloatMap remapping = new TFloatFloatHashMap();
            for (float key : keys) {
                remapping.put(key, key);
            }

            // Sort the array
            Arrays.sort(keys);
            if (!mergeToLargerLabel) {
                keys = ArrayUtils.reverse(keys);
            }

            for (int i = 0; i < keys.length; i++) {
                float key = keys[i];
                int count = counts.get(key);

                if (excludeZero && key == 0) {
                    continue;
                }

                if (count < minimumNumberOfPixels) {
                    Float targetKey = null;
                    if (i >= keys.length - 1) {
                        // Fallback case
                        for (int j = i - 1; j >= 0; j--) {
                            float candidateKey = keys[j];
                            if (excludeZero && candidateKey == 0)
                                continue;
                            targetKey = candidateKey;
                            break;
                        }
                    } else {
                        for (int j = i + 1; j < keys.length; j++) {
                            float candidateKey = keys[j];
                            if (excludeZero && candidateKey == 0)
                                continue;
                            targetKey = candidateKey;
                            break;
                        }
                    }
                    if (targetKey == null) {
                        progressInfo.log("Unable to find target for " + key + " with count " + count);
                        continue;
                    }

                    // Update mapping and counts
                    remapping.put(key, targetKey);
                    counts.adjustValue(targetKey, counts.get(key));
                    counts.put(key, 0);
                }
            }

            // Resolve mapping
            for (float sourceKey : keys) {
                float key = sourceKey;
                while (remapping.get(key) != key) {
                    key = remapping.get(key);
                }
                remapping.put(sourceKey, key);
            }

            // Apply remapping
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                float[] pixels = (float[]) ip.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = remapping.get(pixels[i]);
                }
            }, progressInfo.resolve("Apply remapping"));
        } else {
            throw new UnsupportedOperationException("Unsupported bit depth: " + image.getBitDepth());
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(image), progressInfo);
    }

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    @JIPipeDocumentation(name = "Extract values from ...", description = "Determines from which image areas the pixel values are extracted")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @JIPipeDocumentation(name = "Minimum number of pixels", description = "The minimum number of pixels that a label should have")
    @JIPipeParameter(value = "minimum-number-of-pixels", important = true)
    public int getMinimumNumberOfPixels() {
        return minimumNumberOfPixels;
    }

    @JIPipeParameter("minimum-number-of-pixels")
    public void setMinimumNumberOfPixels(int minimumNumberOfPixels) {
        this.minimumNumberOfPixels = minimumNumberOfPixels;
    }

    @JIPipeDocumentation(name = "Merge to ...", description = "Determines to which direction small labels are merged. If set to 'Smaller', small regions will be preferably merged into regions with a smaller label. If set to 'Larger', prefer merging to regions with a higher label.")
    @JIPipeParameter("merge-to-larger-labels")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Larger", falseLabel = "Smaller")
    public boolean isMergeToLargerLabel() {
        return mergeToLargerLabel;
    }

    @JIPipeParameter("merge-to-larger-labels")
    public void setMergeToLargerLabel(boolean mergeToLargerLabel) {
        this.mergeToLargerLabel = mergeToLargerLabel;
    }

    @JIPipeDocumentation(name = "Exclude zero", description = "If enabled, do not apply the algorithm to the zero label. Also prevents merging into the zero-label.")
    @JIPipeParameter("exclude-zero")
    public boolean isExcludeZero() {
        return excludeZero;
    }

    @JIPipeParameter("exclude-zero")
    public void setExcludeZero(boolean excludeZero) {
        this.excludeZero = excludeZero;
    }
}
