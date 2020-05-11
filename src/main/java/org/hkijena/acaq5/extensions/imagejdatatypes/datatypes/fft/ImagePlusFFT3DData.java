package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 3D image in frequency space
 */
@ACAQDocumentation(name = "3D FFT Image")
@ACAQOrganization(menuPath = "Images\nFFT")
public class ImagePlusFFT3DData extends ImagePlusFFT2DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFT3DData(ImagePlus image) {
        super(image);

        if (image.getNDimensions() > 3) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 3D data!");
        }
    }

}
