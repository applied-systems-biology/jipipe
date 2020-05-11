package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 4D image in frequency space
 */
@ACAQDocumentation(name = "4D FFT Image")
@ACAQOrganization(menuPath = "Images\nFFT")
public class ImagePlusFFT4DData extends ImagePlusFFT2DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFT4DData(ImagePlus image) {
        super(image);

        if (image.getNDimensions() > 4) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 4D data!");
        }
    }

}
