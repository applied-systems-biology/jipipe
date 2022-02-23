package org.hkijena.jipipe.extensions.imagej2.io.parameters.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.DoubleList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.IntegerList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class IntegerArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<int[], IntegerList> {
    @Override
    public IntegerList convertFromModuleToJIPipe(int[] obj) {
        return null;
    }

    @Override
    public int[] convertFromJIPipeToModule(IntegerList obj) {
        int[] result = new int[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            int item = obj.get(i);
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<IntegerList> getJIPipeParameterClass() {
        return IntegerList.class;
    }

    @Override
    public Class<int[]> getModuleClass() {
        return int[].class;
    }
}
