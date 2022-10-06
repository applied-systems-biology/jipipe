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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.graph.InputSlotMapParameterCollection;

import java.util.*;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Blend images", description = "Overlays greyscale or RGB images.")
@JIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class BlendImagesAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection layers;
    private boolean renderGreyscaleImages = true;

    public BlendImagesAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Output", "The output image", ImagePlusColorRGBData.class, null)
                .restrictInputTo(ImagePlusData.class)
                .sealOutput()
                .build());
        layers = new InputSlotMapParameterCollection(Layer.class, this, this::getNewChannel, false);
        layers.updateSlots();
        registerSubParameter(layers);
    }

    public BlendImagesAlgorithm(BlendImagesAlgorithm other) {
        super(other);
        this.renderGreyscaleImages = other.renderGreyscaleImages;
        layers = new InputSlotMapParameterCollection(Layer.class, this, this::getNewChannel, false);
        other.layers.copyTo(layers);
        registerSubParameter(layers);
    }

    private Layer getNewChannel(JIPipeDataSlotInfo info) {
        Layer layer = new Layer();
        layer.priority = layers.getParameters().values().stream().map(access -> access.get(Layer.class).priority + 1).max(Comparator.naturalOrder()).orElse(0);
        return layer;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @JIPipeDocumentation(name = "Render greyscale images to RGB", description = "If enabled, greyscale images are fully rendered to RGB, including their LUT. Otherwise, the greyscale images are only converted to RGB; the LUT is ignored.")
    @JIPipeParameter("render-greyscale-images")
    public boolean isRenderGreyscaleImages() {
        return renderGreyscaleImages;
    }

    @JIPipeParameter("render-greyscale-images")
    public void setRenderGreyscaleImages(boolean renderGreyscaleImages) {
        this.renderGreyscaleImages = renderGreyscaleImages;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> inputImages = new ArrayList<>();
        Map<JIPipeDataSlot, Layer> channelMap = new HashMap<>();
        Map<JIPipeDataSlot, ImagePlus> channelInputMap = new HashMap<>();
        for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
            ImagePlus image = dataBatch.getInputData(inputSlot, ImagePlusData.class, progressInfo).getImage();

            if (image.getType() != ImagePlus.COLOR_RGB) {
                if (renderGreyscaleImages) {
                    image = ImageJUtils.renderToRGBWithLUTIfNeeded(image, progressInfo.resolve("Render to RGB"));
                } else {
                    image = ImageJUtils.convertToColorRGBIfNeeded(image);
                }
            }

            Layer layerInfo = layers.getParameter(inputSlot.getName(), Layer.class);
            channelMap.put(inputSlot, layerInfo);
            channelInputMap.put(inputSlot, image);
            inputImages.add(image);
        }
        if (inputImages.isEmpty())
            return;
        if (!ImageJUtils.imagesHaveSameSize(inputImages)) {
            throw new UserFriendlyRuntimeException("Input images do not have the same size!",
                    "Input images do not have the same size!",
                    getName(),
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels.",
                    "Please check the input images.");
        }
        final int sx = inputImages.get(0).getWidth();
        final int sy = inputImages.get(0).getHeight();
        final int sz = inputImages.get(0).getNSlices();
        final int sc = inputImages.get(0).getNChannels();
        final int st = inputImages.get(0).getNFrames();
        ImagePlus resultImage = IJ.createHyperStack(getDisplayName(), sx, sy, sc, sz, st, 24);

        List<JIPipeInputDataSlot> sortedSlots = getDataInputSlots().stream().sorted(Comparator.comparing(slot -> channelMap.get(slot).priority)
                .thenComparing(slot -> getDataInputSlots().indexOf(slot))).collect(Collectors.toList());

        ImageJUtils.forEachIndexedZCTSlice(resultImage, (resultIp, index) -> {
            for (JIPipeDataSlot inputSlot : sortedSlots) {
                Layer layer = channelMap.get(inputSlot);
                ImagePlus inputImage = channelInputMap.get(inputSlot);
                ImageProcessor inputIp = ImageJUtils.getSliceZero(inputImage, index);
                double opacity = Math.max(0, Math.min(1, layer.opacity));

                int[] inputPixels = (int[]) inputIp.getPixels();
                int[] outputPixels = (int[]) resultIp.getPixels();

                for (int i = 0; i < outputPixels.length; i++) {
                    outputPixels[i] = layer.blendMode.blend(outputPixels[i], inputPixels[i], opacity);
                }
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(resultImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Layers", description = "Modify here how the images (layers) are merged. The order is determined by the priority value (lower values indicating a bottom layer) and if equal by the order of input slots.")
    @JIPipeParameter("layers")
    public InputSlotMapParameterCollection getLayers() {
        return layers;
    }

    @JIPipeDocumentationDescription(description = "See https://en.wikipedia.org/wiki/Blend_modes")
    public enum BlendMode {
        Normal("Normal"),
        Multiply("Multiply"),
        DivideBA("Divide (top / bottom)"),
        DivideAB("Divide (bottom / top)"),
        Add("Add"),
        Minimum("Minimum (darken)"),
        Maximum("Maximum (lighten)"),
        SubtractAB("Subtract (bottom - top)"),
        SubtractBA("Subtract (top - bottom)"),
        Difference("Difference"),
        Overlay("Overlay"),
        Screen("Screen");

        private final String label;

        BlendMode(String label) {

            this.label = label;
        }

        public int blend(int aRGB, int bRGB, double opacity) {
            final int ar, ag, ab, br, bg, bb;
            ar = (aRGB & 0xff0000) >> 16;
            ag = (aRGB & 0xff00) >> 8;
            ab = aRGB & 0xff;
            br = (bRGB & 0xff0000) >> 16;
            bg = (bRGB & 0xff00) >> 8;
            bb = bRGB & 0xff;
            final int or, og, ob;

            // Mixing
            switch (this) {
                case Normal: {
                    or = br;
                    og = bg;
                    ob = bb;
                }
                break;
                case Overlay: {
                    if (ar < 127)
                        or = Math.min(255, 2 * ar * br);
                    else
                        or = (int) Math.min(255, (255.0 * (1.0 - 2.0 * (1.0 - (ar / 255.0)) * (1.0 - (br / 255.0)))));
                    if (ag < 127)
                        og = Math.min(255, 2 * ag * bg);
                    else
                        og = (int) Math.min(255, (255.0 * (1.0 - 2.0 * (1.0 - (ag / 255.0)) * (1.0 - (bg / 255.0)))));
                    if (ab < 127)
                        ob = Math.min(255, 2 * ab * bb);
                    else
                        ob = (int) Math.min(255, (255.0 * (1.0 - 2.0 * (1.0 - (ab / 255.0)) * (1.0 - (bb / 255.0)))));
                }
                break;
                case Screen: {
                    or = (int) (255.0 * (1.0 - (1.0 - ar / 255.0) * (1.0 - br / 255.0)));
                    og = (int) (255.0 * (1.0 - (1.0 - ag / 255.0) * (1.0 - bg / 255.0)));
                    ob = (int) (255.0 * (1.0 - (1.0 - ab / 255.0) * (1.0 - bb / 255.0)));
                }
                break;
                case Difference: {
                    or = Math.abs(br - ar);
                    og = Math.abs(bg - ag);
                    ob = Math.abs(bb - ab);
                }
                break;
                case Minimum: {
                    or = Math.min(br, ar);
                    og = Math.min(bg, ag);
                    ob = Math.min(bb, ab);
                }
                break;
                case Maximum: {
                    or = Math.max(br, ar);
                    og = Math.max(bg, ag);
                    ob = Math.max(bb, ab);
                }
                break;
                case Multiply: {
                    or = (int) (((br / 255.0) * (ar / 255.0)) * 255);
                    og = (int) (((bg / 255.0) * (ag / 255.0)) * 255);
                    ob = (int) (((bb / 255.0) * (ab / 255.0)) * 255);
                }
                break;
                case Add: {
                    or = Math.min(255, ar + br);
                    og = Math.min(255, ag + bg);
                    ob = Math.min(255, ab + bb);
                }
                break;
                case SubtractAB: {
                    or = Math.max(0, ar - br);
                    og = Math.max(0, ag - bg);
                    ob = Math.max(0, ab - bb);
                }
                break;
                case SubtractBA: {
                    or = Math.max(0, br - ar);
                    og = Math.max(0, bg - ag);
                    ob = Math.max(0, bb - ab);
                }
                break;
                case DivideBA: {
                    if (ar > 0)
                        or = (int) (((br / 255.0) / (ar / 255.0)) * 255);
                    else
                        or = 255;
                    if (ag > 0)
                        og = (int) (((bg / 255.0) / (ag / 255.0)) * 255);
                    else
                        og = 255;
                    if (ab > 0)
                        ob = (int) (((bb / 255.0) / (ab / 255.0)) * 255);
                    else
                        ob = 255;
                }
                break;
                case DivideAB: {
                    if (br > 0)
                        or = (int) (((ar / 255.0) / (br / 255.0)) * 255);
                    else
                        or = 255;
                    if (bg > 0)
                        og = (int) (((ag / 255.0) / (bg / 255.0)) * 255);
                    else
                        og = 255;
                    if (bb > 0)
                        ob = (int) (((ab / 255.0) / (bb / 255.0)) * 255);
                    else
                        ob = 255;
                }
                break;
                default:
                    throw new UnsupportedOperationException("Unsupported blend mode " + this);
            }

            // Alpha blending
            int r = Math.min(255, Math.max((int) (opacity * or + (1.0 - opacity) * ar), 0));
            int g = Math.min(255, Math.max((int) (opacity * og + (1.0 - opacity) * ag), 0));
            int b = Math.min(255, Math.max((int) (opacity * ob + (1.0 - opacity) * ab), 0));

            return b + (g << 8) + (r << 16);
        }


        @Override
        public String toString() {
            return label;
        }
    }

    public static class Layer extends AbstractJIPipeParameterCollection {

        private int priority = 0;

        private BlendMode blendMode = BlendMode.Screen;
        private double opacity = 1;

        public Layer() {
        }

        public Layer(Layer other) {
            this.priority = other.priority;
            this.blendMode = other.blendMode;
            this.opacity = other.opacity;
        }

        @JIPipeDocumentation(name = "Priority (lower = earlier)")
        @JIPipeParameter("order")
        public int getPriority() {
            return priority;
        }

        @JIPipeParameter("order")
        public void setPriority(int priority) {
            this.priority = priority;
        }

        @JIPipeDocumentation(name = "Blend mode")
        @JIPipeParameter("blend-mode")
        public BlendMode getBlendMode() {
            return blendMode;
        }

        @JIPipeParameter("blend-mode")
        public void setBlendMode(BlendMode blendMode) {
            this.blendMode = blendMode;
        }

        @JIPipeDocumentation(name = "Opacity")
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
