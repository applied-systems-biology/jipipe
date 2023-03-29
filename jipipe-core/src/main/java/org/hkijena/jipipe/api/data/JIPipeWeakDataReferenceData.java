package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * A helper data type that weakly references other data
 */
@JIPipeDocumentation(name = "Weak data reference", description = "References other data (weakly).")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@JIPipeHidden
public class JIPipeWeakDataReferenceData implements JIPipeData {

    private final WeakReference<JIPipeData> dataReference;

    public JIPipeWeakDataReferenceData(JIPipeData target) {
        dataReference = new WeakReference<>(target);
    }

    public static JIPipeWeakDataReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new JIPipeWeakDataReferenceData(null);
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataReference.get();
        if (data != null) {
            data.exportData(storage, name, forceName, progressInfo);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeWeakDataReferenceData(dataReference.get());
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JIPipeData data = dataReference.get();
        if (data != null) {
            data.display(displayName, workbench, source);
        }
    }

    @Override
    public Component preview(int width, int height) {
        JIPipeData data = dataReference.get();
        if (data != null) {
            return data.preview(width, height);
        } else {
            return null;
        }
    }

    public WeakReference<JIPipeData> getDataReference() {
        return dataReference;
    }
}
