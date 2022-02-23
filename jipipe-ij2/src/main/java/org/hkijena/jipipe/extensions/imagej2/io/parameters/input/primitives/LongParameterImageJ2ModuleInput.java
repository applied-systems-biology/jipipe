package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class LongParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Long, Long> {
    @Override
    public Long convertFromModuleToJIPipe(Long obj) {
        return obj;
    }

    @Override
    public Long convertFromJIPipeToModule(Long obj) {
        return obj;
    }

    @Override
    public Class<Long> getJIPipeParameterClass() {
        return Long.class;
    }

    @Override
    public Class<Long> getModuleClass() {
        return Long.class;
    }
}
