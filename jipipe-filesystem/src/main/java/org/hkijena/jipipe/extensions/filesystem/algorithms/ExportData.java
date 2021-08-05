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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Export data (path input)", description = "Exports all incoming data to the path specified by the path input data. " +
        "The files will be named according to the last path component. Depending on the data type one or multiple files " +
        "that contain the last path component in their name might be created. " +
        "Duplicate files might be silently overwritten, meaning that the paths should be unique." +
        "Please note that you do not need to explicitly export data, as JIPipe automatically saves all output data.")
@JIPipeInputSlot(JIPipeData.class)
@JIPipeInputSlot(PathData.class)
@JIPipeOutputSlot(PathData.class)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Export")
public class ExportData extends JIPipeIteratingAlgorithm {

    public ExportData(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Data", JIPipeData.class)
                .addInputSlot("Path", PathData.class)
                .addOutputSlot("Path", PathData.class, null)
                .seal()
                .build());
    }

    public ExportData(ExportData other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path basePath = dataBatch.getInputData("Path", PathData.class, progressInfo).toPath();
        Path outputFolder = basePath.getParent();
        String name = basePath.getFileName().toString();

        if (!Files.exists(outputFolder)) {
            try {
                Files.createDirectories(outputFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (StringUtils.isNullOrEmpty(name))
            name = "unnamed";

        dataBatch.getInputData("Data", JIPipeData.class, progressInfo).saveTo(outputFolder, name, true, progressInfo);
        dataBatch.addOutputData("Path", new PathData(outputFolder), progressInfo);
    }
}

