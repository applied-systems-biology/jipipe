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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.LongList;

@JIPipeDocumentation(name = "Define group IDs", description = "Manually defines OMERO group ids.")
@JIPipeOutputSlot(value = OMEROGroupReferenceData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROGroupReferenceDataSource extends JIPipeSimpleIteratingAlgorithm {

    private LongList groupIds = new LongList();

    public OMEROGroupReferenceDataSource(JIPipeNodeInfo info) {
        super(info);
        groupIds.add(0L);
    }

    public OMEROGroupReferenceDataSource(OMEROGroupReferenceDataSource other) {
        super(other);
        groupIds = new LongList(other.groupIds);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        for (Long projectId : groupIds) {
            dataBatch.addOutputData(getFirstOutputSlot(), new OMEROProjectReferenceData(projectId), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Group IDs", description = "List of group IDs")
    @JIPipeParameter("group-ids")
    public LongList getGroupIds() {
        return groupIds;
    }

    @JIPipeParameter("group-ids")
    public void setGroupIds(LongList groupIds) {
        this.groupIds = groupIds;
    }
}