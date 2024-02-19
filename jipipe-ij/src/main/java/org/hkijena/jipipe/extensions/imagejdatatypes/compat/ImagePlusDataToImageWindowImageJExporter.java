package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Open in ImageJ", description = "Displays the image(s) as ImageJ windows")
public class ImagePlusDataToImageWindowImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        List<Object> result = new ArrayList<>();
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            ImagePlusData data = dataTable.getData(row, ImagePlusData.class, new JIPipeProgressInfo());
            ImagePlus image;
            if (parameters.isDuplicate()) {
                image = data.getDuplicateImage();
            } else {
                image = data.getImage();
            }
            result.add(image);
            if (parameters.isActivate()) {
                if (!StringUtils.isNullOrEmpty(parameters.getName())) {
                    image.setTitle(parameters.getName());
                }
                if (!parameters.isNoWindows())
                    WindowManager.setTempCurrentImage(image);
                image.show();
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
