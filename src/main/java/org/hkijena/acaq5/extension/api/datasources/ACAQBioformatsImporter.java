package org.hkijena.acaq5.extension.api.datasources;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;

import java.io.IOException;

@ACAQDocumentation(name = "Bioformats importer")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMultichannelImageData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQGreyscaleImageData.class)
@AlgorithmOutputSlot(value = ACAQMaskData.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQBioformatsImporter extends ACAQIteratingAlgorithm {

    private ColorMode colorMode = ColorMode.Default;
    private Order stackOrder = Order.XYCZT;
    private boolean splitChannels;
    private boolean splitFocalPlanes;
    private boolean splitTimePoints;
    private boolean swapDimensions;
    private boolean concatenate;
    private boolean crop;
    private boolean stitchTiles;

    public ACAQBioformatsImporter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        ((ACAQMutableSlotConfiguration) getSlotConfiguration()).setOutputSealed(false);
    }

    public ACAQBioformatsImporter(ACAQBioformatsImporter other) {
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
    }

    @Override
    protected void initializeTraits() {
        super.initializeTraits();
        ((ACAQDefaultMutableTraitConfiguration) getTraitConfiguration()).setTraitModificationsSealed(false);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData inputFile = dataInterface.getInputData(getFirstInputSlot());
        ImporterOptions options;
        try {
            options = new ImporterOptions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        options.setId(inputFile.getFilePath().toString());
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

        ImagePlus[] images;

        try {
            new ImporterPrompter(process);
            process.execute();

            DisplayHandler displayHandler = new DisplayHandler(process);
            displayHandler.displayOriginalMetadata();
            displayHandler.displayOMEXML();

            ImagePlusReader reader = new ImagePlusReader(process);
            images = reader.openImagePlus();

            if (!process.getOptions().isVirtual())
                process.getReader().close();
        } catch (FormatException | IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < Math.min(getOutputSlots().size(), images.length); ++i) {
            ACAQDataSlot slot = getOutputSlots().get(i);
            dataInterface.addOutputData(slot, ACAQData.createInstance(slot.getAcceptedDataType(), images[i]));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQDocumentation(name = "Color mode")
    @ACAQParameter("color-mode")
    public ColorMode getColorMode() {
        return colorMode;
    }

    @ACAQParameter("color-mode")
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
    }

    @ACAQDocumentation(name = "Stack order")
    @ACAQParameter("stack-order")
    public Order getStackOrder() {
        return stackOrder;
    }

    @ACAQParameter("stack-order")
    public void setStackOrder(Order stackOrder) {
        this.stackOrder = stackOrder;
    }

    @ACAQDocumentation(name = "Split channels")
    @ACAQParameter("split-channels")
    public boolean isSplitChannels() {
        return splitChannels;
    }

    @ACAQParameter("split-channels")
    public void setSplitChannels(boolean splitChannels) {
        this.splitChannels = splitChannels;
    }

    @ACAQDocumentation(name = "Split focal planes")
    @ACAQParameter("split-focal-planes")
    public boolean isSplitFocalPlanes() {
        return splitFocalPlanes;
    }

    @ACAQParameter("split-focal-planes")
    public void setSplitFocalPlanes(boolean splitFocalPlanes) {
        this.splitFocalPlanes = splitFocalPlanes;
    }

    @ACAQDocumentation(name = "Split time points")
    @ACAQParameter("split-time-points")
    public boolean isSplitTimePoints() {
        return splitTimePoints;
    }

    @ACAQParameter("split-time-points")
    public void setSplitTimePoints(boolean splitTimePoints) {
        this.splitTimePoints = splitTimePoints;
    }

    @ACAQDocumentation(name = "Swap dimensions")
    @ACAQParameter("swap-dimensions")
    public boolean isSwapDimensions() {
        return swapDimensions;
    }

    @ACAQParameter("swap-dimensions")
    public void setSwapDimensions(boolean swapDimensions) {
        this.swapDimensions = swapDimensions;
    }

    @ACAQDocumentation(name = "Concatenate compatible series")
    @ACAQParameter("concatenate")
    public boolean isConcatenate() {
        return concatenate;
    }

    @ACAQParameter("concatenate")
    public void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;
    }

    @ACAQDocumentation(name = "Crop images")
    @ACAQParameter("crop")
    public boolean isCrop() {
        return crop;
    }

    @ACAQParameter("crop")
    public void setCrop(boolean crop) {
        this.crop = crop;
    }

    @ACAQDocumentation(name = "Stitch tiles")
    @ACAQParameter("stitch-tiles")
    public boolean isStitchTiles() {
        return stitchTiles;
    }

    @ACAQParameter("stitch-tiles")
    public void setStitchTiles(boolean stitchTiles) {
        this.stitchTiles = stitchTiles;
    }

    public enum ColorMode {
        Default,
        Composite,
        Colorized,
        Grayscale,
        Custom
    }

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
