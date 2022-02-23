package org.hkijena.jipipe.extensions.imagej2.io.parameters.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class BooleanParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Boolean, Boolean> {
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
