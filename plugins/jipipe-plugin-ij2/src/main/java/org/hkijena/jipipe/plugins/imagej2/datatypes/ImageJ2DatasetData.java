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

package org.hkijena.jipipe.plugins.imagej2.datatypes;

import ij.ImagePlus;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@SetJIPipeDocumentation(name = "IJ2 Dataset", description = "An ImageJ2 image")
@LabelAsJIPipeHeavyData
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

    public static ImageJ2DatasetData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFile == null) {
            throw new JIPipeValidationRuntimeException(
                    new FileNotFoundException("Unable to find file in location '" + storage + "'"),
                    "Could not find a compatible image file in '" + storage + "'!",
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

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        wrap().exportData(storage, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ImageJ2DatasetData(dataset.duplicate());
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return wrap().createThumbnail(width, height, progressInfo);
    }

    /**
     * Wraps the IJ2 image to an IJ1 image data
     *
     * @return the IJ1 data
     */
    public ImagePlusData wrap() {
        return new ImagePlusData(ImageJFunctions.wrap((ImgPlus) dataset.getImgPlus(), dataset.getName()));
    }

    /**
     * Gets the data set
     *
     * @return the data set
     */
    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return dataset.toString();
    }
}
