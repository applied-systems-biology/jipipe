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
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ProjectData;
import omero.gateway.model.ScreenData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
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
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROScreenReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "List OMERO screens", description = "Returns the ID(s) of screens(s) according to search criteria.")
@AddJIPipeInputSlot(value = OMEROGroupReferenceData.class, name = "Group", create = true, description = "The group to be utilized. If not provided, the user's default group is used.", optional = true)
@AddJIPipeOutputSlot(value = OMEROScreenReferenceData.class, name = "Screens", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListScreensAlgorithm extends JIPipeSingleIterationAlgorithm implements OMEROCredentialAccessNode {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListScreensAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListScreensAlgorithm(OMEROListScreensAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected boolean isAllowEmptyIterationStep() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());

        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            if (getFirstInputSlot().getRowCount() > 0) {
                for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                    JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO group", row, getFirstInputSlot().getRowCount());
                    long groupId = getFirstInputSlot().getData(row, OMEROGroupReferenceData.class, rowProgress).getGroupId();
                    processGroupId(rowProgress, groupId, gateway, row, environment);
                }
            } else {
                JIPipeProgressInfo rowProgress = progressInfo.resolveAndLog("OMERO default group (" + gateway.getUser().getGroupId() + ")");
                long groupId = gateway.getUser().getGroupId();
                processGroupId(rowProgress, groupId, gateway, -1, environment);
            }
        }
    }

    private void processGroupId(JIPipeProgressInfo progressInfo, long groupId, OMEROGateway gateway, int row, OMEROCredentialsEnvironment environment) {
        SecurityContext context = new SecurityContext(groupId);
        try {
            for (ScreenData screenData : gateway.getBrowseFacility().getScreens(context)) {
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(this);
                if (row >= 0) {
                    variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));
                }
                variables.put("name", screenData.getName());
                variables.put("description", screenData.getDescription());
                variables.put("protocol.identifier", screenData.getProtocolIdentifier());
                variables.put("protocol.description", screenData.getProtocolDescription());
                variables.put("reagent.identifier", screenData.getReagentSetIdentifier());
                variables.put("reagent.description", screenData.getReagentSetDescripion());
                variables.put("id", screenData.getId());
                variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, screenData));
                variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, screenData)));
                if (filters.test(variables)) {
                    getFirstOutputSlot().addData(new OMEROScreenReferenceData(screenData, environment),
                            getFirstInputSlot().getTextAnnotations(row),
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            getFirstInputSlot().getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting,
                            JIPipeDataContext.create(this),
                            progressInfo);
                }
            }
        } catch (DSOutOfServiceException | DSAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Keep screen if", description = "Allows to filter the returned projects")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO screen")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @AddJIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen name", description = "Name of the screen", key = "name")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen description", description = "Description of the screen", key = "description")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen protocol identifier", description = "Identifier of the protocol", key = "protocol.identifier")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen protocol description", description = "Description of the protocol", key = "protocol.description")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen reagent identifier", description = "Identifier of the reagent", key = "reagent.identifier")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen reagent description", description = "Description of the reagent", key = "reagent.description")
    @AddJIPipeExpressionParameterVariable(name = "OMERO screen id", description = "ID of the screen", key = "id")
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
