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

package org.hkijena.jipipe.plugins.omero.nodes.datasources;

import omero.gateway.LoginCredentials;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialAccessNode;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.OMEROPluginApplicationSettings;
import org.hkijena.jipipe.plugins.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.LongList;

import java.util.List;

@SetJIPipeDocumentation(name = "Define project IDs", description = "Manually defines OMERO project ids.")
@AddJIPipeOutputSlot(value = OMEROProjectReferenceData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROProjectReferenceDataSource extends JIPipeSimpleIteratingAlgorithm implements OMEROCredentialAccessNode {

    private LongList projectIds = new LongList();
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();

    public OMEROProjectReferenceDataSource(JIPipeNodeInfo info) {
        super(info);
        projectIds.add(0L);
    }

    public OMEROProjectReferenceDataSource(OMEROProjectReferenceDataSource other) {
        super(other);
        this.projectIds = new LongList(other.projectIds);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getConfiguredOMEROCredentialsEnvironment();
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (Long projectId : projectIds) {
                progressInfo.log("Reading info about project ID=" + projectId);
                ProjectData projectData = gateway.getProject(projectId, -1);
                iterationStep.addOutputData(getFirstOutputSlot(), new OMEROProjectReferenceData(projectData, environment), progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Project IDs", description = "List of project IDs")
    @JIPipeParameter("dataset-ids")
    public LongList getProjectIds() {
        return projectIds;
    }

    @JIPipeParameter("dataset-ids")
    public void setProjectIds(LongList projectIds) {
        this.projectIds = projectIds;
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
