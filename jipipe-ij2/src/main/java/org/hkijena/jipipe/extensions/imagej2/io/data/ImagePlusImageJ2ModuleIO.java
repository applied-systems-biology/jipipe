package org.hkijena.jipipe.extensions.imagej2.io.data;

import ij.ImagePlus;
import net.imagej.Dataset;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Handling of {@link ij.ImagePlus}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ImagePlusImageJ2ModuleIO extends DataSlotModuleIO<ImagePlus, ImagePlusData> {
    @Override
    public ImagePlus convertJIPipeToModuleData(ImagePlusData obj) {
        return obj.getImage();
    }

    @Override
    public ImagePlusData convertModuleToJIPipeData(ImagePlus obj) {
        return new ImagePlusData(obj);
    }

    @Override
    public Class<ImagePlus> getModuleDataType() {
        return ImagePlus.class;
    }

    @Override
    public Class<ImagePlusData> getJIPipeDataType() {
        return ImagePlusData.class;
    }
}
