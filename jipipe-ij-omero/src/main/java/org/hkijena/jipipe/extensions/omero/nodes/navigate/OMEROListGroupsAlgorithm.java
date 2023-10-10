/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.omero.nodes.navigate;

import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.GroupData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
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

@JIPipeDocumentation(name = "List OMERO groups", description = "Returns the ID(s) of groups(s) according to search criteria.")
@JIPipeOutputSlot(value = OMEROGroupReferenceData.class, slotName = "Groups", autoCreate = true)
@JIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROListGroupsAlgorithm extends JIPipeSingleIterationAlgorithm {

    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();
    private DefaultExpressionParameter filters = new DefaultExpressionParameter("");

    public OMEROListGroupsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROListGroupsAlgorithm(OMEROListGroupsAlgorithm other) {
        super(other);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
        this.filters = new DefaultExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        LoginCredentials credentials = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials()).toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            ExperimenterData user =  gateway.getUser();
            progressInfo.log("Listing groups ...");
            for (GroupData groupData : user.getGroups()) {
                SecurityContext context = new SecurityContext(groupData.getGroupId());
                ExpressionVariables variables = new ExpressionVariables();
                variables.put("name", groupData.getName());
                variables.put("id", groupData.getId());
                variables.put("kv_pairs", OMEROUtils.getKeyValuePairs(gateway.getMetadataFacility(), context, groupData));
                variables.put("tags", new ArrayList<>(OMEROUtils.getTags(gateway.getMetadataFacility(), context, groupData)));
                if(filters.test(variables)) {
                    getFirstOutputSlot().addData(new OMEROGroupReferenceData(groupData.getId()), progressInfo);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Filter", description = "Allows to filter the returned groups")
    @JIPipeParameter("filter")
    @ExpressionParameterSettings(hint = "per OMERO data set")
    @ExpressionParameterSettingsVariable(name = "OMERO tags", description = "List of OMERO tag names associated with the data object", key = "tags")
    @ExpressionParameterSettingsVariable(name = "OMERO key-value pairs", description = "Map containing OMERO key-value pairs with the data object", key = "kv_pairs")
    @ExpressionParameterSettingsVariable(name = "OMERO group name", description = "Name of the group", key = "name")
    @ExpressionParameterSettingsVariable(name = "OMERO group id", description = "ID of the group", key = "id")
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
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

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        report.report(new GraphNodeValidationReportContext(context, this), environment);
    }
}
