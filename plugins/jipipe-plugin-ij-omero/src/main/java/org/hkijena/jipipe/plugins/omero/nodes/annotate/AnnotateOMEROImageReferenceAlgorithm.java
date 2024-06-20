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

package org.hkijena.jipipe.plugins.omero.nodes.annotate;

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
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROKeyValuePairToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROTagToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate image with OMERO metadata", description = "Annotates an OMERO image ID with OMERO metadata.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For OMERO")
@AddJIPipeInputSlot(value = OMEROImageReferenceData.class, name = "Images", create = true)
@AddJIPipeOutputSlot(value = OMEROImageReferenceData.class, name = "Images", create = true)
public class AnnotateOMEROImageReferenceAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private final OMEROKeyValuePairToAnnotationImporter keyValuePairToAnnotationImporter;
    private final OMEROTagToAnnotationImporter tagToAnnotationImporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private OptionalTextAnnotationNameParameter nameAnnotation = new OptionalTextAnnotationNameParameter("Image title", true);
    private OptionalTextAnnotationNameParameter idAnnotation = new OptionalTextAnnotationNameParameter("#OMERO:Image_ID", true);
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
        this.nameAnnotation = new OptionalTextAnnotationNameParameter(other.nameAnnotation);
        this.idAnnotation = new OptionalTextAnnotationNameParameter(other.idAnnotation);
        this.annotationMergeMode = other.annotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
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
                if (idAnnotation.isEnabled()) {
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
    public OptionalTextAnnotationNameParameter getNameAnnotation() {
        return nameAnnotation;
    }

    @JIPipeParameter("name-annotation")
    public void setNameAnnotation(OptionalTextAnnotationNameParameter nameAnnotation) {
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
    public OptionalTextAnnotationNameParameter getIdAnnotation() {
        return idAnnotation;
    }

    @JIPipeParameter("id-annotation")
    public void setIdAnnotation(OptionalTextAnnotationNameParameter idAnnotation) {
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
