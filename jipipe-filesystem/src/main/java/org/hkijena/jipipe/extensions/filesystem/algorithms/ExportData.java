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
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                .sealOutput()
                .build());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Path basePath = dataBatch.getInputData("Path", PathData.class).getPath();
        Path outputFolder = basePath.getParent();
        String name = basePath.getFileName().toString();

        if(!Files.exists(outputFolder)) {
            try {
                Files.createDirectories(outputFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        dataBatch.getInputData("Data", JIPipeData.class).saveTo(outputFolder, name, true);
    }

    public ExportData(ExportData other) {
        super(other);
    }
}

