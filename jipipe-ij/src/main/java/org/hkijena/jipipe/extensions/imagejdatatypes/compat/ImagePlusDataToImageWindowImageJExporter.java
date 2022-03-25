package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Open in ImageJ", description = "Displays the image(s) as ImageJ windows")
public class ImagePlusDataToImageWindowImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters) {
        List<Object> result = new ArrayList<>();
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            ImagePlusData data = dataTable.getData(row, ImagePlusData.class, new JIPipeProgressInfo());
            result.add(data.getImage());
            if(parameters.isActivate() && !parameters.isNoWindows()) {
                if(!StringUtils.isNullOrEmpty(parameters.getName())) {
                    data.getImage().setTitle(parameters.getName());
                }
                data.getImage().show();
            }
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return ImagePlusData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return ImagePlus.class;
    }
}
