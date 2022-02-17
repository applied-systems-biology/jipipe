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
    public static JIPipeEmptyData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return new JIPipeEmptyData();
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return this;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return "N/A";
    }
}
