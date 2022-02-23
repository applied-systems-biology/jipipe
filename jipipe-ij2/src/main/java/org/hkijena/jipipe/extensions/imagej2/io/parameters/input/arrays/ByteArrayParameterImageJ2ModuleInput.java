package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.IntegerList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class ByteArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<byte[], IntegerList> {
    @Override
    public IntegerList convertFromModuleToJIPipe(byte[] obj) {
        return null;
    }

    @Override
    public byte[] convertFromJIPipeToModule(IntegerList obj) {
        byte[] result = new byte[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            byte item = obj.get(i).byteValue();
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<IntegerList> getJIPipeParameterClass() {
        return IntegerList.class;
    }

    @Override
    public Class<byte[]> getModuleClass() {
        return byte[].class;
    }
}
