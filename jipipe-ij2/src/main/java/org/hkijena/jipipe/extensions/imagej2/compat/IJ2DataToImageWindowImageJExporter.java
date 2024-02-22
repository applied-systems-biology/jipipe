package org.hkijena.jipipe.extensions.imagej2.compat;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataToImageWindowImageJExporter;

import java.util.List;

@SetJIPipeDocumentation(name = "Open IJ2 Dataset in ImageJ", description = "Displays the image(s) as ImageJ windows")
public class IJ2DataToImageWindowImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        ImagePlusDataToImageWindowImageJExporter exporter = new ImagePlusDataToImageWindowImageJExporter();
        return exporter.exportData(dataTable, parameters, progressInfo);
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return ImageJ2DatasetData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return ImagePlus.class;
    }
}
