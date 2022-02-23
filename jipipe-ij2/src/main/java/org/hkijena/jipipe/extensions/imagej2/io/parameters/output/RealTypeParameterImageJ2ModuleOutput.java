package org.hkijena.jipipe.extensions.imagej2.io.parameters.output;

import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class RealTypeParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<RealType, Double> {
    @Override
    public Double convertFromModuleToJIPipe(RealType obj) {
        return obj.getRealDouble();
    }

    @Override
    public RealType convertFromJIPipeToModule(Double obj) {
        return new DoubleType(obj);
    }

    @Override
    public Class<Double> getJIPipeParameterClass() {
        return Double.class;
    }

    @Override
    public Class<RealType> getModuleClass() {
        return RealType.class;
    }
}
