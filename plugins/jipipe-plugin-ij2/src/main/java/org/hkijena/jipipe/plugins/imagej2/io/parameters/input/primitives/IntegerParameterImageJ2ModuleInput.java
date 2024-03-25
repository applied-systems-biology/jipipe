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

package org.hkijena.jipipe.plugins.imagej2.io.parameters.input.primitives;

import org.hkijena.jipipe.plugins.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.plugins.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class IntegerParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<Integer, Integer> {
    @Override
    public Integer convertFromModuleToJIPipe(Integer obj) {
        return obj;
    }

    @Override
    public Integer convertFromJIPipeToModule(Integer obj) {
        return obj;
    }

    @Override
    public Class<Integer> getJIPipeParameterClass() {
        return Integer.class;
    }

    @Override
    public Class<Integer> getModuleClass() {
        return Integer.class;
    }
}
