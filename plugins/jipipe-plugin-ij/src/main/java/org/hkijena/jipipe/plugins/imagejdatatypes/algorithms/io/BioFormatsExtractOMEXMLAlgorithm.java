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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.config.ConfigWindow;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.WindowTools;
import ome.xml.meta.OMEXMLMetadata;
import ome.xml.model.enums.DimensionOrder;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEXMLData;
import org.hkijena.jipipe.plugins.imagejdatatypes.parameters.OMEColorMode;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDataAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.strings.XMLData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * BioFormats importer wrapper
 */
@SetJIPipeDocumentation(name = "Read OME XML from file", description = "Obtains the OME XML from Bio-Formats without importing the file itself")
@AddJIPipeInputSlot(value = FileData.class, name = "Input", description = "The image file(s)", create = true)
@AddJIPipeOutputSlot(value = OMEXMLData.class, name = "Output", description = "The OME XML", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeCitation("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
        "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
        "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nBio-Formats", aliasName = "Bio-Formats Importer")
public class BioFormatsExtractOMEXMLAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final BioFormatsSettings bioFormatsSettings;

    public BioFormatsExtractOMEXMLAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.bioFormatsSettings = new BioFormatsSettings();
        registerSubParameter(bioFormatsSettings);
    }

    public BioFormatsExtractOMEXMLAlgorithm(BioFormatsExtractOMEXMLAlgorithm other) {
        super(other);
        this.bioFormatsSettings = new BioFormatsSettings(other.bioFormatsSettings);
        registerSubParameter(bioFormatsSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData inputFile = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        ImporterOptions options;
        try {
            options = new ImporterOptions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        options.setId(inputFile.getPath());
        options.setWindowless(true);
        options.setQuiet(true);
        options.setShowMetadata(false);
        options.setShowOMEXML(false);
        options.setShowROIs(false);
        options.setVirtual(true);
        options.setColorMode(bioFormatsSettings.colorMode.name());
        options.setStackOrder(bioFormatsSettings.stackOrder.name());
        options.setSplitChannels(bioFormatsSettings.splitChannels);
        options.setSplitFocalPlanes(bioFormatsSettings.splitFocalPlanes);
        options.setSplitTimepoints(bioFormatsSettings.splitTimePoints);
        options.setSwapDimensions(bioFormatsSettings.swapDimensions);
        options.setConcatenate(bioFormatsSettings.concatenate);
        options.setCrop(bioFormatsSettings.crop);
        options.setAutoscale(bioFormatsSettings.autoScale);
        options.setStitchTiles(bioFormatsSettings.stitchTiles);
        options.setOpenAllSeries(true);

        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            ImportProcess process = new ImportProcess(options);
            if (!process.execute()) {
                throw new NullPointerException();
            }
            ImagePlusReader reader = new ImagePlusReader(process);
            ImagePlus[] images = reader.openImagePlus();
            if (!options.isVirtual()) {
                process.getReader().close();
            }

            OMEXMLMetadata omexmlMetadata = null;
            if (process.getOMEMetadata() instanceof OMEXMLMetadata) {
                omexmlMetadata = (OMEXMLMetadata) process.getOMEMetadata();
            }

            try {
                iterationStep.addOutputData(getFirstOutputSlot(), new OMEXMLData(omexmlMetadata.dumpXML()), progressInfo);
            }
            finally {
                // Close everything
                for (ImagePlus image : images) {
                    image.close();
                }
                process.getReader().close();
            }


        } catch (FormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Configure Bio-Formats", description = "Opens the Bio-Formats configuration window that allows to set format-specific settings. Please note that these settings are global and not managed by JIPipe.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/bioformats.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/bioformats.png")
    public void setToExample(JIPipeWorkbench parent) {
        ConfigWindow cw = new ConfigWindow();
        WindowTools.placeWindow(cw);
        cw.setVisible(true);
    }

    @SetJIPipeDocumentation(name = "Bio-Formats settings", description = "Settings related to the Bio-Formats importer")
    @JIPipeParameter(value = "bio-formats-settings", collapsed = true)
    public BioFormatsSettings getBioFormatsSettings() {
        return bioFormatsSettings;
    }

    public static class BioFormatsSettings extends AbstractJIPipeParameterCollection {
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

        public BioFormatsSettings() {

        }

        public BioFormatsSettings(BioFormatsSettings other) {
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
    }
}
