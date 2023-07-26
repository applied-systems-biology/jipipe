/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.library.scripts;

import org.hkijena.jipipe.extensions.parameters.api.scripts.ScriptParameter;
import org.scijava.script.ScriptLanguage;

/**
 * Encapsulates ImageJ macro code to be detected by the parameter system
 */
public class PythonScript extends ScriptParameter {
    public PythonScript() {
        super();
    }

    public PythonScript(PythonScript other) {
        super(other);
    }

    @Override
    public String getMimeType() {
        return "text/x-python";
    }

    @Override
    public String getLanguageName() {
        return "Python script";
    }

    @Override
    public String getExtension() {
        return ".py";
    }

    @Override
    public ScriptLanguage getLanguage() {
        return null;
    }
}
