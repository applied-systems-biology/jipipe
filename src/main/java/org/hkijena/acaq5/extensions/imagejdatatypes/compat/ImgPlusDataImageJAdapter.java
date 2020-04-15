package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Adapter between {@link ImagePlus} and {@link ImagePlusData}
 */
public class ImgPlusDataImageJAdapter implements ImageJDatatypeAdapter {

    private Class<? extends ImagePlusData> acaqDataClass;

    /**
     * @param acaqDataClass the ACAQ data class
     */
    public ImgPlusDataImageJAdapter(Class<? extends ImagePlusData> acaqDataClass) {
        this.acaqDataClass = acaqDataClass;
    }

    @Override
    public boolean canConvertImageJToACAQ(Object imageJData) {
        return imageJData instanceof ImagePlus;
    }

    @Override
    public boolean canConvertACAQToImageJ(ACAQData acaqData) {
        return acaqData.getClass() == acaqDataClass;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ImagePlus.class;
    }

    @Override
    public Class<? extends ACAQData> getACAQDatatype() {
        return acaqDataClass;
    }

    /**
     * Converts {@link ImagePlus} to an {@link ImagePlusData} instance.
     * If the imageJData is a {@link String}, the image is taken from the window with this name
     * If the imageJData is null, it is taken from the current WindowManager image
     *
     * @param imageJData the ImageJ data
     * @return the converted data
     */
    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        // If we provide a window name, convert it to ImagePlus first
        if (imageJData instanceof String) {
            imageJData = WindowManager.getImage((String) imageJData);
        }
        if (imageJData == null) {
            imageJData = IJ.getImage();
        }
        try {
            ImagePlus img = ((ImagePlus) imageJData).duplicate();
            return (ACAQData) ConstructorUtils.invokeConstructor(acaqDataClass, img);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate, boolean noWindow, String windowName) {
        ImagePlus img = ((ImagePlusData) acaqData).getImage().duplicate();
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
    public ACAQData importFromImageJ(String windowName) {
        if (StringUtils.isNullOrEmpty(windowName))
            return convertImageJToACAQ(IJ.getImage());
        ImagePlus image = WindowManager.getImage(windowName);
        return convertImageJToACAQ(image);
    }
}
