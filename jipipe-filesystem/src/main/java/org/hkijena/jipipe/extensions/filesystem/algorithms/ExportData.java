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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Export data", description = "Exports all incoming data to the path specified by the path input data. " +
        "The files will be named according to the last path component. Depending on the data type one or multiple files " +
        "that contain the last path component in their name might be created. " +
        "Duplicate files might be silently overwritten, meaning that the paths should be unique." +
        "Please note that you do not need to explicitly export data, as JIPipe automatically saves all output data.")
@JIPipeInputSlot(JIPipeData.class)
@JIPipeInputSlot(PathData.class)
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Miscellaneous)
public class ExportData extends JIPipeIteratingAlgorithm {

    public ExportData(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Data", JIPipeData.class)
                .addInputSlot("Path", PathData.class)
                .seal()
                .build());
    }

    public ExportData(ExportData other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Path basePath = dataBatch.getInputData("Path", PathData.class).getPath();
        Path outputFolder = basePath.getParent();
        String name = basePath.getFileName().toString();

        if (!Files.exists(outputFolder)) {
            try {
                Files.createDirectories(outputFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        dataBatch.getInputData("Data", JIPipeData.class).saveTo(outputFolder, name, true);
    }
}

