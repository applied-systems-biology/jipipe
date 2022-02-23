package org.hkijena.jipipe.extensions.imagej2.io.parameters.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.LongList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class LongArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<long[], LongList> {
    @Override
    public LongList convertFromModuleToJIPipe(long[] obj) {
        return null;
    }

    @Override
    public long[] convertFromJIPipeToModule(LongList obj) {
        long[] result = new long[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            long item = obj.get(i);
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<LongList> getJIPipeParameterClass() {
        return LongList.class;
    }

    @Override
    public Class<long[]> getModuleClass() {
        return long[].class;
    }
}
