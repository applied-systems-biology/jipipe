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

package org.hkijena.jipipe.extensions.omero.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.parameters.primitives.LongList;

@JIPipeDocumentation(name = "Define dataset IDs", description = "Manually defines OMERO dataset ids.")
@JIPipeOutputSlot(value = OMERODatasetReferenceData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMERODatasetReferenceDataSource extends JIPipeSimpleIteratingAlgorithm {

    private LongList datasetIds = new LongList();

    public OMERODatasetReferenceDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public OMERODatasetReferenceDataSource(OMERODatasetReferenceDataSource other) {
        super(other);
        datasetIds = new LongList(other.datasetIds);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        for (Long datasetId : datasetIds) {
            dataBatch.addOutputData(getFirstOutputSlot(), new OMERODatasetReferenceData(datasetId));
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
}
