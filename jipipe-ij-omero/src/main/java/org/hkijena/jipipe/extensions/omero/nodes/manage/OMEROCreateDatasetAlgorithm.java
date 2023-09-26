package org.hkijena.jipipe.extensions.omero.nodes.manage;

import omero.ServerError;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.parameters.AnnotationsToOMEROKeyValuePairExporter;
import org.hkijena.jipipe.extensions.omero.parameters.AnnotationsToOMEROTagExporter;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Create OMERO dataset", description = "Creates a new OMERO data set an associates it to an existing project")
@JIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
@JIPipeInputSlot(value = OMEROProjectReferenceData.class, slotName = "Project", description = "The project that will contain the dataset", autoCreate = true)
@JIPipeOutputSlot(value = OMERODatasetReferenceData.class, slotName = "Dataset", description = "The created dataset", autoCreate = true)
public class OMEROCreateDatasetAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private DefaultExpressionParameter nameGenerator = new DefaultExpressionParameter("\"Untitled\"");
    private final AnnotationsToOMEROKeyValuePairExporter keyValuePairExporter;
    private final AnnotationsToOMEROTagExporter tagExporter;

    public OMEROCreateDatasetAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.keyValuePairExporter = new AnnotationsToOMEROKeyValuePairExporter();
        registerSubParameter(keyValuePairExporter);
        this.tagExporter = new AnnotationsToOMEROTagExporter();
        registerSubParameter(tagExporter);
    }

    public OMEROCreateDatasetAlgorithm(OMEROCreateDatasetAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.nameGenerator = new DefaultExpressionParameter(other.nameGenerator);
        this.keyValuePairExporter = new AnnotationsToOMEROKeyValuePairExporter(other.keyValuePairExporter);
        registerSubParameter(keyValuePairExporter);
        this.tagExporter = new AnnotationsToOMEROTagExporter(other.tagExporter);
        registerSubParameter(tagExporter);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        long projectId = dataBatch.getInputData(getFirstInputSlot(), OMEROProjectReferenceData.class, progressInfo).getProjectId();
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        progressInfo.log("Connecting to " + environment);

        // Generate tags and kv-pairs
        Set<String> tags = new HashSet<>();
        Map<String, String> kvPairs = new HashMap<>();
        keyValuePairExporter.createKeyValuePairs(kvPairs, dataBatch.getMergedTextAnnotations().values());
        tagExporter.createTags(tags, dataBatch.getMergedTextAnnotations().values());

        // Generate name
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        String datasetName = nameGenerator.evaluateToString(variables);

        try(OMEROGateway gateway = new OMEROGateway(environment.toLoginCredentials(), progressInfo)) {
            ProjectData projectData = gateway.getProject(projectId, -1);
            SecurityContext securityContext = new SecurityContext(projectData.getGroupId());

            DatasetData datasetData = new DatasetData();
            datasetData.setName(datasetName);

            datasetData = gateway.getDataManagerFacility().createDataset(securityContext, datasetData, projectData);

            // Attach information
            progressInfo.log("Attaching key-value pairs ...");
            gateway.attachKeyValuePairs(kvPairs, datasetData, securityContext);

            progressInfo.log("Attaching tags ...");
            gateway.attachTags(tags, datasetData, securityContext);

            dataBatch.addOutputData(getFirstOutputSlot(), new OMERODatasetReferenceData(datasetData, environment), progressInfo);
        } catch (DSOutOfServiceException | DSAccessException | ServerError e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Dataset name", description = "Expression that determines that name of the data set")
    @JIPipeParameter("name-generator")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getNameGenerator() {
        return nameGenerator;
    }

    @JIPipeParameter("name-generator")
    public void setNameGenerator(DefaultExpressionParameter nameGenerator) {
        this.nameGenerator = nameGenerator;
    }

    @JIPipeDocumentation(name = "Override OMERO credentials", description = "Allows to override the OMERO credentials provided in the JIPipe application settings")
    @JIPipeParameter("override-credentials")
    public OptionalOMEROCredentialsEnvironment getOverrideCredentials() {
        return overrideCredentials;
    }

    @JIPipeParameter("override-credentials")
    public void setOverrideCredentials(OptionalOMEROCredentialsEnvironment overrideCredentials) {
        this.overrideCredentials = overrideCredentials;
    }

    @JIPipeDocumentation(name = "Export annotations as key-value pairs", description = "The following settings allow you to export annotations as key-value pairs")
    @JIPipeParameter("key-value-pair-exporter")
    public AnnotationsToOMEROKeyValuePairExporter getKeyValuePairExporter() {
        return keyValuePairExporter;
    }

    @JIPipeDocumentation(name = "Export list annotation as tag", description = "The following settings allow you to export a single list-like annotation as tag list.")
    @JIPipeParameter("tag-exporter")
    public AnnotationsToOMEROTagExporter getTagExporter() {
        return tagExporter;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(context, this), environment);
    }
}
