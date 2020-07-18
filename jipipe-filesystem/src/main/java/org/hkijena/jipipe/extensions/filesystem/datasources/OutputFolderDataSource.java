package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Run output folder", description = "Generates a path that points to the data output folder of the current run.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output", autoCreate = true)
public class OutputFolderDataSource extends JIPipeAlgorithm {


    public OutputFolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public OutputFolderDataSource(OutputFolderDataSource other) {
        super(other);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Path storagePath = getFirstOutputSlot().getStoragePath().getParent().getParent().getParent();
        getFirstOutputSlot().addData(new PathData(storagePath));
    }
}
