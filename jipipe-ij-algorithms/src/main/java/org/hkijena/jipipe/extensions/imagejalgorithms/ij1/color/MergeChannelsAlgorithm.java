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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.CompositeConverter;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.collections.InputSlotMapParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;
import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_COLOR_RGB_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Merge channels", description = "Merges each greyscale image plane into a multi-channel image. " + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@JIPipeOrganization(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class MergeChannelsAlgorithm extends JIPipeIteratingAlgorithm {

    private static final RGBStackMerge RGB_STACK_MERGE = new RGBStackMerge();
    private InputSlotMapParameterCollection channelColorAssignment;
    private boolean createComposite = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().restrictInputTo(TO_COLOR_RGB_CONVERSION.keySet())
                .restrictInputSlotCount(ChannelColor.values().length)
                .addOutputSlot("Output", ImagePlusData.class, "Input", TO_COLOR_RGB_CONVERSION)
                .allowOutputSlotInheritance(true)
                .sealOutput()
                .build());
        channelColorAssignment = new InputSlotMapParameterCollection(ChannelColor.class, this, this::getNewChannelColor, false);
        channelColorAssignment.updateSlots();
        registerSubParameter(channelColorAssignment);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MergeChannelsAlgorithm(MergeChannelsAlgorithm other) {
        super(other);
        this.createComposite = other.createComposite;
        channelColorAssignment = new InputSlotMapParameterCollection(ChannelColor.class, this, this::getNewChannelColor, false);
        other.channelColorAssignment.copyTo(channelColorAssignment);
        registerSubParameter(channelColorAssignment);
    }

    private ChannelColor getNewChannelColor() {
        for (ChannelColor value : ChannelColor.values()) {
            if (channelColorAssignment.getParameters().values().stream().noneMatch(
                    parameterAccess -> parameterAccess.get(ChannelColor.class) == value)) {
                return value;
            }
        }
        return ChannelColor.Red;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus[] images = new ImagePlus[ChannelColor.values().length];
        ImagePlus firstImage = null;
        for (int i = 0; i < ChannelColor.values().length; ++i) {
            ChannelColor color = ChannelColor.values()[i];
            for (Map.Entry<String, JIPipeParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
                ChannelColor entryColor = entry.getValue().get(ChannelColor.class);
                if (entryColor == color) {
                    images[i] = new ImagePlusGreyscale8UData(dataBatch.getInputData(entry.getKey(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage()).getImage();
                    if (firstImage == null)
                        firstImage = images[i];
                }
            }
        }

        boolean composite = this.createComposite;
        int stackSize = 0;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int slices = 0;
        int frames = 0;
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null && width == 0) {
                width = images[i].getWidth();
                height = images[i].getHeight();
                stackSize = images[i].getStackSize();
                bitDepth = images[i].getBitDepth();
                slices = images[i].getNSlices();
                frames = images[i].getNFrames();
            }
        }
        if (width == 0) {
            throw new RuntimeException("There must be at least one source image or stack.");
        }

        boolean mergeHyperstacks = false;
        for (int i = 0; i < images.length; i++) {
            ImagePlus img = images[i];
            if (img == null) continue;
            if (img.getStackSize() != stackSize) {
                throw new RuntimeException("The source stacks must have the same number of images.");
            }
            if (img.isHyperStack()) {
                if (img.isComposite()) {
                    CompositeImage ci = (CompositeImage) img;
                    if (ci.getMode() != IJ.COMPOSITE) {
                        ci.setMode(IJ.COMPOSITE);
                        img.updateAndDraw();
                        if (!IJ.isMacro()) IJ.run("Channels Tool...");
                        return;
                    }
                }
                if (bitDepth == 24) {
                    throw new RuntimeException("Source hyperstacks cannot be RGB.");
                }
                if (img.getNChannels() > 1) {
                    throw new RuntimeException("Source hyperstacks cannot have more than 1 channel.");
                }
                if (img.getNSlices() != slices || img.getNFrames() != frames) {
                    throw new RuntimeException("Source hyperstacks must have the same dimensions.");
                }
                mergeHyperstacks = true;
            } // isHyperStack
            if (img.getWidth() != width || images[i].getHeight() != height) {
                throw new RuntimeException("The source images or stacks must have the same width and height.");
            }
            if (composite && img.getBitDepth() != bitDepth) {
                throw new RuntimeException("The source images must have the same bit depth.");
            }
        }

        ImageStack[] stacks = new ImageStack[images.length];
        for (int i = 0; i < images.length; i++)
            stacks[i] = images[i] != null ? images[i].getStack() : null;
        ImagePlus imp2;
        boolean fourOrMoreChannelRGB = false;
        for (int i = 3; i < images.length; i++) {
            if (stacks[i] != null) {
                if (!composite)
                    fourOrMoreChannelRGB = true;
                composite = true;
            }
        }
        if (fourOrMoreChannelRGB)
            composite = true;
        boolean isRGB = false;
        int extraIChannels = 0;
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null) {
                if (i > 2)
                    extraIChannels++;
                if (images[i].getBitDepth() == 24)
                    isRGB = true;
            }
        }
        if (isRGB && extraIChannels > 0) {
            imp2 = mergeUsingRGBProjection(firstImage, images, composite);
        } else if ((composite && !isRGB) || mergeHyperstacks) {
            imp2 = RGB_STACK_MERGE.mergeHyperstacks(images, true);
            if (imp2 == null) return;
        } else {
            ImageStack rgb = RGB_STACK_MERGE.mergeStacks(width, height, stackSize, stacks[0], stacks[1], stacks[2], true);
            imp2 = new ImagePlus("RGB", rgb);
            if (composite) {
                imp2 = CompositeConverter.makeComposite(imp2);
                imp2.setTitle("Composite");
            }
        }
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null) {
                imp2.setCalibration(images[i].getCalibration());
                break;
            }
        }
        if (fourOrMoreChannelRGB) {
            if (imp2.getNSlices() == 1 && imp2.getNFrames() == 1) {
                imp2 = imp2.flatten();
                imp2.setTitle("RGB");
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp2), progressInfo);
    }

    private ImagePlus mergeUsingRGBProjection(ImagePlus imp, ImagePlus[] images, boolean createComposite) {
        ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null)
                stack.addSlice(images[i].getProcessor());
        }
        ImagePlus imp2 = new ImagePlus("temp", stack);
        ZProjector zp = new ZProjector(imp2);
        zp.setMethod(ZProjector.MAX_METHOD);
        zp.doRGBProjection();
        imp2 = zp.getProjection();
        if (createComposite) {
            imp2 = CompositeConverter.makeComposite(imp2);
            imp2.setTitle("Composite");
        } else
            imp2.setTitle("RGB");
        return imp2;
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        Set<ChannelColor> existing = new HashSet<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
            ChannelColor color = entry.getValue().get(ChannelColor.class);
            report.forCategory("Channel colors").forCategory(entry.getKey()).checkNonNull(color, this);
            if (color != null) {
                if (existing.contains(color))
                    report.forCategory("Channel colors").forCategory(entry.getKey()).reportIsInvalid("Duplicate color assignment!",
                            "Color '" + color + "' is already assigned.",
                            "Please assign another color.",
                            this);
                existing.add(color);
            }
        }
    }

    @JIPipeDocumentation(name = "Channel colors", description = "Assigns a color to the specified input slot")
    @JIPipeParameter("channel-color-assignments")
    public InputSlotMapParameterCollection getChannelColorAssignment() {
        return channelColorAssignment;
    }

    @JIPipeDocumentation(name = "Create composite", description = "If true, the generated image is a composite where the color channels are stored as individual greyscale planes. " +
            "If false, the result is an RGB image.")
    @JIPipeParameter("create-composite")
    public boolean isCreateComposite() {
        return createComposite;
    }

    @JIPipeParameter("create-composite")
    public void setCreateComposite(boolean createComposite) {
        this.createComposite = createComposite;
    }

    @JIPipeDocumentation(name = "3 channel merge", description = "Loads example parameters that merge three channels.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/channelmixer.png")
    public void setTo3ChannelExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            for (int i = 0; i < 3; i++) {
                slotConfiguration.addSlot("C" + (i + 1), new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Input), true);
            }
        }
    }

    @JIPipeDocumentation(name = "2 channel merge", description = "Loads example parameters that merge two channels.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/channelmixer.png")
    public void setTo2ChannelExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            for (int i = 0; i < 2; i++) {
                slotConfiguration.addSlot("C" + (i + 1), new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Input), true);
            }
        }
    }


    /**
     * Color a slice can be mapped to
     */
    public enum ChannelColor {
        Red,
        Green,
        Blue,
        Gray,
        Cyan,
        Magenta,
        Yellow
    }
}
