package org.hkijena.jipipe.extensions.imagej2.io.parameters.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.DoubleList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class FloatArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<float[], DoubleList> {
    @Override
    public DoubleList convertFromModuleToJIPipe(float[] obj) {
        return null;
    }

    @Override
    public float[] convertFromJIPipeToModule(DoubleList obj) {
        float[] result = new float[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            float item = obj.get(i).floatValue();
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<DoubleList> getJIPipeParameterClass() {
        return DoubleList.class;
    }

    @Override
    public Class<float[]> getModuleClass() {
        return float[].class;
    }
}
