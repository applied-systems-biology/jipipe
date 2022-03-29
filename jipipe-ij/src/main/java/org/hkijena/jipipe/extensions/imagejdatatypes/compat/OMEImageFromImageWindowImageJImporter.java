package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@JIPipeDocumentation(name = "Import OME Image from ImageJ window", description = "Imports an image window into JIPipe")
public class OMEImageFromImageWindowImageJImporter implements ImageJDataImporter {

    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus;
        if (StringUtils.isNullOrEmpty(parameters.getName())) {
            imagePlus = IJ.getImage(); // The active image
        } else {
            imagePlus = WindowManager.getImage(parameters.getName());
        }
        if (parameters.isDuplicate()) {
            String title = imagePlus.getTitle();
            imagePlus = imagePlus.duplicate();
            imagePlus.setTitle(title);
        }
        JIPipeDataTable result = new JIPipeDataTable(OMEImageData.class);
        ROIListData rois = new ROIListData();
        for (Roi roi : RoiManager.getRoiManager().getRoisAsArray()) {
            if (roi.getImage() == imagePlus) {
                if (parameters.isDuplicate())
                    rois.add((Roi) roi.clone());
                else
                    rois.add(roi);
            }
        }
        OMEImageData omeImageData = new OMEImageData(imagePlus, rois, null);
        result.addData(omeImageData, new JIPipeProgressInfo());
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return OMEImageData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ImagePlus.class;
    }
}
