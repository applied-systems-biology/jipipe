package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class BooleanParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Boolean, Boolean> {
    @Override
    public Boolean convertFromModuleToJIPipe(Boolean obj) {
        return obj;
    }

    @Override
    public Boolean convertFromJIPipeToModule(Boolean obj) {
        return obj;
    }

    @Override
    public Class<Boolean> getJIPipeParameterClass() {
        return Boolean.class;
    }

    @Override
    public Class<Boolean> getModuleClass() {
        return Boolean.class;
    }
}
