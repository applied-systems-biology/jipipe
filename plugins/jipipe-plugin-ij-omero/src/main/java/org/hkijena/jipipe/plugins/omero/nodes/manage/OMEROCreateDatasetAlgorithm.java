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

package org.hkijena.jipipe.plugins.omero.nodes.manage;

import omero.ServerError;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.plugins.omero.parameters.AnnotationsToOMEROKeyValuePairExporter;
import org.hkijena.jipipe.plugins.omero.parameters.AnnotationsToOMEROTagExporter;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;

import java.util.*;

@SetJIPipeDocumentation(name = "Create OMERO dataset", description = "Creates a new OMERO data set an associates it to an existing project")
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
@AddJIPipeInputSlot(value = OMEROProjectReferenceData.class, name = "Project", description = "The project that will contain the dataset", create = true)
@AddJIPipeOutputSlot(value = OMERODatasetReferenceData.class, name = "Dataset", description = "The created dataset", create = true)
public class OMEROCreateDatasetAlgorithm extends JIPipeSimpleIteratingAlgorithm implements OMEROCredentialAccessNode {
    private final AnnotationsToOMEROKeyValuePairExporter keyValuePairExporter;
    private final AnnotationsToOMEROTagExporter tagExporter;
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter nameGenerator = new JIPipeExpressionParameter("\"Untitled\"");

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
        this.nameGenerator = new JIPipeExpressionParameter(other.nameGenerator);
        this.keyValuePairExporter = new AnnotationsToOMEROKeyValuePairExporter(other.keyValuePairExporter);
        registerSubParameter(keyValuePairExporter);
        this.tagExporter = new AnnotationsToOMEROTagExporter(other.tagExporter);
        registerSubParameter(tagExporter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        long projectId = iterationStep.getInputData(getFirstInputSlot(), OMEROProjectReferenceData.class, progressInfo).getProjectId();
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        progressInfo.log("Connecting to " + environment);

        // Generate tags and kv-pairs
        Set<String> tags = new HashSet<>();
        Map<String, String> kvPairs = new HashMap<>();
        keyValuePairExporter.createKeyValuePairs(kvPairs, iterationStep.getMergedTextAnnotations().values());
        tagExporter.createTags(tags, iterationStep.getMergedTextAnnotations().values());

        // Generate name
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        String datasetName = nameGenerator.evaluateToString(variables);

        try (OMEROGateway gateway = new OMEROGateway(environment.toLoginCredentials(), progressInfo)) {
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

            iterationStep.addOutputData(getFirstOutputSlot(), new OMERODatasetReferenceData(datasetData, environment), progressInfo);
        } catch (DSOutOfServiceException | DSAccessException | ServerError e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Dataset name", description = "Expression that determines that name of the data set")
    @JIPipeParameter("name-generator")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getNameGenerator() {
        return nameGenerator;
    }

    @JIPipeParameter("name-generator")
    public void setNameGenerator(JIPipeExpressionParameter nameGenerator) {
        this.nameGenerator = nameGenerator;
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

    @SetJIPipeDocumentation(name = "Export annotations as key-value pairs", description = "The following settings allow you to export annotations as key-value pairs")
    @JIPipeParameter("key-value-pair-exporter")
    public AnnotationsToOMEROKeyValuePairExporter getKeyValuePairExporter() {
        return keyValuePairExporter;
    }

    @SetJIPipeDocumentation(name = "Export list annotation as tag", description = "The following settings allow you to export a single list-like annotation as tag list.")
    @JIPipeParameter("tag-exporter")
    public AnnotationsToOMEROTagExporter getTagExporter() {
        return tagExporter;
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
