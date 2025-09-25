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
import omero.gateway.model.ScreenData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.RegisterJIPipeEnvironmentUsage;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROScreenReferenceData;
import org.hkijena.jipipe.plugins.omero.util.OMEROGateway;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.LongList;

@SetJIPipeDocumentation(name = "Define screen IDs", description = "Manually defines OMERO screen ids.")
@AddJIPipeOutputSlot(value = OMEROScreenReferenceData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
@RegisterJIPipeEnvironmentUsage(OMEROCredentialsEnvironment.class)
public class OMEROScreenReferenceDataSource extends JIPipeSimpleIteratingAlgorithm {

    private LongList screenIds = new LongList();

    public OMEROScreenReferenceDataSource(JIPipeNodeInfo info) {
        super(info);
        screenIds.add(0L);
    }

    public OMEROScreenReferenceDataSource(OMEROScreenReferenceDataSource other) {
        super(other);
        this.screenIds = new LongList(other.screenIds);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = getEnvironment(OMEROCredentialsEnvironment.class, runContext, progressInfo);
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (Long screenId : screenIds) {
                progressInfo.log("Reading info about screen ID=" + screenId);
                ScreenData screenData = gateway.getScreen(screenId, -1);
                iterationStep.addOutputData(getFirstOutputSlot(), new OMEROScreenReferenceData(screenData, environment), progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Screen IDs", description = "List of screen IDs")
    @JIPipeParameter("screen-ids")
    public LongList getScreenIds() {
        return screenIds;
    }

    @JIPipeParameter("screen-ids")
    public void setScreenIds(LongList screenIds) {
        this.screenIds = screenIds;
    }
}
