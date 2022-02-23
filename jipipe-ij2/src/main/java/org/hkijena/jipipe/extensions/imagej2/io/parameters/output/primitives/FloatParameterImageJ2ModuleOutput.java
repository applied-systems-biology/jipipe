package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class FloatParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Float, Float> {
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
