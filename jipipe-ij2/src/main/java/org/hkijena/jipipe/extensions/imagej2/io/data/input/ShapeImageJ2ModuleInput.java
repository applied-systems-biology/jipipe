package org.hkijena.jipipe.extensions.imagej2.io.data.input;

import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.ImageJ2ShapeData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

/**
 * Handling of shapes
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ShapeImageJ2ModuleInput extends DataSlotModuleInput<Shape, ImageJ2ShapeData> {
    @Override
    public Shape convertJIPipeToModuleData(ImageJ2ShapeData obj) {
        return null;
    }

    @Override
    public ImageJ2ShapeData convertModuleToJIPipeData(Shape obj) {
        return null;
    }

    @Override
    public Class<Shape> getModuleDataType() {
        return Shape.class;
    }

    @Override
    public Class<ImageJ2ShapeData> getJIPipeDataType() {
        return ImageJ2ShapeData.class;
    }
}
