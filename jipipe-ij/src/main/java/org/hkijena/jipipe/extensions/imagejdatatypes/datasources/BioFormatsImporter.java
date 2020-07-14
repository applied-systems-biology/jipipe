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

package org.hkijena.jipipe.extensions.imagejdatatypes.datasources;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.*;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * BioFormats importer wrapper
 */
@JIPipeDocumentation(name = "Bioformats importer", description = "Imports images via the Bioformats plugin")
@JIPipeInputSlot(value = FileData.class, slotName = "Files")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
public class BioFormatsImporter extends JIPipeSimpleIteratingAlgorithm {

    private ColorMode colorMode = ColorMode.Default;
    private Order stackOrder = Order.XYCZT;
    private boolean splitChannels;
    private boolean splitFocalPlanes;
    private boolean splitTimePoints;
    private boolean swapDimensions;
    private boolean concatenate;
    private boolean crop;
    private boolean stitchTiles;
    private OptionalStringParameter titleAnnotation = new OptionalStringParameter();

    /**
     * @param info the info
     */
    public BioFormatsImporter(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", FileData.class)
                .addOutputSlot("Output", ImagePlusData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        titleAnnotation.setContent("Image title");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public BioFormatsImporter(BioFormatsImporter other) {
        super(other);
        this.colorMode = other.colorMode;
        this.stackOrder = other.stackOrder;
        this.splitChannels = other.splitChannels;
        this.splitFocalPlanes = other.splitFocalPlanes;
        this.splitTimePoints = other.splitTimePoints;
        this.swapDimensions = other.swapDimensions;
        this.concatenate = other.concatenate;
        this.crop = other.crop;
        this.stitchTiles = other.stitchTiles;
        this.titleAnnotation = new OptionalStringParameter(other.titleAnnotation);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData inputFile = dataBatch.getInputData(getFirstInputSlot(), FileData.class);
        ImporterOptions options;
        try {
            options = new ImporterOptions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        options.setId(inputFile.getPath().toString());
        options.setWindowless(true);
        options.setQuiet(true);
        options.setShowMetadata(false);
        options.setShowOMEXML(false);
        options.setShowROIs(false);
        options.setColorMode(colorMode.name());
        options.setStackOrder(stackOrder.name());
        options.setSplitChannels(splitChannels);
        options.setSplitFocalPlanes(splitFocalPlanes);
        options.setSplitTimepoints(splitTimePoints);
        options.setSwapDimensions(swapDimensions);
        options.setConcatenate(concatenate);
        options.setCrop(crop);
        options.setStitchTiles(stitchTiles);

        ImportProcess process = new ImportProcess(options);

        try {
            new ImporterPrompter(process);
            process.execute();

            DisplayHandler displayHandler = new DisplayHandler(process);
            displayHandler.displayOriginalMetadata();
            displayHandler.displayOMEXML();

            ImagePlusReader reader = new ImagePlusReader(process);
            ImagePlus[] images = reader.openImagePlus();

            for (ImagePlus image : images) {
                List<JIPipeAnnotation> traits = new ArrayList<>();
                if (titleAnnotation.isEnabled()) {
                    traits.add(new JIPipeAnnotation(titleAnnotation.getContent(), image.getTitle()));
                }
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), traits);
            }

            if (!process.getOptions().isVirtual())
                process.getReader().close();
        } catch (FormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Title annotation").checkNonEmpty(getTitleAnnotation().getContent(), null);
    }

    @JIPipeDocumentation(name = "Color mode")
    @JIPipeParameter("color-mode")
    public ColorMode getColorMode() {
        return colorMode;
    }

    @JIPipeParameter("color-mode")
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;

    }

    @JIPipeDocumentation(name = "Stack order")
    @JIPipeParameter("stack-order")
    public Order getStackOrder() {
        return stackOrder;
    }

    @JIPipeParameter("stack-order")
    public void setStackOrder(Order stackOrder) {
        this.stackOrder = stackOrder;

    }

    @JIPipeDocumentation(name = "Split channels")
    @JIPipeParameter("split-channels")
    public boolean isSplitChannels() {
        return splitChannels;
    }

    @JIPipeParameter("split-channels")
    public void setSplitChannels(boolean splitChannels) {
        this.splitChannels = splitChannels;

    }

    @JIPipeDocumentation(name = "Split focal planes")
    @JIPipeParameter("split-focal-planes")
    public boolean isSplitFocalPlanes() {
        return splitFocalPlanes;
    }

    @JIPipeParameter("split-focal-planes")
    public void setSplitFocalPlanes(boolean splitFocalPlanes) {
        this.splitFocalPlanes = splitFocalPlanes;

    }

    @JIPipeDocumentation(name = "Split time points")
    @JIPipeParameter("split-time-points")
    public boolean isSplitTimePoints() {
        return splitTimePoints;
    }

    @JIPipeParameter("split-time-points")
    public void setSplitTimePoints(boolean splitTimePoints) {
        this.splitTimePoints = splitTimePoints;

    }

    @JIPipeDocumentation(name = "Swap dimensions")
    @JIPipeParameter("swap-dimensions")
    public boolean isSwapDimensions() {
        return swapDimensions;
    }

    @JIPipeParameter("swap-dimensions")
    public void setSwapDimensions(boolean swapDimensions) {
        this.swapDimensions = swapDimensions;

    }

    @JIPipeDocumentation(name = "Concatenate compatible series")
    @JIPipeParameter("concatenate")
    public boolean isConcatenate() {
        return concatenate;
    }

    @JIPipeParameter("concatenate")
    public void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;

    }

    @JIPipeDocumentation(name = "Crop images")
    @JIPipeParameter("crop")
    public boolean isCrop() {
        return crop;
    }

    @JIPipeParameter("crop")
    public void setCrop(boolean crop) {
        this.crop = crop;

    }

    @JIPipeDocumentation(name = "Stitch tiles")
    @JIPipeParameter("stitch-tiles")
    public boolean isStitchTiles() {
        return stitchTiles;
    }

    @JIPipeParameter("stitch-tiles")
    public void setStitchTiles(boolean stitchTiles) {
        this.stitchTiles = stitchTiles;

    }

    @JIPipeDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalStringParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    /**
     * Wrapper around Bioformats color modes
     */
    public enum ColorMode {
        Default,
        Composite,
        Colorized,
        Grayscale,
        Custom
    }

    /**
     * Wrapper around Bioformats plane orders
     */
    public enum Order {
        Default,
        XYZCT,
        XYZTC,
        XYCZT,
        XYTCZ,
        XYCTZ,
        XYTZC
    }
}
