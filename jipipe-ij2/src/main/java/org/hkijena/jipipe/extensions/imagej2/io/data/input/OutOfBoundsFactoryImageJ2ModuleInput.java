package org.hkijena.jipipe.extensions.imagej2.io.data.input;

import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.ImageJ2ShapeData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Handling of {@link net.imglib2.outofbounds.OutOfBoundsFactory}
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class OutOfBoundsFactoryImageJ2ModuleInput extends DataSlotModuleInput<OutOfBoundsFactory, ImageJ2OutOfBoundsFactoryData> {
    @Override
    public OutOfBoundsFactory convertJIPipeToModuleData(ImageJ2OutOfBoundsFactoryData obj) {
        return obj.createFactory();
    }

    @Override
    public ImageJ2OutOfBoundsFactoryData convertModuleToJIPipeData(OutOfBoundsFactory obj) {
        for (Class<? extends JIPipeData> dataClass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            if(!ImageJ2OutOfBoundsFactoryData.class.isAssignableFrom(dataClass))
                continue;
            Constructor<? extends JIPipeData> constructor = ConstructorUtils.getMatchingAccessibleConstructor(dataClass, obj.getClass());
            if(constructor != null) {
                try {
                    return (ImageJ2OutOfBoundsFactoryData) constructor.newInstance(obj);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Unknown factory type: " + obj);
    }

    @Override
    public Class<OutOfBoundsFactory> getModuleDataType() {
        return OutOfBoundsFactory.class;
    }

    @Override
    public Class<ImageJ2OutOfBoundsFactoryData> getJIPipeDataType() {
        return ImageJ2OutOfBoundsFactoryData.class;
    }
}
