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
import omero.gateway.model.DatasetData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.OMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.OMEROSettings;
import org.hkijena.jipipe.extensions.omero.OptionalOMEROCredentialsEnvironment;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.util.OMEROGateway;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.LongList;

@JIPipeDocumentation(name = "Define dataset IDs", description = "Manually defines OMERO dataset ids.")
@JIPipeOutputSlot(value = OMERODatasetReferenceData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMERODatasetReferenceDataSource extends JIPipeSimpleIteratingAlgorithm {

    private LongList datasetIds = new LongList();
    private OptionalOMEROCredentialsEnvironment overrideCredentials = new OptionalOMEROCredentialsEnvironment();

    public OMERODatasetReferenceDataSource(JIPipeNodeInfo info) {
        super(info);
        datasetIds.add(0L);
    }

    public OMERODatasetReferenceDataSource(OMERODatasetReferenceDataSource other) {
        super(other);
        datasetIds = new LongList(other.datasetIds);
        this.overrideCredentials = new OptionalOMEROCredentialsEnvironment(other.overrideCredentials);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        OMEROCredentialsEnvironment environment = overrideCredentials.getContentOrDefault(OMEROSettings.getInstance().getDefaultCredentials());
        LoginCredentials credentials = environment.toLoginCredentials();
        progressInfo.log("Connecting to " + credentials.getUser().getUsername() + "@" + credentials.getServer().getHost());
        try (OMEROGateway gateway = new OMEROGateway(credentials, progressInfo)) {
            for (Long datasetId : datasetIds) {
                progressInfo.log("Reading info about dataset ID=" + datasetId);
                DatasetData dataset = gateway.getDataset(datasetId, -1);
                iterationStep.addOutputData(getFirstOutputSlot(), new OMERODatasetReferenceData(dataset, environment), progressInfo);
            }
        }
    }

    @JIPipeDocumentation(name = "Dataset IDs", description = "List of dataset IDs")
    @JIPipeParameter("dataset-ids")
    public LongList getDatasetIds() {
        return datasetIds;
    }

    @JIPipeParameter("dataset-ids")
    public void setDatasetIds(LongList datasetIds) {
        this.datasetIds = datasetIds;
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
