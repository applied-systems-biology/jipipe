package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.ArrayList;
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
     * Converts a JIPipe data type to its corresponding ImageJ data type
     *
     * @param data JIPipe data
     * @param parameters Properties of the export operation
     * @return Converted object
     */
    default List<Object> exportData(JIPipeData data, ImageJExportParameters parameters) {
        JIPipeDataTable dataTable = new JIPipeDataTable(data.getClass());
        dataTable.addData(data, new JIPipeProgressInfo());
        return exportData(dataTable, parameters);
    }

    /**
     * @return the {@link JIPipeData} type that is exported by this class
     */
    Class<? extends JIPipeData> getExportedJIPipeDataType();

    /**
     * @return the ImageJ data type that is exported by this importer
     */
    Class<?> getExportedImageJDataType();

    /**
     * A documentation name. Utilizes a {@link org.hkijena.jipipe.api.JIPipeDocumentation} by default (if present). Otherwise, returns the class name.
     * @return the name of this operation
     */
    default String getName() {
        JIPipeDocumentation annotation = getClass().getAnnotation(JIPipeDocumentation.class);
        if(annotation != null) {
            return annotation.name();
        }
        else {
            return getClass().getName();
        }
    }

    /**
     * A documentation description. Utilizes a {@link org.hkijena.jipipe.api.JIPipeDocumentation} by default (if present). Otherwise, returns an empty string.
     * @return the description of this operation
     */
    default String getDescription() {
        JIPipeDocumentation annotation = getClass().getAnnotation(JIPipeDocumentation.class);
        if(annotation != null) {
            return annotation.description();
        }
        else {
            return "";
        }
    }
}
