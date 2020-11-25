package org.hkijena.jipipe.extensions.clij2.datatypes;

import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.clij2.CLIJSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.DisplayRangeCalibrationAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ImageJUtils;

import java.awt.Component;
import java.nio.file.Path;

/**
 * Contains a CLIJ image
 */
@JIPipeDocumentation(name = "GPU image", description = "Image data stored on the GPU utilized by CLIJ")
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

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = pull();
        data.saveTo(storageFilePath, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate() {
        return new CLIJImageData(this);
    }

    @Override
    public void finalize() throws Throwable {
        image.close();
        image = null;
        super.finalize();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        ImagePlusData data = pull();
        data.display(displayName, workbench, source);
    }

    @Override
    public Component preview(int width, int height) {
        ImagePlusData data = pull();
        return data.preview(width, height);
    }

    /**
     * Extracts {@link ImagePlusData} from the GPU memory
     *
     * @return the {@link ImagePlusData}
     */
    public ImagePlusData pull() {
        CLIJ2 clij = CLIJ2.getInstance();
        ImagePlus imagePlus = clij.pull(image);
        if (CLIJSettings.getInstance().isAutoCalibrateAfterPulling()) {
            DisplayRangeCalibrationAlgorithm contrastEnhancer = CLIJSettings.getInstance().getContrastEnhancer();
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

    public static CLIJImageData importFrom(Path storageFilePath) {
        return new CLIJImageData(ImagePlusData.importFrom(storageFilePath));
    }
}
