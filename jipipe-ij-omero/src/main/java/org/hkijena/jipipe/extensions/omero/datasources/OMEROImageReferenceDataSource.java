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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.parameters.primitives.LongList;

import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Define image IDs", description = "Manually defines OMERO image ids that can be used for importing data.")
@JIPipeOutputSlot(value = OMEROImageReferenceData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class, menuPath = "OMERO")
public class OMEROImageReferenceDataSource extends JIPipeSimpleIteratingAlgorithm {

    private LongList imageIds = new LongList();

    public OMEROImageReferenceDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public OMEROImageReferenceDataSource(OMEROImageReferenceDataSource other) {
        super(other);
        imageIds = new LongList(other.imageIds);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (Long imageId : imageIds) {
            dataBatch.addOutputData(getFirstOutputSlot(), new OMEROImageReferenceData(imageId));
        }
    }

    @JIPipeDocumentation(name = "Image IDs", description = "List of image IDs")
    @JIPipeParameter("image-ids")
    public LongList getImageIds() {
        return imageIds;
    }

    @JIPipeParameter("image-ids")
    public void setImageIds(LongList imageIds) {
        this.imageIds = imageIds;
    }
}
