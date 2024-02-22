package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.List;

public interface ImageJDataExporter {
    /**
     * Converts a JIPipe data type to its corresponding ImageJ data type
     *
     * @param dataTable    JIPipe data as table
     * @param parameters   Properties of the export operation
     * @param progressInfo the progress info
     * @return Converted object
     */
    List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo);

    /**
     * Converts a JIPipe data type to its corresponding ImageJ data type
     *
     * @param data         JIPipe data
     * @param parameters   Properties of the export operation
     * @param progressInfo the progress info
     * @return Converted object
     */
    default List<Object> exportData(JIPipeData data, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(data.getClass());
        dataTable.addData(data, new JIPipeProgressInfo());
        return exportData(dataTable, parameters, progressInfo);
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
     * A documentation name. Utilizes a {@link SetJIPipeDocumentation} by default (if present). Otherwise, returns the class name.
     *
     * @return the name of this operation
     */
    default String getName() {
        SetJIPipeDocumentation annotation = getClass().getAnnotation(SetJIPipeDocumentation.class);
        if (annotation != null) {
            return annotation.name();
        } else {
            return getClass().getName();
        }
    }

    /**
     * A documentation description. Utilizes a {@link SetJIPipeDocumentation} by default (if present). Otherwise, returns an empty string.
     *
     * @return the description of this operation
     */
    default String getDescription() {
        SetJIPipeDocumentation annotation = getClass().getAnnotation(SetJIPipeDocumentation.class);
        if (annotation != null) {
            return annotation.description();
        } else {
            return "";
        }
    }
}
