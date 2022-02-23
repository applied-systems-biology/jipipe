package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class StringArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<String[], StringList> {
    @Override
    public StringList convertFromModuleToJIPipe(String[] obj) {
        return null;
    }

    @Override
    public String[] convertFromJIPipeToModule(StringList obj) {
        String[] result = new String[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            String item = obj.get(i);
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<StringList> getJIPipeParameterClass() {
        return StringList.class;
    }

    @Override
    public Class<String[]> getModuleClass() {
        return String[].class;
    }
}
