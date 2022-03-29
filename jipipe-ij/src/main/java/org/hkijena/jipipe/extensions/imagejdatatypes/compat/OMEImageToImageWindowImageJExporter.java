package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Open OME Image in ImageJ", description = "Displays the image(s) as ImageJ windows")
public class OMEImageToImageWindowImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        List<Object> result = new ArrayList<>();
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            OMEImageData data = dataTable.getData(row, OMEImageData.class, new JIPipeProgressInfo());
            ImagePlus image;
            if(parameters.isDuplicate()) {
                image = data.getDuplicateImage();
            }
            else {
                image =data.getImage();
            }
            result.add(image);
            if(parameters.isActivate() && !parameters.isNoWindows()) {
                if(!StringUtils.isNullOrEmpty(parameters.getName())) {
                    image.setTitle(parameters.getName());
                }
                image.show();
            }
            if(data.getRois() != null) {
                for (Roi roi : data.getRois()) {
                    roi.setImage(image);
                    RoiManager.getRoiManager().addRoi(roi);
                }
            }
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return OMEImageData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return ImagePlus.class;
    }
}
