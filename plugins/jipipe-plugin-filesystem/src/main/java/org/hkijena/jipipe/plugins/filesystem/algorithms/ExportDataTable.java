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

package org.hkijena.jipipe.plugins.filesystem.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Export data table (path input)", description = "Exports all incoming data as data table directory.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", description = "The data to be exported", create = true)
@AddJIPipeInputSlot(value = PathData.class, name = "Path", description = "The directory where the data will be stored", create = true)
@AddJIPipeOutputSlot(value = PathData.class, name = "Path", description = "The directory where the data was stored", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class)
public class ExportDataTable extends JIPipeMergingAlgorithm {

    public ExportDataTable(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportDataTable(ExportDataTable other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path outputDirectory = iterationStep.getInputData("Path", PathData.class, progressInfo).get(0).toPath();
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
        JIPipeDataSlot batchSlot = iterationStep.toDummySlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Output", ""), this, getInputSlot("Data"), progressInfo);
        batchSlot.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, outputDirectory), progressInfo);
        iterationStep.addOutputData("Path", new PathData(outputDirectory), progressInfo);
    }
}

