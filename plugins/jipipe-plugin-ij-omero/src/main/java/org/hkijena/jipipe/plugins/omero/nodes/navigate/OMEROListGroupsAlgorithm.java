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
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.GroupData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.environments.RegisterJIPipeEnvironmentUsage;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;

import java.util.ArrayList;

@SetJIPipeDocumentation(name = "List OMERO groups", description = "Returns the ID(s) of groups(s) according to search criteria.")
@AddJIPipeOutputSlot(value = OMEROGroupReferenceData.class, name = "Groups", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
@RegisterJIPipeEnvironmentUsage(OMEROCredentialsEnvironment.class)
public class OMEROListGroupsAlgorithm extends JIPipeSingleIterationAlgorithm {

    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter("");

    public OMEROListGroupsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListGroupsAlgorithm(OMEROListGroupsAlgorithm other) {
        super(other);
        this.filters = new JIPipeExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getEnvironment(OMEROCredentialsEnvironment.class, runContext, progressInfo);
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            ExperimenterData user = gateway.getUser();
            progressInfo.log("Listing groups ...");
            for (GroupData groupData : user.getGroups()) {
                SecurityContext context = new SecurityContext(groupData.getGroupId());
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
                variables.put("name", groupData.getName());
                variables.put("id", groupData.getId());
                variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, groupData));
                variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, groupData)));
                if (filters.test(variables)) {
                    getFirstOutputSlot().addData(new OMEROGroupReferenceData(groupData.getId()),
                            JIPipeDataContext.create(this),
                            progressInfo);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Keep group if", description = "Allows to filter the returned groups")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per OMERO group")
    @AddJIPipeExpressionParameterVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @AddJIPipeExpressionParameterVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @AddJIPipeExpressionParameterVariable(name = "OMERO group name", description = "Name of the group", key = "name")
    @AddJIPipeExpressionParameterVariable(name = "OMERO group id", description = "ID of the group", key = "id")
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

}
