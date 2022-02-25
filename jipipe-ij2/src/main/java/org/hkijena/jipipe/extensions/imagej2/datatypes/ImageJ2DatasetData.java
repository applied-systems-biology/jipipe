package org.hkijena.jipipe.extensions.imagej2.datatypes;

import ij.ImagePlus;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.extensions.imagej2.util.ImageJDataSetDataImageViewerCustomLoader;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@JIPipeDocumentation(name = "IJ2 Dataset", description = "An ImageJ2 image")
@JIPipeHeavyData
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
public class ImageJ2DatasetData implements JIPipeData {

    private final Dataset dataset;

    public ImageJ2DatasetData(Dataset dataset) {
        this.dataset = Objects.requireNonNull(dataset);
    }

    public ImageJ2DatasetData(ImagePlus image) {
        this.dataset = new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus(ImageJFunctions.wrap(image)));
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        wrap().exportData(storage, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ImageJ2DatasetData(dataset.duplicate());
    }

    @Override
    public Component preview(int width, int height) {
        return wrap().preview(width, height);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if (source instanceof JIPipeDataTableDataSource) {
            CachedImagePlusDataViewerWindow window = new CachedImagePlusDataViewerWindow(workbench, (JIPipeDataTableDataSource) source, displayName, true);
            window.setCustomDataLoader(new ImageJDataSetDataImageViewerCustomLoader());
            window.setVisible(true);
            SwingUtilities.invokeLater(window::reloadDisplayedData);
        } else {
            wrap().getDuplicateImage().show();
        }
    }

    /**
     * Wraps the IJ2 image to an IJ1 image data
     * @return the IJ1 data
     */
    public ImagePlusData wrap() {
        return new ImagePlusData(ImageJFunctions.wrap((ImgPlus)dataset.getImgPlus(), dataset.getName()));
    }

    /**
     * Gets the data set
     * @return the data set
     */
    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return dataset.toString();
    }

    public static ImageJ2DatasetData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFile == null) {
            throw new UserFriendlyNullPointerException("Could not find a compatible image file in '" + storage + "'!",
                    "Unable to find file in location '" + storage + "'",
                    "ImagePlusData loading",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        DatasetIOService service = JIPipe.getInstance().getContext().getService(DatasetIOService.class);
        try {
            Dataset dataset = service.open(targetFile.toString());
            return new ImageJ2DatasetData(dataset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
