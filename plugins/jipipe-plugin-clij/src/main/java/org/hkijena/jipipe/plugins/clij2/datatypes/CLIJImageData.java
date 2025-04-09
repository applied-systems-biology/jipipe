/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.clij2.datatypes;

import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.clij2.CLIJPluginApplicationSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * Contains a CLIJ image
 */
@SetJIPipeDocumentation(name = "GPU image", description = "Image data stored on the GPU utilized by CLIJ")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpeg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@LabelAsJIPipeHeavyData
public class CLIJImageData implements JIPipeData {

    ClearCLBuffer image;

    /**
     * Initializes a new instance from an existing buffer
     *
     * @param image the buffer
     */
    public CLIJImageData(ClearCLBuffer image) {
        this.image = image;
    }

    /**
     * Initializes an instance by copying data from {@link ImagePlusData}
     *
     * @param image the image
     */
    public CLIJImageData(ImagePlusData image) {
        CLIJ2 clij = CLIJ2.getInstance();
        this.image = clij.push(image.getImage());
    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public CLIJImageData(CLIJImageData other) {
        CLIJ2 clij = CLIJ2.getInstance();
        this.image = clij.create(other.image);
        other.image.copyTo(this.image, true);
    }

    public static CLIJImageData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new CLIJImageData(ImagePlusData.importData(storage, progressInfo));
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = pull();
        data.exportData(storage, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new CLIJImageData(this);
    }

    @Override
    public void close() {
        try {
            image.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        image = null;
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = pull();
        if (data != null) {
            return data.createThumbnail(width, height, progressInfo);
        } else {
            return null;
        }
    }

    /**
     * Extracts {@link ImagePlusData} from the GPU memory
     *
     * @return the {@link ImagePlusData}
     */
    public ImagePlusData pull() {
        CLIJ2 clij = CLIJ2.getInstance();
        ImagePlus imagePlus = clij.pull(image);
        if (CLIJPluginApplicationSettings.getInstance().isAutoCalibrateAfterPulling()) {
            CLIJPluginApplicationSettings.ContrastEnhancerSettings contrastEnhancer = CLIJPluginApplicationSettings.getInstance().getContrastEnhancerSettings();
            ImageJUtils.calibrate(imagePlus,
                    contrastEnhancer.getCalibrationMode(),
                    contrastEnhancer.getCustomMin(),
                    contrastEnhancer.getCustomMax());
        }
        return new ImagePlusData(imagePlus);
    }

    public ClearCLBuffer getImage() {
        return image;
    }

    @Override
    public String toString() {
        if (image != null)
            return image.toString();
        else
            return "<Null>";
    }
}
