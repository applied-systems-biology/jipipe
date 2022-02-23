package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class ByteParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Byte, Byte> {
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
