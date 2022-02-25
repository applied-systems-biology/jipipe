package org.hkijena.jipipe.extensions.imagej2.io.data.input;

import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

/**
 * Handling of {@link Iterable}, assumed to be {@link ImgPlus}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class IterableImageJ2ModuleInput extends DataSlotModuleInput<Iterable, ImageJ2DatasetData> {

    @Override
    public Iterable convertJIPipeToModuleData(ImageJ2DatasetData obj) {
        return obj.getDataset().getImgPlus();
    }

    @Override
    public ImageJ2DatasetData convertModuleToJIPipeData(Iterable obj) {
        if(obj instanceof ImgPlus) {
            return new ImageJ2DatasetData(new DefaultDataset(JIPipe.getInstance().getContext(), (ImgPlus)obj));
        }
        else {
            return new ImageJ2DatasetData(new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus((Img)obj)));
        }
    }

    @Override
    public Class<Iterable> getModuleDataType() {
        return Iterable.class;
    }

    @Override
    public Class<ImageJ2DatasetData> getJIPipeDataType() {
        return ImageJ2DatasetData.class;
    }
}
