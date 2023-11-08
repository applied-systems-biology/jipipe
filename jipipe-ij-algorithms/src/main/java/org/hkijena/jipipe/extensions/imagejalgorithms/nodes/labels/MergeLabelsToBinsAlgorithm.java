package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import com.google.common.primitives.Floats;
import gnu.trove.map.TFloatFloatMap;
import gnu.trove.map.TFloatIntMap;
import gnu.trove.map.hash.TFloatFloatHashMap;
import gnu.trove.map.hash.TFloatIntHashMap;
import gnu.trove.set.TFloatSet;
import gnu.trove.set.hash.TFloatHashSet;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Merge labels (bin) 2D", description = "Merges labels into a specified number of of bins. The values are distributed so uniformly unless 'Equalize frequencies' is enabled. The resulting labels are determined by the bin index. " +
        " If the image has multiple slices, the algorithm is applied per slice.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class MergeLabelsToBinsAlgorithm extends JIPipeIteratingAlgorithm {

    private int numBins = 10;
    private boolean excludeZero = true;
    private SortOrder sortOrder = SortOrder.Ascending;
    private boolean equalizeFrequencies = false;

    public MergeLabelsToBinsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeLabelsToBinsAlgorithm(MergeLabelsToBinsAlgorithm other) {
        super(other);
        this.numBins = other.numBins;
        this.excludeZero = other.excludeZero;
        this.sortOrder = other.sortOrder;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus outputImage = iterationStep.getInputData("Input", ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();

        ImageJUtils.forEachIndexedZCTSlice(outputImage, (ip, index) -> {
            float[] pixels = (float[]) ip.getPixels();

            if(equalizeFrequencies) {
                uniformBinning(pixels);
            }
            else {
                equalizedBinning(pixels);
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(outputImage), progressInfo);
    }

    private void equalizedBinning(float[] pixels) {
        TFloatIntMap counts = new TFloatIntHashMap();
        int numValues = 0;
        for (int i = 0; i < pixels.length; i++) {
            float value = pixels[i];
            if (excludeZero && value == 0)
                continue;
            counts.adjustOrPutValue(value, 1, 1);
            ++numValues;
        }

        if(numValues == 0)
            return;

        // Binning

        int numPerBin = numValues / numBins;
        int currentBinNum = 0;
        List<TFloatSet> bins = new ArrayList<>();
        bins.add(new TFloatHashSet());
        float[] keys = counts.keys();
        if(sortOrder == SortOrder.Ascending) {
            Arrays.sort(keys);
        }
        else {
            Floats.sortDescending(keys);
        }

        for (float key : keys) {
            int count = counts.get(key);
            if(currentBinNum + count > numPerBin && currentBinNum > 0) {
                // Shift to next bin
                currentBinNum = count;
                bins.add(new TFloatHashSet());
                bins.get(bins.size() - 1).add(key);
            }
            else {
                // Add to current bin
                bins.get(bins.size() - 1).add(key);
                currentBinNum += count;
            }
        }

        // Create bin map
        TFloatFloatMap pixelMap = new TFloatFloatHashMap();
        for (int i = 0; i < bins.size(); i++) {
            TFloatSet bin = bins.get(i);
            for (float v : bin.toArray()) {
                pixelMap.put(v, i + 1);
            }
        }

        // Apply
        for (int i = 0; i < pixels.length; i++) {
            float value = pixels[i];
            if(excludeZero && value == 0) {
                continue;
            }
            value = pixelMap.get(value);
            pixels[i] = value;
        }
    }

    private void uniformBinning(float[] pixels) {
        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < pixels.length; i++) {
            float value = pixels[i];
            if(excludeZero && value == 0)
                continue;
            max = Math.max(value, max);
            min = Math.min(value, min);
        }

        if(max == min || Float.isInfinite(max)) {
            // Do nothing
            return;
        }

        float binWidth = (max - min) / numBins;

        for (int i = 0; i < pixels.length; i++) {
            float value = pixels[i];
            if (excludeZero && value == 0f) {
                continue;
            }

            value = (int) ((value - min) / binWidth) + 1;
            pixels[i] = value;
        }
    }

    @JIPipeDocumentation(name = "Number of bins", description = "The number of bins")
    @JIPipeParameter(value = "num-bins", important = true)
    public int getNumBins() {
        return numBins;
    }

    @JIPipeParameter("num-bins")
    public void setNumBins(int numBins) {
        this.numBins = numBins;
    }

    @JIPipeDocumentation(name = "Exclude zero", description = "If enabled, do not apply the algorithm to the zero label.")
    @JIPipeParameter("exclude-zero")
    public boolean isExcludeZero() {
        return excludeZero;
    }

    @JIPipeParameter("exclude-zero")
    public void setExcludeZero(boolean excludeZero) {
        this.excludeZero = excludeZero;
    }

    @JIPipeDocumentation(name = "Sort order", description = "Determines from which direction the merging is applied (from lowest/ascending or highest/descending). Only used if 'Equalize frequencies' is enabled.")
    @JIPipeParameter("sort-order")
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @JIPipeParameter("sort-order")
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    @JIPipeDocumentation(name = "Equalize frequencies", description = "If enabled, attempt to equalize the bins. Please note that the last bin might be larger, as remaining values are assigned to higher bins.")
    @JIPipeParameter("equalize-frequencies")
    public boolean isEqualizeFrequencies() {
        return equalizeFrequencies;
    }

    @JIPipeParameter("equalize-frequencies")
    public void setEqualizeFrequencies(boolean equalizeFrequencies) {
        this.equalizeFrequencies = equalizeFrequencies;
    }
}
