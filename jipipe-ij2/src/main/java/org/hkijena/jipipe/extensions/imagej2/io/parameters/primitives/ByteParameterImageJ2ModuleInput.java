package org.hkijena.jipipe.extensions.imagej2.io.parameters.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class ByteParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Byte, Byte> {
    @Override
    public Byte convertFromModuleToJIPipe(Byte obj) {
        return obj;
    }

    @Override
    public Byte convertFromJIPipeToModule(Byte obj) {
        return obj;
    }

    @Override
    public Class<Byte> getJIPipeParameterClass() {
        return Byte.class;
    }

    @Override
    public Class<Byte> getModuleClass() {
        return Byte.class;
    }
}
