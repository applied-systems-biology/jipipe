package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.List;

public interface ImageJDataExporter {
    /**
     * Converts a JIPipe data type to its corresponding ImageJ data type
     *
     * @param dataTable JIPipe data as table
     * @param parameters Properties of the export operation
     * @return Converted object
     */
    List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters);

    /**
     * @return the {@link JIPipeData} type that is exported by this class
     */
    Class<? extends JIPipeData> getExportedJIPipeDataType();

    /**
     * @return the ImageJ data type that is exported by this importer
     */
    Class<?> getExportedImageJDataType();
}
