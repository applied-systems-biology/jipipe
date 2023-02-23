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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Export data table (path input)", description = "Exports all incoming data as data table directory.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", description = "The data to be exported", autoCreate = true)
@JIPipeInputSlot(value = PathData.class, slotName = "Path", description = "The directory where the data will be stored", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Path", description = "The directory where the data was stored", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportDataTable extends JIPipeMergingAlgorithm {

    public ExportDataTable(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportDataTable(ExportDataTable other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path outputDirectory = dataBatch.getInputData("Path", PathData.class, progressInfo).get(0).toPath();
        if (outputDirectory == null || outputDirectory.toString().isEmpty() || !outputDirectory.isAbsolute()) {
            outputDirectory = getFirstOutputSlot().getSlotStoragePath().resolve(StringUtils.nullToEmpty(outputDirectory));
        }
        if (!Files.exists(outputDirectory)) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        JIPipeDataSlot batchSlot = dataBatch.toDummySlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Output", ""), this, getInputSlot("Data"));
        batchSlot.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputDirectory), progressInfo);
        dataBatch.addOutputData("Path", new PathData(outputDirectory), progressInfo);
    }
}

