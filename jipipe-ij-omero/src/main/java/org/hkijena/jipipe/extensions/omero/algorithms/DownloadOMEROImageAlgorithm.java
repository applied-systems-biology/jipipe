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

package org.hkijena.jipipe.extensions.omero.algorithms;

import ij.ImagePlus;
import loci.common.Region;
import loci.formats.FormatException;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import ome.xml.meta.OMEXMLMetadata;
import ome.xml.model.enums.DimensionOrder;
import omero.gateway.LoginCredentials;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEColorMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ROIHandler;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.library.roi.RectangleList;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@JIPipeDocumentation(name = "Download image from OMERO", description = "Imports an image from OMERO into ImageJ")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = OMEImageData.class, slotName = "Output", autoCreate = true)
public class DownloadOMEROImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Map<Thread, OMEROGateway> currentGateways = new HashMap<>();
    private OMEROCredentials credentials = new OMEROCredentials();
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
    private OptionalAnnotationNameParameter titleAnnotation = new OptionalAnnotationNameParameter();
    private RectangleList cropRegions = new RectangleList();
    private boolean addKeyValuePairsAsAnnotations = true;
    private OptionalStringParameter tagAnnotation = new OptionalStringParameter("Tags", true);
    private JIPipeProgressInfo parameterProgressInfo;
    private AtomicLong lastGroupId = new AtomicLong(-1);


    public DownloadOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
        titleAnnotation.setContent("Image title");
    }

    public DownloadOMEROImageAlgorithm(DownloadOMEROImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
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
        this.titleAnnotation = new OptionalAnnotationNameParameter(other.titleAnnotation);
        this.extractRois = other.extractRois;
        this.addKeyValuePairsAsAnnotations = other.addKeyValuePairsAsAnnotations;
        this.tagAnnotation = new OptionalStringParameter(other.tagAnnotation);
        registerSubParameter(credentials);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        this.parameterProgressInfo = progressInfo;
        this.lastGroupId.set(-1);
        // Run downloader
        super.runParameterSet(progressInfo, parameterAnnotations);
        for (OMEROGateway gateway : currentGateways.values()) {
            try {
                gateway.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEROImageReferenceData imageReferenceData = dataBatch.getInputData(getFirstInputSlot(), OMEROImageReferenceData.class, progressInfo);
        LoginCredentials lc = credentials.getCredentials();

        // Get gateway and fine the appropriate group for the image
        OMEROGateway gateway;
        synchronized (this) {
            gateway = currentGateways.getOrDefault(Thread.currentThread(), null);
            if (gateway == null) {
                parameterProgressInfo.log("Creating OMERO gateway for thread " + Thread.currentThread());
                gateway = new OMEROGateway(credentials.getCredentials(), parameterProgressInfo);
                currentGateways.put(Thread.currentThread(), gateway);
            }
        }

        ImageData imageData = gateway.getImage(imageReferenceData.getImageId(), lastGroupId.get());
        if (imageData == null) {
            progressInfo.log("Unable to obtain image info. Retrying by testing available groups.");
            imageData = gateway.getImage(imageReferenceData.getImageId(), -1);
        }
        if (imageData == null) {
            throw new RuntimeException("Unable to find image with ID=" + imageReferenceData.getImageId());
        }
        lastGroupId.set(imageData.getGroupId());

        // Setup for Bioformats
        ImporterOptions options;
        try {
            options = new ImporterOptions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Workaround bug where OMERO is not added to the list of available locations
        if (!options.getStringOption(ImporterOptions.KEY_LOCATION).getPossible().contains(ImporterOptions.LOCATION_OMERO)) {
            options.getStringOption(ImporterOptions.KEY_LOCATION).addPossible(ImporterOptions.LOCATION_OMERO);
        }

        options.setLocation(ImporterOptions.LOCATION_OMERO);
        String omeroId = "omero:server=" +
                lc.getServer().getHost() +
                "\nuser=" +
                lc.getUser().getUsername() +
                "\nport=" +
                lc.getServer().getPort() +
                "\npass=" +
                lc.getUser().getPassword() +
                "\ngroupID=" +
                imageData.getGroupId() +
                "\niid=" +
                imageReferenceData.getImageId();
        options.setId(omeroId);
        options.setWindowless(true);
        options.setQuiet(true);
        options.setShowMetadata(false);
        options.setShowOMEXML(false);
        options.setShowROIs(false);
        options.setVirtual(false);
        options.setColorMode(colorMode.name());
        options.setStackOrder(stackOrder.name());
        options.setSplitChannels(splitChannels);
        options.setSplitFocalPlanes(splitFocalPlanes);
        options.setSplitTimepoints(splitTimePoints);
        options.setSwapDimensions(swapDimensions);
        options.setConcatenate(concatenate);
        options.setCrop(crop);
        options.setAutoscale(autoScale);
        options.setStitchTiles(stitchTiles);
        for (int i = 0; i < cropRegions.size(); i++) {
            Rectangle rectangle = cropRegions.get(i);
            options.setCropRegion(i, new Region(rectangle.x, rectangle.y, rectangle.width, rectangle.height));
        }

        try {
            ImportProcess process = new ImportProcess(options);
            progressInfo.log("Downloading image ID=" + imageReferenceData.getImageId() + " from " + lc.getUser().getUsername() + "@" + lc.getServer().getHost() + ":" + lc.getServer().getPort() + " (Group " + imageData.getGroupId() + ")");
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

            for (ImagePlus image : images) {
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();

                if (addKeyValuePairsAsAnnotations || tagAnnotation.isEnabled()) {
                    try {
//                        ImageData imageData = gateway.getBrowseFacility().getImage(gateway.getContext(), imageReferenceData.getImageId());
//                        if (addKeyValuePairsAsAnnotations) {
//                            for (Map.Entry<String, String> entry : OMEROUtils.getKeyValuePairAnnotations(gateway.getMetadata(), gateway.getContext(), imageData).entrySet()) {
//                                annotations.add(new JIPipeTextAnnotation(entry.getKey(), entry.getValue()));
//                            }
//                        }
//                        if (tagAnnotation.isEnabled()) {
//                            List<String> sortedTags = OMEROUtils.getTagAnnotations(gateway.getMetadata(), gateway.getContext(), imageData).stream().sorted().collect(Collectors.toList());
//                            annotations.add(new JIPipeTextAnnotation(tagAnnotation.getContent(), JsonUtils.toJsonString(sortedTags)));
//                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                if (titleAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(titleAnnotation.getContent(), image.getTitle()));
                }

                ROIListData rois = new ROIListData();
                if (extractRois) {
                    rois = ROIHandler.openROIs(process.getOMEMetadata(), new ImagePlus[]{image});
                }

                dataBatch.addOutputData(getFirstOutputSlot(), new OMEImageData(image, rois, omexmlMetadata), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        } catch (FormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }

    @JIPipeDocumentation(name = "Auto scale", description = "Stretches the channel histograms to each channel's global minimum and maximum value throughout the stack. " +
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

    @JIPipeDocumentation(name = "Color mode", description = "Color mode - Visualizes channels according to the specified scheme.  Possible choices are:\n" +
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

    @JIPipeDocumentation(name = "Crop regions")
    @JIPipeParameter("crop-regions")
    public RectangleList getCropRegions() {
        return cropRegions;
    }

    @JIPipeParameter("crop-regions")
    public void setCropRegions(RectangleList cropRegions) {
        this.cropRegions = cropRegions;
    }

    @JIPipeDocumentation(name = "Stack order")
    @JIPipeParameter("stack-order")
    public DimensionOrder getStackOrder() {
        return stackOrder;
    }

    @JIPipeParameter("stack-order")
    public void setStackOrder(DimensionOrder stackOrder) {
        this.stackOrder = stackOrder;

    }

    @JIPipeDocumentation(name = "Split channels", description = "Each channel is opened as a separate stack.  " +
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

    @JIPipeDocumentation(name = "Split focal planes", description = "Each focal plane is opened as a separate stack.")
    @JIPipeParameter("split-focal-planes")
    public boolean isSplitFocalPlanes() {
        return splitFocalPlanes;
    }

    @JIPipeParameter("split-focal-planes")
    public void setSplitFocalPlanes(boolean splitFocalPlanes) {
        this.splitFocalPlanes = splitFocalPlanes;

    }

    @JIPipeDocumentation(name = "Split time points", description = "Timelapse data will be opened as a separate stack for each timepoint.")
    @JIPipeParameter("split-time-points")
    public boolean isSplitTimePoints() {
        return splitTimePoints;
    }

    @JIPipeParameter("split-time-points")
    public void setSplitTimePoints(boolean splitTimePoints) {
        this.splitTimePoints = splitTimePoints;

    }

    @JIPipeDocumentation(name = "Swap dimensions", description = "Allows reassignment of dimensional axes (e.g., channel, Z and time).  " +
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

    @JIPipeDocumentation(name = "Concatenate compatible series", description = "Allows multiple image series to be joined end to end.  " +
            "Example: You want to join two sequential timelapse series.")
    @JIPipeParameter("concatenate")
    public boolean isConcatenate() {
        return concatenate;
    }

    @JIPipeParameter("concatenate")
    public void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;

    }

    @JIPipeDocumentation(name = "Crop images", description = "Image planes may be cropped during import to conserve memory. Use the 'Crop regions' parameter " +
            "to define which regions should be cropped.")
    @JIPipeParameter("crop")
    public boolean isCrop() {
        return crop;
    }

    @JIPipeParameter("crop")
    public void setCrop(boolean crop) {
        this.crop = crop;

    }

    @JIPipeDocumentation(name = "Stitch tiles", description = "Stitch tiles - Performs very simple stitching of tiles. " +
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

    @JIPipeDocumentation(name = "Title annotation", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalAnnotationNameParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @JIPipeDocumentation(name = "Extract ROIs", description = "If enabled, ROIs are extracted from OME data.")
    @JIPipeParameter("extract-rois")
    public boolean isExtractRois() {
        return extractRois;
    }

    @JIPipeParameter("extract-rois")
    public void setExtractRois(boolean extractRois) {
        this.extractRois = extractRois;
    }

    @JIPipeDocumentation(name = "Annotate with tags", description = "Creates an annotation with given key and writes the tags into them in JSON format.")
    @JIPipeParameter("tag-annotation")
    public OptionalStringParameter getTagAnnotation() {
        return tagAnnotation;
    }

    @JIPipeParameter("tag-annotation")
    public void setTagAnnotation(OptionalStringParameter tagAnnotation) {
        this.tagAnnotation = tagAnnotation;
    }

    @JIPipeDocumentation(name = "Add Key-Value pairs as annotations", description = "Adds OMERO project annotations as JIPipe annotations")
    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public boolean isAddKeyValuePairsAsAnnotations() {
        return addKeyValuePairsAsAnnotations;
    }

    @JIPipeParameter("add-key-value-pairs-as-annotations")
    public void setAddKeyValuePairsAsAnnotations(boolean addKeyValuePairsAsAnnotations) {
        this.addKeyValuePairsAsAnnotations = addKeyValuePairsAsAnnotations;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }
}
