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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @deprecated Use {@link BlendImagesAlgorithm} instead
 */
@SetJIPipeDocumentation(name = "Overlay images", description = "Overlays greyscale or RGB images RGB image. ")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Merge Channels... (overlay)")
@Deprecated
@LabelAsJIPipeHidden
public class OverlayImagesAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection channelColorAssignment;

    public OverlayImagesAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().restrictInputTo(ImagePlusGreyscale8UData.class, ImagePlusColorRGBData.class)
                .addOutputSlot("Output", "", ImagePlusColorRGBData.class)
                .sealOutput()
                .build());
        channelColorAssignment = new InputSlotMapParameterCollection(Channel.class, this, this::getNewChannelColor, false);
        channelColorAssignment.updateSlots();
        registerSubParameter(channelColorAssignment);
    }

    public OverlayImagesAlgorithm(OverlayImagesAlgorithm other) {
        super(other);
        channelColorAssignment = new InputSlotMapParameterCollection(Channel.class, this, this::getNewChannelColor, false);
        other.channelColorAssignment.copyTo(channelColorAssignment);
        registerSubParameter(channelColorAssignment);
    }

    private Channel getNewChannelColor(JIPipeDataSlotInfo info) {
        int index = channelColorAssignment.getParameters().size() % 7 - 1;
        switch (index) {
            case 0:
                Channel channel = new Channel(Color.RED, 1);
                channel.setBlackToAlpha(false);
                return channel;
            case 1:
                return new Channel(Color.GREEN);
            case 2:
                return new Channel(Color.BLUE);
            case 3:
                return new Channel(Color.WHITE);
            case 4:
                return new Channel(Color.CYAN);
            case 5:
                return new Channel(Color.MAGENTA);
            default:
                return new Channel(Color.YELLOW);
        }
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> inputImages = new ArrayList<>();
        Map<JIPipeDataSlot, Channel> channelMap = new HashMap<>();
        Map<JIPipeDataSlot, ImagePlus> channelInputMap = new HashMap<>();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            ImagePlus image = ((ImagePlusData) iterationStep.getInputData(inputSlot, inputSlot.getAcceptedDataType(), progressInfo)).getImage();
            if (image.getType() != ImagePlus.COLOR_RGB) {
                image = ImageJUtils.convertToGreyscale8UIfNeeded(image);
            }
            Channel channelInfo = channelColorAssignment.getParameter(inputSlot.getName(), Channel.class);
            channelMap.put(inputSlot, channelInfo);
            channelInputMap.put(inputSlot, image);
            inputImages.add(image);
        }
        if (inputImages.isEmpty())
            return;
        if (!ImageJUtils.imagesHaveSameSize(inputImages)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }
        final int sx = inputImages.get(0).getWidth();
        final int sy = inputImages.get(0).getHeight();
        final int sz = inputImages.get(0).getNSlices();
        final int sc = inputImages.get(0).getNChannels();
        final int st = inputImages.get(0).getNFrames();
        ImagePlus resultImage = IJ.createHyperStack(getDisplayName(), sx, sy, sc, sz, st, 24);

        ImageJUtils.forEachIndexedZCTSlice(resultImage, (resultIp, index) -> {
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                Channel channel = channelMap.get(inputSlot);
                ImagePlus inputImage = channelInputMap.get(inputSlot);
                ImageProcessor inputIp = ImageJUtils.getSliceZero(inputImage, index);
                double channelOpacity = Math.max(0, Math.min(1, channel.opacity));

                final boolean isRGB = inputIp instanceof ColorProcessor;
                final int cr = channel.color.getRed();
                final int cg = channel.color.getGreen();
                final int cb = channel.color.getBlue();

                byte[] inputBytes8U = null;
                int[] inputBytesRGB = null;
                if (isRGB) {
                    inputBytesRGB = (int[]) inputIp.getPixels();
                } else {
                    inputBytes8U = (byte[]) inputIp.getPixels();
                }

                int[] resultBytes = (int[]) resultIp.getPixels();
                for (int i = 0; i < resultBytes.length; i++) {

                    // Source color (apply LUT if greyscale)
                    int rs, gs, bs;
                    double opacity;

                    if (isRGB) {
                        rs = (inputBytesRGB[i] & 0xff0000) >> 16;
                        gs = (inputBytesRGB[i] & 0xff00) >> 8;
                        bs = inputBytesRGB[i] & 0xff;

                        if (channel.blackToAlpha) {
                            int value = (rs + gs + bs) / 255;
                            opacity = channelOpacity * (value / 255.0);
                        } else {
                            opacity = channelOpacity;
                        }
                    } else {
                        int vs = Byte.toUnsignedInt(inputBytes8U[i]);
                        rs = (int) (vs / 255.0 * cr);
                        gs = (int) (vs / 255.0 * cg);
                        bs = (int) (vs / 255.0 * cb);

                        if (channel.blackToAlpha) {
                            opacity = channelOpacity * (vs / 255.0);
                        } else {
                            opacity = channelOpacity;
                        }
                    }

                    // Calculate target color
                    int rt = (resultBytes[i] & 0xff0000) >> 16;
                    int gt = (resultBytes[i] & 0xff00) >> 8;
                    int bt = resultBytes[i] & 0xff;
                    int r = Math.min(255, Math.max((int) (opacity * rs + (1.0 - opacity) * rt), 0));
                    int g = Math.min(255, Math.max((int) (opacity * gs + (1.0 - opacity) * gt), 0));
                    int b = Math.min(255, Math.max((int) (opacity * bs + (1.0 - opacity) * bt), 0));
                    int rgb = b + (g << 8) + (r << 16);
                    resultBytes[i] = rgb;
                }
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(resultImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Channels", description = "Modify here how channels are merged. Please note that the color setting has no effect on RGB images.")
    @JIPipeParameter("channel-color-assignments")
    public InputSlotMapParameterCollection getChannelColorAssignment() {
        return channelColorAssignment;
    }

    public static class Channel extends AbstractJIPipeParameterCollection {
        private Color color = Color.RED;
        private double opacity = 0.5;
        private boolean blackToAlpha = true;

        public Channel() {
        }

        public Channel(Color color) {
            this.color = color;
        }

        public Channel(Color color, double opacity) {
            this.color = color;
            this.opacity = opacity;
        }

        public Channel(Channel other) {
            this.color = other.color;
            this.opacity = other.opacity;
        }

        @SetJIPipeDocumentation(name = "Correct for black background")
        @JIPipeParameter("black-to-alpha")
        public boolean isBlackToAlpha() {
            return blackToAlpha;
        }

        @JIPipeParameter("black-to-alpha")
        public void setBlackToAlpha(boolean blackToAlpha) {
            this.blackToAlpha = blackToAlpha;
        }

        @SetJIPipeDocumentation(name = "Color")
        @JIPipeParameter("color")
        @JsonGetter("color")
        public Color getColor() {
            return color;
        }

        @JIPipeParameter("color")
        @JsonSetter("color")
        public void setColor(Color color) {
            this.color = color;
        }

        @SetJIPipeDocumentation(name = "Opacity")
        @JIPipeParameter("opacity")
        @JsonGetter("opacity")
        public double getOpacity() {
            return opacity;
        }

        @JIPipeParameter("opacity")
        @JsonSetter("opacity")
        public void setOpacity(double opacity) {
            this.opacity = opacity;
        }
    }
}
