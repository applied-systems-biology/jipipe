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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

import java.awt.*;

@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
@SetJIPipeDocumentation(name = "Apply displayed contrast", description = "Applies the displayed contrast settings of the image and writes them into the pixel values. Does nothing if the image is a 32-bit floating point image. " +
        "The operation is applied to all slices.")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Brightness/Contrast... (Apply)")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ApplyDisplayContrastAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ApplyDisplayContrastAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ApplyDisplayContrastAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    public static void apply(ImagePlus imp, ImageProcessor ip) {
        boolean RGBImage = imp.getType() == ImagePlus.COLOR_RGB;
        int bitDepth = imp.getBitDepth();
        String option = null;

        if (RGBImage) {
            if (imp.getStackSize() > 1)
                applyRGBStack(imp);
            else
                applyRGB(imp, ip);
            return;
        }
        if (bitDepth == 32) {
            return;
        }
        int range = 256;
        if (bitDepth == 16) {
            range = 65536;
            int defaultRange = ImagePlus.getDefault16bitRange();
            if (defaultRange > 0)
                range = (int) Math.pow(2, defaultRange) - 1;
        }
        int tableSize = bitDepth == 16 ? 65536 : 256;
        int[] table = new int[tableSize];
        int min = (int) imp.getDisplayRangeMin();
        int max = (int) imp.getDisplayRangeMax();
        for (int i = 0; i < tableSize; i++) {
            if (i <= min)
                table[i] = 0;
            else if (i >= max)
                table[i] = range - 1;
            else
                table[i] = (int) (((double) (i - min) / (max - min)) * range);
        }
        ip.setRoi(imp.getRoi());
        if (imp.getStackSize() > 1 && !imp.isComposite()) {
            int current = imp.getCurrentSlice();
            ImageProcessor mask = imp.getMask();
            for (int i = 1; i <= imp.getStackSize(); i++) {
                imp.setSlice(i);
                ip = imp.getProcessor();
                if (mask != null) ip.snapshot();
                ip.applyTable(table);
                ip.reset(mask);
            }
            imp.setSlice(current);
        } else {
            ip.snapshot();
            ip.applyTable(table);
            ip.reset(ip.getMask());
        }
        reset(imp, ip);
        imp.changes = true;
    }

    public static void applyRGB(ImagePlus imp, ImageProcessor ip) {
        ip.snapshot();
        ip.setMinAndMax(0, 255);
        reset(imp, ip);
		/*
		double min = imp.getDisplayRangeMin();
		double max = imp.getDisplayRangeMax();
 		ip.setRoi(imp.getRoi());
 		ip.reset();
		if (channels!=7)
			((ColorProcessor)ip).setMinAndMax(min, max, channels);
		else
			ip.setMinAndMax(min, max);
		ip.reset(ip.getMask());
		imp.changes = true;
		previousImageID = 0;
	 	((ColorProcessor)ip).caSnapshot(false);
		setup();
		if (Recorder.record) {
			if (Recorder.scriptMode())
				Recorder.recordCall("IJ.run(imp, \"Apply LUT\", \"\");");
			else
				Recorder.record("run", "Apply LUT");
		}
		*/
    }

    public static void applyRGBStack(ImagePlus imp) {
        double min = imp.getDisplayRangeMin();
        double max = imp.getDisplayRangeMax();
//        int channels = imp.getNChannels();
        int current = imp.getCurrentSlice();
        int n = imp.getStackSize();
        ImageProcessor mask = imp.getMask();
        Rectangle roi = imp.getRoi() != null ? imp.getRoi().getBounds() : null;
        ImageStack stack = imp.getStack();
        for (int i = 1; i <= n; i++) {
            if (i != current) {
                ImageProcessor ip = stack.getProcessor(i);
                ip.setRoi(roi);
                if (mask != null) ip.snapshot();
//                if (channels!=7)
//                    ((ColorProcessor)ip).setMinAndMax(min, max, channels);
//                else
//                    ip.setMinAndMax(min, max);
                ip.setMinAndMax(min, max);
                if (mask != null) ip.reset(mask);
            }
        }
        imp.setStack(null, stack);
        imp.setSlice(current);
        imp.changes = true;
    }

    public static void reset(ImagePlus imp, ImageProcessor ip) {
        boolean RGBImage = imp.getType() == ImagePlus.COLOR_RGB;
        if (RGBImage)
            ip.reset();
        int bitDepth = imp.getBitDepth();
        if (bitDepth == 16 || bitDepth == 32) {
            imp.resetDisplayRange();
        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        apply(imagePlus, imagePlus.getProcessor());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }
}
