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

package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.*;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * BioFormats importer wrapper
 */
@ACAQDocumentation(name = "Bioformats importer", description = "Imports images via the Bioformats plugin")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Image")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class BioFormatsImporter extends ACAQSimpleIteratingAlgorithm {

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
     * @param declaration the declaration
     */
    public BioFormatsImporter(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", FileData.class)
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData inputFile = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
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
                List<ACAQAnnotation> traits = new ArrayList<>();
                if (titleAnnotation.isEnabled()) {
                    traits.add(new ACAQAnnotation(titleAnnotation.getContent(), image.getTitle()));
                }
                dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), traits);
            }

            if (!process.getOptions().isVirtual())
                process.getReader().close();
        } catch (FormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Title annotation").checkNonEmpty(getTitleAnnotation().getContent(), null);
    }

    @ACAQDocumentation(name = "Color mode")
    @ACAQParameter("color-mode")
    public ColorMode getColorMode() {
        return colorMode;
    }

    @ACAQParameter("color-mode")
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
        getEventBus().post(new ParameterChangedEvent(this, "color-mode"));
    }

    @ACAQDocumentation(name = "Stack order")
    @ACAQParameter("stack-order")
    public Order getStackOrder() {
        return stackOrder;
    }

    @ACAQParameter("stack-order")
    public void setStackOrder(Order stackOrder) {
        this.stackOrder = stackOrder;
        getEventBus().post(new ParameterChangedEvent(this, "stack-order"));
    }

    @ACAQDocumentation(name = "Split channels")
    @ACAQParameter("split-channels")
    public boolean isSplitChannels() {
        return splitChannels;
    }

    @ACAQParameter("split-channels")
    public void setSplitChannels(boolean splitChannels) {
        this.splitChannels = splitChannels;
        getEventBus().post(new ParameterChangedEvent(this, "split-channels"));
    }

    @ACAQDocumentation(name = "Split focal planes")
    @ACAQParameter("split-focal-planes")
    public boolean isSplitFocalPlanes() {
        return splitFocalPlanes;
    }

    @ACAQParameter("split-focal-planes")
    public void setSplitFocalPlanes(boolean splitFocalPlanes) {
        this.splitFocalPlanes = splitFocalPlanes;
        getEventBus().post(new ParameterChangedEvent(this, "split-focal-planes"));
    }

    @ACAQDocumentation(name = "Split time points")
    @ACAQParameter("split-time-points")
    public boolean isSplitTimePoints() {
        return splitTimePoints;
    }

    @ACAQParameter("split-time-points")
    public void setSplitTimePoints(boolean splitTimePoints) {
        this.splitTimePoints = splitTimePoints;
        getEventBus().post(new ParameterChangedEvent(this, "split-time-points"));
    }

    @ACAQDocumentation(name = "Swap dimensions")
    @ACAQParameter("swap-dimensions")
    public boolean isSwapDimensions() {
        return swapDimensions;
    }

    @ACAQParameter("swap-dimensions")
    public void setSwapDimensions(boolean swapDimensions) {
        this.swapDimensions = swapDimensions;
        getEventBus().post(new ParameterChangedEvent(this, "swap-dimensions"));
    }

    @ACAQDocumentation(name = "Concatenate compatible series")
    @ACAQParameter("concatenate")
    public boolean isConcatenate() {
        return concatenate;
    }

    @ACAQParameter("concatenate")
    public void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;
        getEventBus().post(new ParameterChangedEvent(this, "concatenate"));
    }

    @ACAQDocumentation(name = "Crop images")
    @ACAQParameter("crop")
    public boolean isCrop() {
        return crop;
    }

    @ACAQParameter("crop")
    public void setCrop(boolean crop) {
        this.crop = crop;
        getEventBus().post(new ParameterChangedEvent(this, "crop"));
    }

    @ACAQDocumentation(name = "Stitch tiles")
    @ACAQParameter("stitch-tiles")
    public boolean isStitchTiles() {
        return stitchTiles;
    }

    @ACAQParameter("stitch-tiles")
    public void setStitchTiles(boolean stitchTiles) {
        this.stitchTiles = stitchTiles;
        getEventBus().post(new ParameterChangedEvent(this, "stitch-tiles"));
    }

    @ACAQDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @ACAQParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @ACAQParameter("title-annotation")
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
