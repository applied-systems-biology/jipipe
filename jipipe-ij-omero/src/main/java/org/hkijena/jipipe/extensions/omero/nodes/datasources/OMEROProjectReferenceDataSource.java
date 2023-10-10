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

package org.hkijena.jipipe.extensions.omero.nodes.datasources;

import omero.gateway.LoginCredentials;
import omero.gateway.model.ProjectData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.LongList;

@JIPipeDocumentation(name = "Define project IDs", description = "Manually defines OMERO project ids.")
@JIPipeOutputSlot(value = OMEROProjectReferenceData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROProjectReferenceDataSource extends JIPipeSimpleIteratingAlgorithm {

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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (Long projectId : projectIds) {
                progressInfo.log("Reading info about project ID=" + projectId);
                ProjectData projectData = gateway.getProject(projectId, -1);
                dataBatch.addOutputData(getFirstOutputSlot(), new OMEROProjectReferenceData(projectData, environment), progressInfo);
            }
        }
    }

    @JIPipeDocumentation(name = "Project IDs", description = "List of project IDs")
    @JIPipeParameter("dataset-ids")
    public LongList getProjectIds() {
        return projectIds;
    }

    @JIPipeParameter("dataset-ids")
    public void setProjectIds(LongList projectIds) {
        this.projectIds = projectIds;
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
}
