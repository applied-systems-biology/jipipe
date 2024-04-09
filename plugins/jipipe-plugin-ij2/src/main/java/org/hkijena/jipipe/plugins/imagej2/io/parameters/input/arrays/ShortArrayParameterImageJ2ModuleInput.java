/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagej2.io.parameters.input.arrays;

import org.hkijena.jipipe.plugins.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.plugins.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.IntegerList;
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
