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

package org.hkijena.jipipe.extensions.omero.nodes.navigate;

import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;

import java.util.ArrayList;

@SetJIPipeDocumentation(name = "List OMERO projects", description = "Returns the ID(s) of project(s) according to search criteria.")
@AddJIPipeInputSlot(value = OMEROGroupReferenceData.class, slotName = "Group", create = true, description = "The group to be utilized. If not provided, the user's default group is used.", optional = true)
@AddJIPipeOutputSlot(value = OMEROProjectReferenceData.class, slotName = "Projects", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListProjectsAlgorithm extends JIPipeSingleIterationAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListProjectsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListProjectsAlgorithm(OMEROListProjectsAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());

        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            if(getFirstInputSlot().getRowCount() > 0) {
                for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                    JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO group", row, getFirstInputSlot().getRowCount());
                    long groupId = getFirstInputSlot().getData(row, OMEROGroupReferenceData.class, rowProgress).getGroupId();
                    processGroupId(rowProgress, groupId, gateway, row, environment);
                }
            }
            else {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO default group (" + gateway.getUser().getGroupId() + ")");
                long groupId = gateway.getUser().getGroupId();
                processGroupId(rowProgress, groupId, gateway, -1, environment);
            }
        }
    }

    private void processGroupId(JIPipeProgressInfo progressInfo, long groupId, OMEROGateway gateway, int row, OMEROCredentialsEnvironment environment) {
        SecurityContext context = new SecurityContext(groupId);
        try {
            for (ProjectData project : gateway.getBrowseFacility().getProjects(context)) {
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
                if(row >= 0) {
                    variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));
                }
                variables.put("name", project.getName());
                variables.put("id", project.getId());
                variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, project));
                variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, project)));
                if(filters.test(variables)) {
                    getFirstOutputSlot().addData(new OMEROProjectReferenceData(project, environment), JIPipeDataContext.create(this), progressInfo);
                }
            }
        } catch (DSOutOfServiceException | DSAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Allows to filter the returned projects")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO data set")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @JIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @JIPipeExpressionParameterVariable(name = "OMERO project name", description = "Name of the project", key = "name")
    @JIPipeExpressionParameterVariable(name = "OMERO project id", description = "ID of the project", key = "id")
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
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(reportContext, this), environment);
    }
}
