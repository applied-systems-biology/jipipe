package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class ShortParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Short, Short> {
    @Override
    public Short convertFromModuleToJIPipe(Short obj) {
        return obj;
    }

    @Override
    public Short convertFromJIPipeToModule(Short obj) {
        return obj;
    }

    @Override
    public Class<Short> getJIPipeParameterClass() {
        return Short.class;
    }

    @Override
    public Class<Short> getModuleClass() {
        return Short.class;
    }
}