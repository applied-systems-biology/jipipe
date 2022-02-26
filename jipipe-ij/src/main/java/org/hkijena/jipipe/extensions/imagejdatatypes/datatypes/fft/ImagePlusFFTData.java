/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.FFT;
import ij.process.FHT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Image in frequency space
 */
@JIPipeDocumentation(name = "FFT Image")
@JIPipeNode(menuPath = "Images\nFFT")
@JIPipeHeavyData
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains two image files: fht.ome.tif / fht.tif and power_spectrum.ome.tif / power_spectrum.tif, as well as a file fht_info.json. Either the OME TIFF or TIFF " +
        "must be present. fht.ome.tif / fht.tif contains the FHT (float32). power_spectrum.ome.tif / power_spectrum.tif contains the power spectrum (float32). " +
        "fht_info.json contains a JSON object that defines following properties: quadrant-swap-needed (boolean), original-width (integer), original-height (integer), " +
        "original-bit-depth (integer; 8, 16, or 32 are valid values), power-spectrum-mean (double).", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-fft-data.schema.json")
@ImageTypeInfo
public class ImagePlusFFTData extends ImagePlusData implements FFTImageData {

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFTData(ImagePlus image) {
        super(convertToFFT(image));
    }

    private static ImagePlus convertToFFT(ImagePlus image) {
        Object fht = image.getProperty("FHT");
        if (fht instanceof FHT) {
            // Already a FFT image
            return image;
        } else {
            // Convert to FFT
            return FFT.forward(image);
        }
    }

    public static ImagePlusFFTData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path storageFolder = storage.getFileSystemPath();
        Path fhtOutputPath = storageFolder.resolve("fht.ome.tif");
        Path powerSpectrumOutputPath = storageFolder.resolve("power_spectrum.ome.tif");
        ImagePlus fhtImage = null;
        ImagePlus powerSpectrumImage = null;
        if (!Files.exists(fhtOutputPath)) {
            fhtOutputPath = storageFolder.resolve("fht.tif");
            fhtImage = IJ.openImage(fhtOutputPath.toString());
        } else {
            fhtImage = OMEImageData.simpleOMEImport(fhtOutputPath).getImage();
        }
        if (!Files.exists(powerSpectrumOutputPath)) {
            powerSpectrumOutputPath = storageFolder.resolve("power_spectrum.tif");
            powerSpectrumImage = IJ.openImage(powerSpectrumOutputPath.toString());
        } else {
            powerSpectrumImage = OMEImageData.simpleOMEImport(powerSpectrumOutputPath).getImage();
        }

        // Combine both
        FHT fht = new FHT(fhtImage.getProcessor(), true);

        // Load info
        Path fhtInfoPath = storageFolder.resolve("fht_info.json");
        if (Files.exists(fhtInfoPath)) {
            try {
                FFTInfo info = JsonUtils.getObjectMapper().readerFor(FFTInfo.class).readValue(fhtInfoPath.toFile());
                info.writeTo(fht);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        powerSpectrumImage.setProperty("FHT", fht);
        return new ImagePlusFFTData(powerSpectrumImage);
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlusFFTData(data.getImage());
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path storageFolder = storage.getFileSystemPath();
        FHT fht = (FHT) getImage().getProperty("FHT");
        ImagePlus fhtImage = new ImagePlus("FHT", fht);

        String fhtImageName = "fht";
        String powerSpectrumImageName = "power_spectrum";

        if (forceName) {
            fhtImageName = name + "_" + fhtImageName;
            powerSpectrumImageName = name + "_" + powerSpectrumImageName;
        }

        if (ImageJDataTypesSettings.getInstance().isUseBioFormats()) {
            Path powerSpectrumOutputPath = storageFolder.resolve(powerSpectrumImageName + ".ome.tif");
            OMEImageData.simpleOMEExport(getImage(), powerSpectrumOutputPath);
            Path fhtOutputPath = storageFolder.resolve(fhtImageName + ".ome.tif");
            OMEImageData.simpleOMEExport(fhtImage, fhtOutputPath);
        } else {
            Path powerSpectrumOutputPath = storageFolder.resolve(powerSpectrumImageName + ".tif");
            IJ.saveAsTiff(getImage(), powerSpectrumOutputPath.toString());
            Path fhtOutputPath = storageFolder.resolve(fhtImageName + ".tif");
            IJ.saveAsTiff(fhtImage, fhtOutputPath.toString());
        }

        Path fhtInfoPath = forceName ? storageFolder.resolve(name + "_fht_info.json") :
                storageFolder.resolve("fht_info.json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fhtInfoPath.toFile(), new FFTInfo(fht));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Class that stores info about the FHT object
     */
    public static class FFTInfo {
        private boolean quadrantSwapNeeded;
        /**
         * Used by the FFT class.
         */
        private int originalWidth;
        /**
         * Used by the FFT class.
         */
        private int originalHeight;
        /**
         * Used by the FFT class.
         */
        private int originalBitDepth;
        /**
         * Used by the FFT class.
         */
        private double powerSpectrumMean;

        public FFTInfo() {

        }

        public FFTInfo(FHT fht) {
            this.quadrantSwapNeeded = fht.quadrantSwapNeeded;
            this.originalWidth = fht.originalWidth;
            this.originalHeight = fht.originalHeight;
            this.originalBitDepth = fht.originalBitDepth;
            this.powerSpectrumMean = fht.powerSpectrumMean;
        }

        public void writeTo(FHT fht) {
            fht.quadrantSwapNeeded = quadrantSwapNeeded;
            fht.originalWidth = originalWidth;
            fht.originalHeight = originalHeight;
            fht.originalBitDepth = originalBitDepth;
            fht.powerSpectrumMean = powerSpectrumMean;
        }

        @JsonGetter("quadrant-swap-needed")
        public boolean isQuadrantSwapNeeded() {
            return quadrantSwapNeeded;
        }

        @JsonSetter("quadrant-swap-needed")
        public void setQuadrantSwapNeeded(boolean quadrantSwapNeeded) {
            this.quadrantSwapNeeded = quadrantSwapNeeded;
        }

        @JsonGetter("original-width")
        public int getOriginalWidth() {
            return originalWidth;
        }

        @JsonSetter("original-width")
        public void setOriginalWidth(int originalWidth) {
            this.originalWidth = originalWidth;
        }

        @JsonGetter("original-height")
        public int getOriginalHeight() {
            return originalHeight;
        }

        @JsonSetter("original-height")
        public void setOriginalHeight(int originalHeight) {
            this.originalHeight = originalHeight;
        }

        @JsonGetter("original-bit-depth")
        public int getOriginalBitDepth() {
            return originalBitDepth;
        }

        @JsonSetter("original-bit-depth")
        public void setOriginalBitDepth(int originalBitDepth) {
            this.originalBitDepth = originalBitDepth;
        }

        @JsonGetter("power-spectrum-mean")
        public double getPowerSpectrumMean() {
            return powerSpectrumMean;
        }

        @JsonSetter("power-spectrum-mean")
        public void setPowerSpectrumMean(double powerSpectrumMean) {
            this.powerSpectrumMean = powerSpectrumMean;
        }
    }
}
