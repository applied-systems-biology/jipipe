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

package org.hkijena.jipipe.plugins.cellpose.environments.cp2;

import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironmentType;

import java.nio.file.Path;

public class Cellpose2Environment extends PythonEnvironment {
    public Cellpose2Environment() {
    }

    public Cellpose2Environment(PythonEnvironmentType type, JIPipeExpressionParameter arguments, Path executablePath) {
        super(type, arguments, executablePath);
    }

    public Cellpose2Environment(PythonEnvironment other) {
        super(other);
    }
}
