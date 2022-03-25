package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.List;

/**
 * An importer that does nothing
 */
@JIPipeDocumentation(name = "No importer", description = "Nothing is imported")
public class EmptyImageJDataImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters) {
        return new JIPipeDataTable(JIPipeData.class);
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return JIPipeData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return Object.class;
    }
}
