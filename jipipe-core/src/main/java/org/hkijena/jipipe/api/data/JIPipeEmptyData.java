package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

@JIPipeDocumentation(name = "Empty data", description = "An empty data type")
@JIPipeDataStorageDocumentation("The storage folder is empty.")
@JIPipeHidden
public class JIPipeEmptyData implements JIPipeData {
    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate() {
        return this;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return "N/A";
    }

    public static JIPipeEmptyData importFrom(Path storagePath) {
        return new JIPipeEmptyData();
    }
}
