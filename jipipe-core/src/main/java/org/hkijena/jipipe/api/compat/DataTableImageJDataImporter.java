package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The fallback/default data adapter that can handle any data type.
 * Its operations are based on importing/exporting data tables via the name parameter
 */
@JIPipeDocumentation(name = "Data table import", description = "Imports a data table directory, provided via the name.")
public class DataTableImageJDataImporter implements ImageJDataImporter {
    public static final String ID = "default";

    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        Path path = Paths.get(parameters.getName());
        return JIPipe.importData(new JIPipeFileSystemReadDataStorage(new JIPipeProgressInfo(), path), JIPipeDataTable.class, new JIPipeProgressInfo());
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
