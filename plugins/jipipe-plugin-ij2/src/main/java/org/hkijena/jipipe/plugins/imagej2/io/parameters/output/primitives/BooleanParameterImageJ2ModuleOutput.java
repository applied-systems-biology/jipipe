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

package org.hkijena.jipipe.plugins.imagej2.io.parameters.output.primitives;

import org.hkijena.jipipe.plugins.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.plugins.imagej2.io.parameters.output.ParameterImageJ2ModuleOutput;
import org.scijava.plugin.Plugin;

@Plugin(type = ImageJ2ModuleIO.class)
public class BooleanParameterImageJ2ModuleOutput extends ParameterImageJ2ModuleOutput<Boolean, Boolean> {
    @Override
    public Boolean convertFromModuleToJIPipe(Boolean obj) {
        return obj;
    }

    @Override
    public Boolean convertFromJIPipeToModule(Boolean obj) {
        return obj;
    }

    @Override
    public Class<Boolean> getJIPipeParameterClass() {
        return Boolean.class;
    }

    @Override
    public Class<Boolean> getModuleClass() {
        return Boolean.class;
    }
}
