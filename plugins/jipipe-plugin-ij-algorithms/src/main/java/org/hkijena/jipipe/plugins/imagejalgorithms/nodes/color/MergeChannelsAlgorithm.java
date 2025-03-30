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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.CompositeConverter;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Merge channels (classic)", description = "Merges each greyscale image plane into a multi-channel image. " +
        "Implements the standard ImageJ algorithm for merging channels that either creates an RGB or a composite image. " +
        "We recommend to use Blend images for creating renders and Merge channels (composite) that provide more options for the specific use cases.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Merge Channels...")
public class MergeChannelsAlgorithm extends JIPipeIteratingAlgorithm {
    private final InputSlotMapParameterCollection channelColorAssignment;
    private boolean createComposite = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().restrictInputTo(ImagePlusGreyscaleData.class)
                .restrictInputSlotCount(ChannelColor.values().length)
                .addOutputSlot("Output", "", ImagePlusData.class)
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

    private ChannelColor getNewChannelColor(JIPipeDataSlotInfo info) {
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
        return false;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        RGBStackMerge rgbStackMerge = new RGBStackMerge();
        
        ImagePlus[] images = new ImagePlus[ChannelColor.values().length];
        ImagePlus firstImage = null;
        for (int i = 0; i < ChannelColor.values().length; ++i) {
            ChannelColor color = ChannelColor.values()[i];
            for (Map.Entry<String, JIPipeParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
                ChannelColor entryColor = entry.getValue().get(ChannelColor.class);
                if (entryColor == color) {
                    images[i] = new ImagePlusGreyscaleData(iterationStep.getInputData(entry.getKey(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage()).getImage();
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
            imp2 = rgbStackMerge.mergeHyperstacks(images, true);
            if (imp2 == null) return;
        } else {
            ImageStack rgb = rgbStackMerge.mergeStacks(width, height, stackSize, stacks[0], stacks[1], stacks[2], true);
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

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp2), progressInfo);
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        Set<ChannelColor> existing = new HashSet<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : channelColorAssignment.getParameters().entrySet()) {
            ChannelColor color = entry.getValue().get(ChannelColor.class);
            if (color == null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new ParameterValidationReportContext(reportContext, this, "Channel colors", "channel-color-assignments"),
                        "No channel color selected!",
                        "Please ensure that all channels are assigned a color."));
            }
            if (color != null) {
                if (existing.contains(color)) {
                    report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new ParameterValidationReportContext(reportContext, this, "Channel colors", "channel-color-assignments"),
                            "Duplicate color assignment!",
                            "Color '" + color + "' is already assigned.",
                            "Please assign another color."));
                }
                existing.add(color);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Channel colors", description = "Assigns a color to the specified input slot")
    @JIPipeParameter("channel-color-assignments")
    public InputSlotMapParameterCollection getChannelColorAssignment() {
        return channelColorAssignment;
    }

    @SetJIPipeDocumentation(name = "Create composite", description = "If true, the generated image is a composite where the color channels are stored as individual greyscale planes. " +
            "If false, the result is an RGB image.")
    @JIPipeParameter("create-composite")
    public boolean isCreateComposite() {
        return createComposite;
    }

    @JIPipeParameter("create-composite")
    public void setCreateComposite(boolean createComposite) {
        this.createComposite = createComposite;
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
