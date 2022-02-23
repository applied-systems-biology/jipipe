package org.hkijena.jipipe.extensions.imagej2.io.parameters.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.IntegerList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class ShortArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<short[], IntegerList> {
    @Override
    public IntegerList convertFromModuleToJIPipe(short[] obj) {
        return null;
    }

    @Override
    public short[] convertFromJIPipeToModule(IntegerList obj) {
        short[] result = new short[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            short item = obj.get(i).shortValue();
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<IntegerList> getJIPipeParameterClass() {
        return IntegerList.class;
    }

    @Override
    public Class<short[]> getModuleClass() {
        return short[].class;
    }
}
