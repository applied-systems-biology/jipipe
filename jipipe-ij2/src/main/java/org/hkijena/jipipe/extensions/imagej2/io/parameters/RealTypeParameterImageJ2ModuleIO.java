package org.hkijena.jipipe.extensions.imagej2.io.parameters;

import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class RealTypeParameterImageJ2ModuleIO extends ParameterImageJ2ModuleIO<RealType, Double> {
    @Override
    public Double moduleToJIPipe(RealType obj) {
        return obj.getRealDouble();
    }

    @Override
    public RealType jiPipeToModule(Double obj) {
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
