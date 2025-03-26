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
import omero.gateway.model.ScreenData;
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
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROScreenReferenceData;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROKeyValuePairToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROTagToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate screen with OMERO metadata", description = "Annotates an OMERO screen ID with OMERO metadata.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For OMERO")
@AddJIPipeInputSlot(value = OMEROScreenReferenceData.class, name = "Screens", create = true)
@AddJIPipeOutputSlot(value = OMEROScreenReferenceData.class, name = "Screens", create = true)
public class AnnotateOMEROScreenReferenceAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private final OMEROKeyValuePairToAnnotationImporter keyValuePairToAnnotationImporter;
    private final OMEROTagToAnnotationImporter tagToAnnotationImporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private OptionalTextAnnotationNameParameter nameAnnotation = new OptionalTextAnnotationNameParameter("Screen title", true);
    private OptionalTextAnnotationNameParameter descriptionAnnotation = new OptionalTextAnnotationNameParameter("Screen description", true);
    private OptionalTextAnnotationNameParameter protocolIdAnnotation = new OptionalTextAnnotationNameParameter("Protocol ID", true);
    private OptionalTextAnnotationNameParameter protocolDescriptionAnnotation = new OptionalTextAnnotationNameParameter("Protocol description", false);
    private OptionalTextAnnotationNameParameter reagentSetIdAnnotation = new OptionalTextAnnotationNameParameter("Reagent ID", true);
    private OptionalTextAnnotationNameParameter reagentSetDescriptionAnnotation = new OptionalTextAnnotationNameParameter("Reagent description", false);
    private OptionalTextAnnotationNameParameter idAnnotation = new OptionalTextAnnotationNameParameter("#OMERO:Screen_ID", true);
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public AnnotateOMEROScreenReferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter();
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter();
        registerSubParameter(tagToAnnotationImporter);
    }

    public AnnotateOMEROScreenReferenceAlgorithm(AnnotateOMEROScreenReferenceAlgorithm other) {
        super(other);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter(other.keyValuePairToAnnotationImporter);
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter(other.tagToAnnotationImporter);
        registerSubParameter(tagToAnnotationImporter);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.nameAnnotation = new OptionalTextAnnotationNameParameter(other.nameAnnotation);
        this.descriptionAnnotation = new OptionalTextAnnotationNameParameter(other.descriptionAnnotation);
        this.idAnnotation = new OptionalTextAnnotationNameParameter(other.idAnnotation);
        this.annotationMergeMode = other.annotationMergeMode;
        this.protocolIdAnnotation = new OptionalTextAnnotationNameParameter(other.protocolIdAnnotation);
        this.protocolDescriptionAnnotation = new OptionalTextAnnotationNameParameter(other.protocolDescriptionAnnotation);
        this.reagentSetIdAnnotation = new OptionalTextAnnotationNameParameter(other.reagentSetIdAnnotation);
        this.reagentSetDescriptionAnnotation = new OptionalTextAnnotationNameParameter(other.reagentSetDescriptionAnnotation);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO screen", row, getFirstInputSlot().getRowCount());
                long screenId = getFirstInputSlot().getData(row, OMEROScreenReferenceData.class, rowProgress).getScreenId();
                ScreenData screenData = gateway.getScreen(screenId, -1);
                SecurityContext context = new SecurityContext(screenData.getGroupId());

                List<JIPipeTextAnnotation> annotations = new ArrayList<>();

                try {
                    tagToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, screenData);
                    keyValuePairToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, screenData);
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }

                if (nameAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(nameAnnotation.getContent(), screenData.getName()));
                }
                if (descriptionAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(descriptionAnnotation.getContent(), screenData.getDescription()));
                }
                if (protocolIdAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(protocolIdAnnotation.getContent(), StringUtils.nullToEmpty(screenData.getProtocolIdentifier())));
                }
                if (protocolDescriptionAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(protocolDescriptionAnnotation.getContent(), StringUtils.nullToEmpty(screenData.getProtocolDescription())));
                }
                if (reagentSetIdAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(reagentSetIdAnnotation.getContent(), StringUtils.nullToEmpty(screenData.getReagentSetIdentifier())));
                }
                if (reagentSetDescriptionAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(reagentSetDescriptionAnnotation.getContent(), StringUtils.nullToEmpty(screenData.getReagentSetDescripion())));
                }
                if (idAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(idAnnotation.getContent(), String.valueOf(screenData.getId())));
                }

                iterationStep.addOutputData(getFirstOutputSlot(), new OMEROScreenReferenceData(screenData, environment), annotations, annotationMergeMode, rowProgress);
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

    @SetJIPipeDocumentation(name = "Annotate with protocol description", description = "If enabled, annotate with the protocol description")
    @JIPipeParameter("protocol-description-annotation")
    public OptionalTextAnnotationNameParameter getProtocolDescriptionAnnotation() {
        return protocolDescriptionAnnotation;
    }

    @JIPipeParameter("protocol-description-annotation")
    public void setProtocolDescriptionAnnotation(OptionalTextAnnotationNameParameter protocolDescriptionAnnotation) {
        this.protocolDescriptionAnnotation = protocolDescriptionAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with protocol ID", description = "If enabled, annotate with the protocol ID")
    @JIPipeParameter("protocol-id-annotation")
    public OptionalTextAnnotationNameParameter getProtocolIdAnnotation() {
        return protocolIdAnnotation;
    }

    @JIPipeParameter("protocol-id-annotation")
    public void setProtocolIdAnnotation(OptionalTextAnnotationNameParameter protocolIdAnnotation) {
        this.protocolIdAnnotation = protocolIdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with reagent set description", description = "If enabled, annotate with the reagent set description")
    @JIPipeParameter("reagent-set-description-annotation")
    public OptionalTextAnnotationNameParameter getReagentSetDescriptionAnnotation() {
        return reagentSetDescriptionAnnotation;
    }

    @JIPipeParameter("reagent-set-description-annotation")
    public void setReagentSetDescriptionAnnotation(OptionalTextAnnotationNameParameter reagentSetDescriptionAnnotation) {
        this.reagentSetDescriptionAnnotation = reagentSetDescriptionAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with reagent set ID", description = "If enabled, annotate with the reagent set ID")
    @JIPipeParameter("reagent-set-id-annotation")
    public OptionalTextAnnotationNameParameter getReagentSetIdAnnotation() {
        return reagentSetIdAnnotation;
    }

    @JIPipeParameter("reagent-set-id-annotation")
    public void setReagentSetIdAnnotation(OptionalTextAnnotationNameParameter reagentSetIdAnnotation) {
        this.reagentSetIdAnnotation = reagentSetIdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with screen name", description = "Optional annotation type where the screen title is written.")
    @JIPipeParameter("name-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalTextAnnotationNameParameter getNameAnnotation() {
        return nameAnnotation;
    }

    @JIPipeParameter("name-annotation")
    public void setNameAnnotation(OptionalTextAnnotationNameParameter nameAnnotation) {
        this.nameAnnotation = nameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with screen description", description = "Optional annotation type where the screen description is written.")
    @JIPipeParameter("description-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalTextAnnotationNameParameter getDescriptionAnnotation() {
        return descriptionAnnotation;
    }

    @JIPipeParameter("description-annotation")
    public void setDescriptionAnnotation(OptionalTextAnnotationNameParameter descriptionAnnotation) {
        this.descriptionAnnotation = descriptionAnnotation;
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

    @SetJIPipeDocumentation(name = "Annotate with OMERO screen ID", description = "If enabled, adds the OMERO screen ID as annotation")
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
