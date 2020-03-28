package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.awt.*;
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

    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        try {
            return (ACAQData)ConstructorUtils.invokeConstructor(acaqDataClass, imageJData);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate) {
        ImagePlus imagePlus = ((ImagePlusData)acaqData).getImage();
        if(activate) {
            imagePlus.show();
        }
        return imagePlus;
    }
}
