package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class FloatParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Float, Float> {
    @Override
    public Float convertFromModuleToJIPipe(Float obj) {
        return obj;
    }

    @Override
    public Float convertFromJIPipeToModule(Float obj) {
        return obj;
    }

    @Override
    public Class<Float> getJIPipeParameterClass() {
        return Float.class;
    }

    @Override
    public Class<Float> getModuleClass() {
        return Float.class;
    }
}
