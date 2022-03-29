package org.hkijena.jipipe.extensions.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class DoubleParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Double, Double> {
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
