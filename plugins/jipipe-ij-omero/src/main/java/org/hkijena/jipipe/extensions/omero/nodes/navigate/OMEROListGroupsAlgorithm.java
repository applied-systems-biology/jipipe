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
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.GroupData;
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
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;

import java.util.ArrayList;

@SetJIPipeDocumentation(name = "List OMERO groups", description = "Returns the ID(s) of groups(s) according to search criteria.")
@AddJIPipeOutputSlot(value = OMEROGroupReferenceData.class, slotName = "Groups", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListGroupsAlgorithm extends JIPipeSingleIterationAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListGroupsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListGroupsAlgorithm(OMEROListGroupsAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        LoginCredentials credentials = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials()).toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            ExperimenterData user =  gateway.getUser();
            progressInfo.log("Listing groups ...");
            for (GroupData groupData : user.getGroups()) {
                SecurityContext context = new SecurityContext(groupData.getGroupId());
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
                variables.put("name", groupData.getName());
                variables.put("id", groupData.getId());
                variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, groupData));
                variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, groupData)));
                if(filters.test(variables)) {
                    getFirstOutputSlot().addData(new OMEROGroupReferenceData(groupData.getId()), JIPipeDataContext.create(this), progressInfo);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Allows to filter the returned groups")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO data set")
    @JIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @JIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @JIPipeExpressionParameterVariable(name = "OMERO group name", description = "Name of the group", key = "name")
    @JIPipeExpressionParameterVariable(name = "OMERO group id", description = "ID of the group", key = "id")
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
