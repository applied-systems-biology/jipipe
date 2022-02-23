package org.hkijena.jipipe.extensions.imagej2.io.data;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Handling of {@link Dataset}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class DatasetImageJ2ModuleIO extends DataSlotModuleIO<Dataset, ImageJ2DatasetData> {

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
