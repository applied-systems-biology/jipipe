package org.hkijena.jipipe.extensions.imagej2.io.data.input;

import net.imagej.Dataset;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

/**
 * Handling of {@link Dataset}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class DatasetImageJ2ModuleInput extends DataSlotModuleInput<Dataset, ImageJ2DatasetData> {

    @Override
    public Dataset convertJIPipeToModuleData(ImageJ2DatasetData obj) {
        return obj.getDataset();
    }

    @Override
    public ImageJ2DatasetData convertModuleToJIPipeData(Dataset obj) {
        return new ImageJ2DatasetData(obj);
    }

    @Override
    public Class<Dataset> getModuleDataType() {
        return Dataset.class;
    }

    @Override
    public Class<ImageJ2DatasetData> getJIPipeDataType() {
        return ImageJ2DatasetData.class;
    }
}
