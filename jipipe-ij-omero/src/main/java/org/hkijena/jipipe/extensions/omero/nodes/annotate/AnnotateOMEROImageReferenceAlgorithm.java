package org.hkijena.jipipe.extensions.omero.nodes.annotate;

import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.parameters.OMEROKeyValuePairToAnnotationImporter;
import org.hkijena.jipipe.extensions.omero.parameters.OMEROTagToAnnotationImporter;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate image with OMERO metadata", description = "Annotates an OMERO image ID with OMERO metadata.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For OMERO")
@AddJIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Images", create = true)
@AddJIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Images", create = true)
public class AnnotateOMEROImageReferenceAlgorithm extends JIPipeSingleIterationAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private OptionalAnnotationNameParameter nameAnnotation = new OptionalAnnotationNameParameter("Image title", true);
    private OptionalAnnotationNameParameter idAnnotation = new OptionalAnnotationNameParameter("#OMERO:Image_ID", true);
    private final OMEROKeyValuePairToAnnotationImporter keyValuePairToAnnotationImporter;
    private final OMEROTagToAnnotationImporter tagToAnnotationImporter;
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public AnnotateOMEROImageReferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter();
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter();
        registerSubParameter(tagToAnnotationImporter);
    }

    public AnnotateOMEROImageReferenceAlgorithm(AnnotateOMEROImageReferenceAlgorithm other) {
        super(other);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter(other.keyValuePairToAnnotationImporter);
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter(other.tagToAnnotationImporter);
        registerSubParameter(tagToAnnotationImporter);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.nameAnnotation = new OptionalAnnotationNameParameter(other.nameAnnotation);
        this.idAnnotation = new OptionalAnnotationNameParameter(other.idAnnotation);
        this.annotationMergeMode = other.annotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO image", row, getFirstInputSlot().getRowCount());
                long imageId = getFirstInputSlot().getData(row, OMEROImageReferenceData.class, rowProgress).getImageId();
                ImageData imageData = gateway.getImage(imageId, -1);
                SecurityContext context = new SecurityContext(imageData.getGroupId());

                List<JIPipeTextAnnotation> annotations = new ArrayList<>();

                try {
                    tagToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, imageData);
                    keyValuePairToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, imageData);
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }

                if (nameAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(nameAnnotation.getContent(), imageData.getName()));
                }
                if(idAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(idAnnotation.getContent(), String.valueOf(imageData.getId())));
                }

                iterationStep.addOutputData(getFirstOutputSlot(), new OMEROImageReferenceData(imageData, environment), annotations, annotationMergeMode, rowProgress);
            }
        }
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


    @SetJIPipeDocumentation(name = "Annotate with image name", description = "Optional annotation type where the image title is written.")
    @JIPipeParameter("name-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalAnnotationNameParameter getNameAnnotation() {
        return nameAnnotation;
    }

    @JIPipeParameter("name-annotation")
    public void setNameAnnotation(OptionalAnnotationNameParameter nameAnnotation) {
        this.nameAnnotation = nameAnnotation;
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

    @SetJIPipeDocumentation(name = "Annotate with OMERO image ID", description = "If enabled, adds the OMERO image ID as annotation")
    @JIPipeParameter("id-annotation")
    public OptionalAnnotationNameParameter getIdAnnotation() {
        return idAnnotation;
    }

    @JIPipeParameter("id-annotation")
    public void setIdAnnotation(OptionalAnnotationNameParameter idAnnotation) {
        this.idAnnotation = idAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotation merge mode", description = "Determines how newly generated annotations are merged with existing ones")
    @JIPipeParameter("annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeMode() {
        return annotationMergeMode;
    }

    @JIPipeParameter("annotation-merge-mode")
    public void setAnnotationMergeMode(JIPipeTextAnnotationMergeMode annotationMergeMode) {
        this.annotationMergeMode = annotationMergeMode;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(reportContext, this), environment);
    }
}
