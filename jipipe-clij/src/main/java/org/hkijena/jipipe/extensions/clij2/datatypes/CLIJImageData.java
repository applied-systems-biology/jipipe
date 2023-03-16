package org.hkijena.jipipe.extensions.clij2.datatypes;

import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.clij2.CLIJSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;

/**
 * Contains a CLIJ image
 */
@JIPipeDocumentation(name = "GPU image", description = "Image data stored on the GPU utilized by CLIJ")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpeg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@JIPipeHeavyData
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
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        CachedImagePlusDataViewerWindow window = new CachedImagePlusDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName);
        window.setCustomDataLoader(new CLIJImageViewerCustomDataLoader());
        window.setVisible(true);
        SwingUtilities.invokeLater(window::reloadDisplayedData);
    }

    @Override
    public Component preview(int width, int height) {
        ImagePlusData data = pull();
        if (data != null) {
            return data.preview(width, height);
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
        if (CLIJSettings.getInstance().isAutoCalibrateAfterPulling()) {
            CLIJSettings.ContrastEnhancerSettings contrastEnhancer = CLIJSettings.getInstance().getContrastEnhancerSettings();
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
