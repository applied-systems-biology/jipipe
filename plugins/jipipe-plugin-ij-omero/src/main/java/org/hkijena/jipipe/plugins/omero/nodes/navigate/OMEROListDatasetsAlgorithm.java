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

package org.hkijena.jipipe.plugins.omero.nodes.navigate;

import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "List OMERO datasets", description = "Returns the ID(s) of dataset(s) according to search criteria. Requires project IDs as input.")
@AddJIPipeInputSlot(value = OMEROProjectReferenceData.class, name = "Projects", create = true)
@AddJIPipeOutputSlot(value = OMERODatasetReferenceData.class, name = "Datasets", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListDatasetsAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListDatasetsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListDatasetsAlgorithm(OMEROListDatasetsAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO project", row, getFirstInputSlot().getRowCount());
                long projectId = getFirstInputSlot().getData(row, OMEROProjectReferenceData.class, rowProgress).getProjectId();
                rowProgress.log("Listing datasets in project ID=" + projectId);
                ProjectData projectData = gateway.getProject(projectId, -1);
                SecurityContext context = new SecurityContext(projectData.getGroupId());
                for (DatasetData dataset : projectData.getDatasets()) {
                    JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
                    variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));
                    variables.put("name", dataset.getName());
                    variables.put("description", dataset.getDescription());
                    variables.put("id", dataset.getId());
                    variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, dataset));
                    variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, dataset)));

                    if (filters.test(variables)) {
                        getFirstOutputSlot().addData(new OMERODatasetReferenceData(dataset, environment),
                                getFirstInputSlot().getTextAnnotations(row),
                                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                                getFirstInputSlot().getDataAnnotations(row),
                                JIPipeDataAnnotationMergeMode.OverwriteExisting,
                                getFirstInputSlot().getDataContext(row).branch(this),
                                rowProgress);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Keep dataset if", description = "Allows to filter the returned data sets")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO data set")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @AddJIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @AddJIPipeExpressionParameterVariable(name = "OMERO dataset name", description = "Name of the data set", key = "name")
    @AddJIPipeExpressionParameterVariable(name = "OMERO dataset description", description = "Name of the data set", key = "description")
    @AddJIPipeExpressionParameterVariable(name = "OMERO dataset id", description = "ID of the data set", key = "id")
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
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
