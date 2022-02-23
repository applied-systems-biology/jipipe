package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class DoubleParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Double, Double> {
    @Override
    public Double convertFromModuleToJIPipe(Double obj) {
        return obj;
    }

    @Override
    public Double convertFromJIPipeToModule(Double obj) {
        return obj;
    }

    @Override
    public Class<Double> getJIPipeParameterClass() {
        return Double.class;
    }

    @Override
    public Class<Double> getModuleClass() {
        return Double.class;
    }
}
