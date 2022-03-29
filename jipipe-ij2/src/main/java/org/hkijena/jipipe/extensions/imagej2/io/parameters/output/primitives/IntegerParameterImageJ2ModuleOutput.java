package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class IntegerParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Integer, Integer> {
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
