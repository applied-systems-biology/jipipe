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

package org.hkijena.jipipe.plugins.omero.nodes.download;

import ij.ImagePlus;
import loci.common.Region;
import loci.formats.FormatException;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import ome.xml.meta.OMEXMLMetadata;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIHandler;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.plugins.omero.parameters.ImageImportParameters;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROKeyValuePairToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROTagToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Download image from OMERO", description = "Imports an image from OMERO into ImageJ")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = OMEROImageReferenceData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = OMEImageData.class, slotName = "Output", create = true)
public class DownloadOMEROImageAlgorithm extends JIPipeSimpleIteratingAlgorithm implements OMEROCredentialAccessNode {
    private final ImageImportParameters imageImportParameters;
    private final OMEROKeyValuePairToAnnotationImporter keyValuePairToAnnotationImporter;
    private final OMEROTagToAnnotationImporter tagToAnnotationImporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private OptionalTextAnnotationNameParameter titleAnnotation = new OptionalTextAnnotationNameParameter("Image title", true);
    private OptionalTextAnnotationNameParameter idAnnotation = new OptionalTextAnnotationNameParameter("#OMERO:Image_ID", true);


    public DownloadOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.imageImportParameters = new ImageImportParameters();
        registerSubParameter(imageImportParameters);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter();
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter();
        registerSubParameter(tagToAnnotationImporter);
    }

    public DownloadOMEROImageAlgorithm(DownloadOMEROImageAlgorithm other) {
        super(other);
        this.imageImportParameters = new ImageImportParameters(other.imageImportParameters);
        registerSubParameter(imageImportParameters);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter(other.keyValuePairToAnnotationImporter);
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter(other.tagToAnnotationImporter);
        registerSubParameter(tagToAnnotationImporter);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.titleAnnotation = new OptionalTextAnnotationNameParameter(other.titleAnnotation);
        this.idAnnotation = new OptionalTextAnnotationNameParameter(other.idAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROImageReferenceData imageReferenceData = iterationStep.getInputData(getFirstInputSlot(), OMEROImageReferenceData.class, progressInfo);
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials lc = environment.toLoginCredentials();

        // Get gateway and fine the appropriate group for the image
        try (OMEROGateway gateway = new OMEROGateway(environment.toLoginCredentials(), progressInfo)) {
            ImageData imageData = gateway.getImage(imageReferenceData.getImageId(), -1);
            if (imageData == null) {
                throw new RuntimeException("Unable to find image with ID=" + imageReferenceData.getImageId());
            }

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
            options.setColorMode(imageImportParameters.getColorMode().name());
            options.setStackOrder(imageImportParameters.getStackOrder().name());
            options.setSplitChannels(imageImportParameters.isSplitChannels());
            options.setSplitFocalPlanes(imageImportParameters.isSplitFocalPlanes());
            options.setSplitTimepoints(imageImportParameters.isSplitTimePoints());
            options.setSwapDimensions(imageImportParameters.isSwapDimensions());
            options.setConcatenate(imageImportParameters.isConcatenate());
            options.setCrop(imageImportParameters.isCrop());
            options.setAutoscale(imageImportParameters.isAutoScale());
            options.setStitchTiles(imageImportParameters.isStitchTiles());
            for (int i = 0; i < imageImportParameters.getCropRegions().size(); i++) {
                Rectangle rectangle = imageImportParameters.getCropRegions().get(i);
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
                    SecurityContext context = new SecurityContext(imageData.getGroupId());

                    try {
                        tagToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, imageData);
                        keyValuePairToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, imageData);
                    } catch (DSOutOfServiceException | DSAccessException e) {
                        throw new RuntimeException(e);
                    }

                    if (titleAnnotation.isEnabled()) {
                        annotations.add(new JIPipeTextAnnotation(titleAnnotation.getContent(), image.getTitle()));
                    }
                    if (idAnnotation.isEnabled()) {
                        annotations.add(new JIPipeTextAnnotation(idAnnotation.getContent(), String.valueOf(imageReferenceData.getImageId())));
                    }

                    ROIListData rois = new ROIListData();
                    if (imageImportParameters.isExtractRois()) {
                        rois = ROIHandler.openROIs(process.getOMEMetadata(), new ImagePlus[]{image});
                    }

                    iterationStep.addOutputData(getFirstOutputSlot(), new OMEImageData(image, rois, omexmlMetadata), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }
            } catch (FormatException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Annotate with OMERO image ID", description = "If enabled, adds the OMERO image ID as annotation")
    @JIPipeParameter("id-annotation")
    public OptionalTextAnnotationNameParameter getIdAnnotation() {
        return idAnnotation;
    }

    @JIPipeParameter("id-annotation")
    public void setIdAnnotation(OptionalTextAnnotationNameParameter idAnnotation) {
        this.idAnnotation = idAnnotation;
    }

    @SetJIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
    }


    @SetJIPipeDocumentation(name = "Annotate with image title", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalTextAnnotationNameParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalTextAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Bio-Formats options", description = "Settings for Bio-Formats, which is utilized for importing images from OMERO")
    @JIPipeParameter(value = "image-import-parameters", collapsed = true)
    public ImageImportParameters getImageImportParameters() {
        return imageImportParameters;
    }

    @SetJIPipeDocumentation(name = "Import key-value pairs", description = "OMERO key-value pairs can be imported into annotations")
    @JIPipeParameter("key-value-pair-to-annotation-importer")
    public OMEROKeyValuePairToAnnotationImporter getKeyValuePairToAnnotationImporter() {
        return keyValuePairToAnnotationImporter;
    }

    @SetJIPipeDocumentation(name = "Import tags", description = "OMERO tags can be imported into annotations")
    @JIPipeParameter("tag-to-annotation-importer")
    public OMEROTagToAnnotationImporter getTagToAnnotationImporter() {
        return tagToAnnotationImporter;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!isPassThrough()) {
            reportConfiguredOMEROEnvironmentValidity(reportContext, report);
        }
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredOMEROCredentialsEnvironment());
    }
}
