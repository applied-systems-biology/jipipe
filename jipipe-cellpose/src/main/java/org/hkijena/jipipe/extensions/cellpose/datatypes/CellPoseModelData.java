package org.hkijena.jipipe.extensions.cellpose.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

/**
 * Wrapper around Cellpose models
 */
@JIPipeDocumentation(name = "Cellpose model", description = "A Cellpose model")
@JIPipeDataStorageDocumentation("A single file without extension that contains the Cellpose model")
public class CellPoseModelData implements JIPipeData {
    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate() {
        return new CellPoseModelData();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    public static CellPoseModelData importFrom(Path storagePath) {
        return new CellPoseModelData();
    }
}
