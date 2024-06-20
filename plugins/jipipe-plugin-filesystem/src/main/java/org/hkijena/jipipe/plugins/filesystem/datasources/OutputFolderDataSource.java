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

package org.hkijena.jipipe.plugins.filesystem.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;

import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Run output folder", description = "Generates a path that points to the data output folder of the current run.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = FolderData.class, name = "Output", create = true)
public class OutputFolderDataSource extends JIPipeAlgorithm {


    public OutputFolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public OutputFolderDataSource(OutputFolderDataSource other) {
        super(other);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path storagePath = getFirstOutputSlot().getSlotStoragePath().getParent().getParent().getParent();
        getFirstOutputSlot().addData(new PathData(storagePath), JIPipeDataContext.create(this), progressInfo);
    }
}
