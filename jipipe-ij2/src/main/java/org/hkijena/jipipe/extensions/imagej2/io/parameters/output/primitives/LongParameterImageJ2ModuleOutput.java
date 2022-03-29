package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class LongParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Long, Long> {
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
