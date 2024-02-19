package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.Collections;
import java.util.List;

/**
 * An exporter that does nothing
 */
@SetJIPipeDocumentation(name = "No export", description = "Nothing is exported")
public class EmptyImageJDataExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        return Collections.emptyList();
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return JIPipeData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return Object.class;
    }
}
