package org.hkijena.jipipe.extensions.imagej2.io.parameters.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class StringParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<String, String> {
    @Override
    public String convertFromModuleToJIPipe(String obj) {
        return obj;
    }

    @Override
    public String convertFromJIPipeToModule(String obj) {
        return obj;
    }

    @Override
    public Class<String> getJIPipeParameterClass() {
        return String.class;
    }

    @Override
    public Class<String> getModuleClass() {
        return String.class;
    }
}
