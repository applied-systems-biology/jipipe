package org.hkijena.jipipe.extensions.imagej2.compat;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import net.imagej.Dataset;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Import of a {@link Dataset} via the {@link ImagePlus} reader
 */
public class ImageJ2DataSetDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return imageJData instanceof ImagePlus;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return jipipeData instanceof ImageJ2DatasetData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ImagePlus.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return ImageJ2DatasetData.class;
    }

    @Override
    public JIPipeData convertImageJToJIPipe(Object imageJData) {
        // If we provide a window name, convert it to ImagePlus first
        if (imageJData instanceof String) {
            imageJData = WindowManager.getImage((String) imageJData);
        }
        if (imageJData == null) {
            imageJData = IJ.getImage();
        }
        ImagePlus img = ImageJUtils.duplicate((ImagePlus) imageJData);
        return new ImageJ2DatasetData(img);
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        ImagePlus img = ((ImageJ2DatasetData) jipipeData).wrap().getDuplicateImage();
        if (activate) {
            if (!noWindow) {
                img.show();
                if (!StringUtils.isNullOrEmpty(windowName)) {
                    img.setTitle(windowName);
                }
            }
            WindowManager.setTempCurrentImage(img);
        }
        return img;
    }

    @Override
    public List<Object> convertMultipleJIPipeToImageJ(List<JIPipeData> jipipeData, boolean activate, boolean noWindow, String windowName) {
        List<Object> result = new ArrayList<>();
        for (JIPipeData data : jipipeData) {
            result.add(convertJIPipeToImageJ(data, activate, noWindow, windowName));
        }
        return result;
    }

    @Override
    public JIPipeData importDataImageJ(String parameters) {
        if (StringUtils.isNullOrEmpty(parameters))
            return convertImageJToJIPipe(IJ.getImage());
        ImagePlus image = WindowManager.getImage(parameters);
        return convertImageJToJIPipe(image);
    }
}
