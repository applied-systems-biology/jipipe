package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class StringParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<String, String> {
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
