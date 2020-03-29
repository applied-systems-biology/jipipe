package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import ij.WindowManager;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Adapter for a type th
 */
public class ImgPlusDataImageJAdapter implements ImageJDatatypeAdapter {

    private Class<? extends ImagePlusData> acaqDataClass;

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
     *
     * @param imageJData
     * @return
     */
    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        // If we provide a window name, convert it to ImagePlus first
        if (imageJData instanceof String) {
            imageJData = WindowManager.getImage((String) imageJData);
        }
        try {
            ImagePlus img = ((ImagePlus) imageJData).duplicate();
            return (ACAQData) ConstructorUtils.invokeConstructor(acaqDataClass, img);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate, String windowName) {
        ImagePlus img = ((ImagePlusData) acaqData).getImage();
        if (activate) {
            img.show();
            if (!StringUtils.isNullOrEmpty(windowName)) {
                img.setTitle(windowName);
            }
            WindowManager.setTempCurrentImage(img);
        }
        return img;
    }

    @Override
    public ACAQData importFromImageJ(String windowName) {
        ImagePlus image = WindowManager.getImage(windowName);
        return convertImageJToACAQ(image);
    }
}
