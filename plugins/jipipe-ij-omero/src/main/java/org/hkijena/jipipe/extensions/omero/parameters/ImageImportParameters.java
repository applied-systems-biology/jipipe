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

package org.hkijena.jipipe.extensions.omero.parameters;

import ome.xml.model.enums.DimensionOrder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEColorMode;
import org.hkijena.jipipe.extensions.parameters.library.roi.RectangleList;

public class ImageImportParameters extends AbstractJIPipeParameterCollection {
    private OMEColorMode colorMode = OMEColorMode.Default;
    private DimensionOrder stackOrder = DimensionOrder.XYCZT;
    private boolean splitChannels;
    private boolean splitFocalPlanes;
    private boolean splitTimePoints;
    private boolean swapDimensions;
    private boolean concatenate;
    private boolean crop;
    private boolean stitchTiles;
    private boolean autoScale = true;
    private boolean extractRois = true;
    private RectangleList cropRegions = new RectangleList();

    public ImageImportParameters() {

    }

    public ImageImportParameters(ImageImportParameters other) {
        this.colorMode = other.colorMode;
        this.stackOrder = other.stackOrder;
        this.splitChannels = other.splitChannels;
        this.splitFocalPlanes = other.splitFocalPlanes;
        this.splitTimePoints = other.splitTimePoints;
        this.swapDimensions = other.swapDimensions;
        this.concatenate = other.concatenate;
        this.crop = other.crop;
        this.stitchTiles = other.stitchTiles;
        this.autoScale = other.autoScale;
        this.cropRegions = new RectangleList(other.cropRegions);
        this.extractRois = other.extractRois;
    }

    @SetJIPipeDocumentation(name = "Auto scale", description = "Stretches the channel histograms to each channel's global minimum and maximum value throughout the stack. " +
            "Does not alter underlying values in the image.  " +
            "If unselected, all channel histograms are scaled to the image's digitization bit depth. " +
            "Note that this range may be narrower than the bit depth of the file. " +
            "For example, a 16-bit file may contain intensities digitized to 16-bits (0-65,535), " +
            "to 12-bits (0-4,095), to 10-bits (0-1,023), etc.  " +
            "Note that you can use the Brightness & Contrast or Window/Level controls to adjust the contrast range" +
            " regardless of whether this option is used. The histogram will provide min/max values in the stack.")
    @JIPipeParameter("auto-scale")
    public boolean isAutoScale() {
        return autoScale;
    }

    @JIPipeParameter("auto-scale")
    public void setAutoScale(boolean autoScale) {
        this.autoScale = autoScale;
    }

    @SetJIPipeDocumentation(name = "Color mode", description = "Color mode - Visualizes channels according to the specified scheme.  Possible choices are:\n" +
            "Default - Display channels as closely as possible to how they are stored in the file.\n" +
            "Composite - Open as a merged composite image. Channels are colorized according to metadata present in the dataset (if any), or in the following default order: 1=red, 2=green, 3=blue, 4=gray, 5=cyan, 6=magenta, 7=yellow.\n" +
            "Colorized - Open with each channel in a separate plane, colorized according to metadata present in the dataset (if any), or in the default order (see Composite above).\n" +
            "Grayscale - Open with each channel in a separate plane, displayed in plain grayscale.\n" +
            "Custom - Same as Colorized, except that you can explicitly choose how to colorize each channel.\n" +
            "Note that ImageJ can only composite together 7 or fewer channels. With more than 7 channels, some of the modes above may not work.")
    @JIPipeParameter("color-mode")
    public OMEColorMode getColorMode() {
        return colorMode;
    }

    @JIPipeParameter("color-mode")
    public void setColorMode(OMEColorMode colorMode) {
        this.colorMode = colorMode;
    }

    @SetJIPipeDocumentation(name = "Crop regions")
    @JIPipeParameter("crop-regions")
    public RectangleList getCropRegions() {
        return cropRegions;
    }

    @JIPipeParameter("crop-regions")
    public void setCropRegions(RectangleList cropRegions) {
        this.cropRegions = cropRegions;
    }

    @SetJIPipeDocumentation(name = "Stack order")
    @JIPipeParameter("stack-order")
    public DimensionOrder getStackOrder() {
        return stackOrder;
    }

    @JIPipeParameter("stack-order")
    public void setStackOrder(DimensionOrder stackOrder) {
        this.stackOrder = stackOrder;
    }

    @SetJIPipeDocumentation(name = "Split channels", description = "Each channel is opened as a separate stack.  " +
            "This option is especially useful if you want to merge the channels into a specific order, " +
            "rather than automatically assign channels to the order of RGB. The bit depth is preserved.")
    @JIPipeParameter("split-channels")
    public boolean isSplitChannels() {
        return splitChannels;
    }

    @JIPipeParameter("split-channels")
    public void setSplitChannels(boolean splitChannels) {
        this.splitChannels = splitChannels;
    }

    @SetJIPipeDocumentation(name = "Split focal planes", description = "Each focal plane is opened as a separate stack.")
    @JIPipeParameter("split-focal-planes")
    public boolean isSplitFocalPlanes() {
        return splitFocalPlanes;
    }

    @JIPipeParameter("split-focal-planes")
    public void setSplitFocalPlanes(boolean splitFocalPlanes) {
        this.splitFocalPlanes = splitFocalPlanes;
    }

    @SetJIPipeDocumentation(name = "Split time points", description = "Timelapse data will be opened as a separate stack for each timepoint.")
    @JIPipeParameter("split-time-points")
    public boolean isSplitTimePoints() {
        return splitTimePoints;
    }

    @JIPipeParameter("split-time-points")
    public void setSplitTimePoints(boolean splitTimePoints) {
        this.splitTimePoints = splitTimePoints;
    }

    @SetJIPipeDocumentation(name = "Swap dimensions", description = "Allows reassignment of dimensional axes (e.g., channel, Z and time).  " +
            "Bio-Formats is supposed to be smart about handling multidimensional image data, but in some cases gets things wrong. " +
            "For example, when stitching together a dataset from multiple files using the Group files with similar names option, " +
            "Bio-Formats may not know which dimensional axis the file numbering is supposed to represent. " +
            "It will take a guess, but in case it guesses wrong, you can use Swap dimensions to reassign which dimensions are which.")
    @JIPipeParameter("swap-dimensions")
    public boolean isSwapDimensions() {
        return swapDimensions;
    }

    @JIPipeParameter("swap-dimensions")
    public void setSwapDimensions(boolean swapDimensions) {
        this.swapDimensions = swapDimensions;
    }

    @SetJIPipeDocumentation(name = "Concatenate compatible series", description = "Allows multiple image series to be joined end to end.  " +
            "Example: You want to join two sequential timelapse series.")
    @JIPipeParameter("concatenate")
    public boolean isConcatenate() {
        return concatenate;
    }

    @JIPipeParameter("concatenate")
    public void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;
    }

    @SetJIPipeDocumentation(name = "Crop images", description = "Image planes may be cropped during import to conserve memory. Use the 'Crop regions' parameter " +
            "to define which regions should be cropped.")
    @JIPipeParameter("crop")
    public boolean isCrop() {
        return crop;
    }

    @JIPipeParameter("crop")
    public void setCrop(boolean crop) {
        this.crop = crop;
    }

    @SetJIPipeDocumentation(name = "Stitch tiles", description = "Stitch tiles - Performs very simple stitching of tiles. " +
            " The overlap is assumed to be 0%, and the stage coordinates are used to determine the proper placement of the tiles. " +
            "This is useful for seeing a quick preview of what the stitched image might look like, " +
            "but is not a substitute for proper stitching plugins such as the 2D/3D Stitching plugin.")
    @JIPipeParameter("stitch-tiles")
    public boolean isStitchTiles() {
        return stitchTiles;
    }

    @JIPipeParameter("stitch-tiles")
    public void setStitchTiles(boolean stitchTiles) {
        this.stitchTiles = stitchTiles;
    }

    @SetJIPipeDocumentation(name = "Extract ROIs", description = "If enabled, ROIs are extracted from OME data.")
    @JIPipeParameter("extract-rois")
    public boolean isExtractRois() {
        return extractRois;
    }

    @JIPipeParameter("extract-rois")
    public void setExtractRois(boolean extractRois) {
        this.extractRois = extractRois;
    }
}
