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
 *
 */

package org.hkijena.jipipe.extensions.ij3d.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;

import java.nio.file.Path;

@JIPipeDocumentation(name = "Import 3D ROI", description = "Imports a 3D ROI list from a *.zip file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true, description = "A *.zip file")
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class ImportROI3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ImportROI3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportROI3DAlgorithm(ImportROI3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path path = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        ROI3DListData roi3D = ROI3DListData.importData(path, progressInfo.resolve("Import ROI3D"));
        dataBatch.addOutputData(getFirstOutputSlot(), roi3D, progressInfo);
    }
}
