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

package org.hkijena.jipipe.extensions.imagej2.io.data.input;

import net.imglib2.algorithm.neighborhood.Shape;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.ImageJ2ShapeData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Handling of {@link Shape}
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ShapeImageJ2ModuleInput extends DataSlotModuleInput<Shape, ImageJ2ShapeData> {
    @Override
    public Shape convertJIPipeToModuleData(ImageJ2ShapeData obj) {
        return obj.createShape();
    }

    @Override
    public ImageJ2ShapeData convertModuleToJIPipeData(Shape obj) {
        for (Class<? extends JIPipeData> dataClass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            if (!ImageJ2ShapeData.class.isAssignableFrom(dataClass))
                continue;
            Constructor<? extends JIPipeData> constructor = ConstructorUtils.getMatchingAccessibleConstructor(dataClass, obj.getClass());
            if (constructor != null) {
                try {
                    return (ImageJ2ShapeData) constructor.newInstance(obj);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Unknown shape type: " + obj);
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
