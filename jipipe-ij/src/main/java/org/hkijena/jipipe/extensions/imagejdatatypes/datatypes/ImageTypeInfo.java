package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes;

import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.IgnoreColorSpace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Carries information about an {@link ImagePlusData} type that can be used for filtering the image
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ImageTypeInfo {

    /**
     * Information about dimension constraints (2D, 3D, ...)
     * If positive, the lowest number must be 2.
     * If negative, no constraints are set.
     *
     * @return the number if dimensions. If negative, no dimension information is assigned.
     */
    int numDimensions() default -1;

    /**
     * Returns the bit depth of the image type (according to ImageJ)
     * If negative, no constraints are set.
     *
     * @return the bit depth or no constraints if negative
     */
    int bitDepth() default -1;

    /**
     * Constrains on the pixel type.
     * Use {@link Object} to indicate no constraints.
     * Use any supported {@link Number} type to set the pixel type.
     * Please note that this type can differ from the bitDepth
     *
     * @return the pixel type constraint
     */
    Class<?> pixelType() default Object.class;

    /**
     * The type of the image processor used by images of this type.
     * Set to {@link ImageProcessor} for no constraints
     *
     * @return the image processor type
     */
    Class<? extends ImageProcessor> imageProcessorType() default ImageProcessor.class;

    /**
     * Constraints on the color space.
     *
     * @return the color space. Returns {@link IgnoreColorSpace} to indicate that there are no constraints.
     */
    Class<? extends ColorSpace> colorSpace() default IgnoreColorSpace.class;
}
