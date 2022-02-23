package org.hkijena.jipipe.extensions.imagej2.io.data;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
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
 * Handling of {@link Interval}, assumed to be {@link ImgPlus}
 * It is assumed that these refer to images (conversion to {@link org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class IntervalImageJ2ModuleIO extends DataSlotModuleIO<Interval, ImageJ2DatasetData> {

    @Override
    public Interval convertJIPipeToModuleData(ImageJ2DatasetData obj) {
        return obj.getDataset().getImgPlus();
    }

    @Override
    public ImageJ2DatasetData convertModuleToJIPipeData(Interval obj) {
        if(obj instanceof ImgPlus) {
            return new ImageJ2DatasetData(new DefaultDataset(JIPipe.getInstance().getContext(), (ImgPlus)obj));
        }
        else {
            return new ImageJ2DatasetData(new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus((Img)obj)));
        }
    }

    @Override
    public Class<Interval> getModuleDataType() {
        return Interval.class;
    }

    @Override
    public Class<ImageJ2DatasetData> getJIPipeDataType() {
        return ImageJ2DatasetData.class;
    }
}
