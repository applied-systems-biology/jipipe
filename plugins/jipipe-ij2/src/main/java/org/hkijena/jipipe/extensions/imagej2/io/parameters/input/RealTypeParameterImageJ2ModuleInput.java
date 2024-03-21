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

package org.hkijena.jipipe.extensions.imagej2.io.parameters.input;

import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class RealTypeParameterImageJ2ModuleInput extends ParameterImageJ2ModuleInput<RealType, Double> {
    @Override
    public Double convertFromModuleToJIPipe(RealType obj) {
        return obj.getRealDouble();
    }

    @Override
    public RealType convertFromJIPipeToModule(Double obj) {
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
