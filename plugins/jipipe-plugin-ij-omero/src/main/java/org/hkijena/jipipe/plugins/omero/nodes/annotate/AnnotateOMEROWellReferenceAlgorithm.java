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
import omero.gateway.model.WellData;
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
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROWellReferenceData;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROKeyValuePairToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.parameters.OMEROTagToAnnotationImporter;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate well with OMERO metadata", description = "Annotates an OMERO well ID with OMERO metadata.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For OMERO")
@AddJIPipeInputSlot(value = OMEROWellReferenceData.class, name = "Wells", create = true)
@AddJIPipeOutputSlot(value = OMEROWellReferenceData.class, name = "Wells", create = true)
public class AnnotateOMEROWellReferenceAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private final OMEROKeyValuePairToAnnotationImporter keyValuePairToAnnotationImporter;
    private final OMEROTagToAnnotationImporter tagToAnnotationImporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private OptionalTextAnnotationNameParameter rowAnnotation = new OptionalTextAnnotationNameParameter("Well row", true);
    private OptionalTextAnnotationNameParameter columnAnnotation = new OptionalTextAnnotationNameParameter("Well column", true);
    private OptionalTextAnnotationNameParameter colorAnnotation = new OptionalTextAnnotationNameParameter("Well color", true);
    private OptionalTextAnnotationNameParameter typeAnnotation = new OptionalTextAnnotationNameParameter("Well type", true);
    private OptionalTextAnnotationNameParameter idAnnotation = new OptionalTextAnnotationNameParameter("#OMERO:Well_ID", true);
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public AnnotateOMEROWellReferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter();
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter();
        registerSubParameter(tagToAnnotationImporter);
    }

    public AnnotateOMEROWellReferenceAlgorithm(AnnotateOMEROWellReferenceAlgorithm other) {
        super(other);
        this.keyValuePairToAnnotationImporter = new OMEROKeyValuePairToAnnotationImporter(other.keyValuePairToAnnotationImporter);
        registerSubParameter(keyValuePairToAnnotationImporter);
        this.tagToAnnotationImporter = new OMEROTagToAnnotationImporter(other.tagToAnnotationImporter);
        registerSubParameter(tagToAnnotationImporter);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.rowAnnotation = new OptionalTextAnnotationNameParameter(other.rowAnnotation);
        this.columnAnnotation = new OptionalTextAnnotationNameParameter(other.columnAnnotation);
        this.idAnnotation = new OptionalTextAnnotationNameParameter(other.idAnnotation);
        this.annotationMergeMode = other.annotationMergeMode;
        this.colorAnnotation = new OptionalTextAnnotationNameParameter(other.colorAnnotation);
        this.typeAnnotation = new OptionalTextAnnotationNameParameter(other.typeAnnotation);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO well", row, getFirstInputSlot().getRowCount());
                long wellId = getFirstInputSlot().getData(row, OMEROWellReferenceData.class, rowProgress).getWellId();
                WellData wellData = gateway.getWell(wellId, -1);
                SecurityContext context = new SecurityContext(wellData.getGroupId());

                List<JIPipeTextAnnotation> annotations = new ArrayList<>();

                try {
                    tagToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, wellData);
                    keyValuePairToAnnotationImporter.createAnnotations(annotations, gateway.getMetadataFacility(), context, wellData);
                } catch (DSOutOfServiceException | DSAccessException e) {
                    throw new RuntimeException(e);
                }

                if (rowAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(rowAnnotation.getContent(), String.valueOf(wellData.getRow())));
                }
                if (columnAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(columnAnnotation.getContent(), String.valueOf(wellData.getColumn())));
                }
                if (typeAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(typeAnnotation.getContent(), StringUtils.nullToEmpty(wellData.getWellType())));
                }
                if (colorAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(colorAnnotation.getContent(), JsonUtils.toJsonString(Arrays.asList(wellData.getRed(),wellData.getGreen(), wellData.getBlue() ,wellData.getAlpha()))));
                }
                if (idAnnotation.isEnabled()) {
                    annotations.add(new JIPipeTextAnnotation(idAnnotation.getContent(), String.valueOf(wellData.getId())));
                }

                iterationStep.addOutputData(getFirstOutputSlot(), new OMEROWellReferenceData(wellData, environment), annotations, annotationMergeMode, rowProgress);
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

    @SetJIPipeDocumentation(name = "Annotate with well color", description = "If enabled, annotate with the well color")
    @JIPipeParameter("color-annotation")
    public OptionalTextAnnotationNameParameter getColorAnnotation() {
        return colorAnnotation;
    }

    @JIPipeParameter("color-annotation")
    public void setColorAnnotation(OptionalTextAnnotationNameParameter colorAnnotation) {
        this.colorAnnotation = colorAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with well type", description = "If enabled, annotate with the well type")
    @JIPipeParameter("type-annotation")
    public OptionalTextAnnotationNameParameter getTypeAnnotation() {
        return typeAnnotation;
    }

    @JIPipeParameter("type-annotation")
    public void setTypeAnnotation(OptionalTextAnnotationNameParameter typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with well row", description = "If enabled, annotate with the well row")
    @JIPipeParameter("row-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalTextAnnotationNameParameter getRowAnnotation() {
        return rowAnnotation;
    }

    @JIPipeParameter("row-annotation")
    public void setRowAnnotation(OptionalTextAnnotationNameParameter rowAnnotation) {
        this.rowAnnotation = rowAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with well column", description = "If enabled, annotate with the well column")
    @JIPipeParameter("column-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalTextAnnotationNameParameter getColumnAnnotation() {
        return columnAnnotation;
    }

    @JIPipeParameter("column-annotation")
    public void setColumnAnnotation(OptionalTextAnnotationNameParameter columnAnnotation) {
        this.columnAnnotation = columnAnnotation;
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

    @SetJIPipeDocumentation(name = "Annotate with OMERO well ID", description = "If enabled, adds the OMERO well ID as annotation")
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
