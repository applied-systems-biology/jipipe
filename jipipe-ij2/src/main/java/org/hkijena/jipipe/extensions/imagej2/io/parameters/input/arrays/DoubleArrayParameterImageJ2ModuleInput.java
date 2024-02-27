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

package org.hkijena.jipipe.extensions.imagej2.io.parameters.input.arrays;

import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.input.ParameterImageJ2ModuleInput;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.DoubleList;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class DoubleArrayParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<double[], DoubleList> {
    @Override
    public DoubleList convertFromModuleToJIPipe(double[] obj) {
        return null;
    }

    @Override
    public double[] convertFromJIPipeToModule(DoubleList obj) {
        double[] result = new double[obj.size()];
        for (int i = 0; i < obj.size(); i++) {
            Double item = obj.get(i);
            result[i] = item;
        }
        return result;
    }

    @Override
    public Class<DoubleList> getJIPipeParameterClass() {
        return DoubleList.class;
    }

    @Override
    public Class<double[]> getModuleClass() {
        return double[].class;
    }
}
