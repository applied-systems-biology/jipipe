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

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.plugins.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.LongList;

@SetJIPipeDocumentation(name = "Define group IDs", description = "Manually defines OMERO group ids.")
@AddJIPipeOutputSlot(value = OMEROGroupReferenceData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (Long projectId : groupIds) {
            iterationStep.addOutputData(getFirstOutputSlot(), new OMEROProjectReferenceData(projectId), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Group IDs", description = "List of group IDs")
    @JIPipeParameter("group-ids")
    public LongList getGroupIds() {
        return groupIds;
    }

    @JIPipeParameter("group-ids")
    public void setGroupIds(LongList groupIds) {
        this.groupIds = groupIds;
    }


}
