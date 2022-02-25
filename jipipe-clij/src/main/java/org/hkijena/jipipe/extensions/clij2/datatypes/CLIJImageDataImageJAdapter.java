package org.hkijena.jipipe.extensions.clij2.datatypes;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for {@link CLIJImageData}
 */
public class CLIJImageDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return imageJData instanceof ImagePlus || imageJData instanceof ClearCLBuffer;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return jipipeData instanceof CLIJImageData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ImagePlus.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return CLIJImageData.class;
    }

    @Override
    public JIPipeData convertImageJToJIPipe(Object imageJData) {
        if (imageJData instanceof String) {
            imageJData = WindowManager.getImage((String) imageJData);
        }
        if (imageJData == null) {
            imageJData = IJ.getImage();
        }
        if (imageJData instanceof ImagePlus) {
            return new CLIJImageData(new ImagePlusData((ImagePlus) imageJData));
        } else if (imageJData instanceof ClearCLBuffer) {
            return new CLIJImageData((ClearCLBuffer) imageJData);
        } else {
            throw new IllegalArgumentException("Unknown data type: " + imageJData);
        }
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        CLIJImageData data = (CLIJImageData) jipipeData;
        ImagePlusData imgData = data.pull();
        ImagePlus img = imgData.getImage();
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
