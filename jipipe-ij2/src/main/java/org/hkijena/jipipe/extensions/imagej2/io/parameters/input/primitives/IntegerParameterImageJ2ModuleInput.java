package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class IntegerParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Integer, Integer> {
    @Override
    public Integer convertFromModuleToJIPipe(Integer obj) {
        return obj;
    }

    @Override
    public Integer convertFromJIPipeToModule(Integer obj) {
        return obj;
    }

    @Override
    public Class<Integer> getJIPipeParameterClass() {
        return Integer.class;
    }

    @Override
    public Class<Integer> getModuleClass() {
        return Integer.class;
    }
}
