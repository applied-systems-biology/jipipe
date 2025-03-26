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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.blending.ImageBlendLayer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;

import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Blend images", description = "Overlays greyscale or RGB images.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input")
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output")
public class BlendImagesAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection layers;
    private boolean renderGreyscaleImages = true;

    public BlendImagesAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Output", "The output image", ImagePlusColorRGBData.class)
                .restrictInputTo(ImagePlusData.class)
                .sealOutput()
                .build());
        layers = new InputSlotMapParameterCollection(ImageBlendLayer.class, this, this::getNewChannel, false);
        layers.updateSlots();
        registerSubParameter(layers);
    }

    public BlendImagesAlgorithm(BlendImagesAlgorithm other) {
        super(other);
        this.renderGreyscaleImages = other.renderGreyscaleImages;
        layers = new InputSlotMapParameterCollection(ImageBlendLayer.class, this, this::getNewChannel, false);
        other.layers.copyTo(layers);
        registerSubParameter(layers);
    }

    private ImageBlendLayer getNewChannel(JIPipeDataSlotInfo info) {
        ImageBlendLayer layer = new ImageBlendLayer();
        layer.setPriority(layers.getParameters().values().stream().map(access -> access.get(ImageBlendLayer.class).getPriority() + 1).max(Comparator.naturalOrder()).orElse(0));
        return layer;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @SetJIPipeDocumentation(name = "Render greyscale images to RGB", description = "If enabled, greyscale images are fully rendered to RGB, including their LUT. Otherwise, the greyscale images are only converted to RGB; the LUT is ignored.")
    @JIPipeParameter("render-greyscale-images")
    public boolean isRenderGreyscaleImages() {
        return renderGreyscaleImages;
    }

    @JIPipeParameter("render-greyscale-images")
    public void setRenderGreyscaleImages(boolean renderGreyscaleImages) {
        this.renderGreyscaleImages = renderGreyscaleImages;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> inputImages = new ArrayList<>();
        Map<JIPipeDataSlot, ImageBlendLayer> channelMap = new HashMap<>();
        Map<JIPipeDataSlot, ImagePlus> channelInputMap = new HashMap<>();
        for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
            ImagePlus image = iterationStep.getInputData(inputSlot, ImagePlusData.class, progressInfo).getImage();

            if (image.getType() != ImagePlus.COLOR_RGB) {
                if (renderGreyscaleImages) {
                    image = ImageJUtils.renderToRGBWithLUTIfNeeded(image, progressInfo.resolve("Render to RGB"));
                } else {
                    image = ImageJUtils.convertToColorRGBIfNeeded(image);
                }
            }

            ImageBlendLayer layerInfo = layers.getParameter(inputSlot.getName(), ImageBlendLayer.class);
            channelMap.put(inputSlot, layerInfo);
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

        List<JIPipeInputDataSlot> sortedSlots = getDataInputSlots().stream().sorted(Comparator.comparing(slot -> channelMap.get(slot).getPriority())
                .thenComparing(slot -> getDataInputSlots().indexOf(slot))).collect(Collectors.toList());

        ImageJIterationUtils.forEachIndexedZCTSlice(resultImage, (resultIp, index) -> {
            for (JIPipeDataSlot inputSlot : sortedSlots) {
                ImageBlendLayer layer = channelMap.get(inputSlot);
                ImagePlus inputImage = channelInputMap.get(inputSlot);
                ImageProcessor inputIp = ImageJUtils.getSliceZero(inputImage, index);
                double opacity = Math.max(0, Math.min(1, layer.getOpacity()));

                int[] inputPixels = (int[]) inputIp.getPixels();
                int[] outputPixels = (int[]) resultIp.getPixels();

                for (int i = 0; i < outputPixels.length; i++) {
                    outputPixels[i] = layer.getBlendMode().blend(outputPixels[i], inputPixels[i], opacity);
                }
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(resultImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Layers", description = "Modify here how the images (layers) are merged. The order is determined by the priority value (lower values indicating a bottom layer) and if equal by the order of input slots.")
    @JIPipeParameter("layers")
    public InputSlotMapParameterCollection getLayers() {
        return layers;
    }

}
