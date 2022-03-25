package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@JIPipeDocumentation(description = "Imports an image window into JIPipe")
public class ImagePlusDataFromImageWindowImageJImporter implements ImageJDataImporter {

    private final Class<? extends ImagePlusData> dataClass;

    public ImagePlusDataFromImageWindowImageJImporter(Class<? extends ImagePlusData> dataClass) {
        this.dataClass = dataClass;
    }

    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters) {
        ImagePlus imagePlus;
        if(StringUtils.isNullOrEmpty(parameters.getName())) {
            imagePlus = IJ.getImage(); // The active image
        }
        else {
            imagePlus = WindowManager.getImage(parameters.getName());
        }
        JIPipeDataTable result = new JIPipeDataTable(getImportedJIPipeDataType());
        result.addData(new ImagePlusData(imagePlus), new JIPipeProgressInfo());
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return dataClass;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ImagePlus.class;
    }

    @Override
    public String getName() {
        return "Import " + JIPipeDataInfo.getInstance(dataClass).getName() + " from ImageJ window";
    }
}
