package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@SetJIPipeDocumentation(name = "Empty data", description = "An empty data type")
@JIPipeDataStorageDocumentation(humanReadableDescription = "The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@LabelAsJIPipeHidden
public class JIPipeEmptyData implements JIPipeData {
    public static JIPipeEmptyData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new JIPipeEmptyData();
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

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
