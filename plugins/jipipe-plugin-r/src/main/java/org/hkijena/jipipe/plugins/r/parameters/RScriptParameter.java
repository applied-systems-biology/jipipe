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

package org.hkijena.jipipe.plugins.r.parameters;

import org.hkijena.jipipe.plugins.parameters.api.scripts.ScriptParameter;
import org.scijava.script.ScriptLanguage;

public class RScriptParameter extends ScriptParameter {

    public RScriptParameter() {
    }

    public RScriptParameter(ScriptParameter other) {
        super(other);
    }

    public RScriptParameter(String code) {
        setCode(code);
    }

    @Override
    public String getMimeType() {
        return "text/x-r-script";
    }

    @Override
    public String getLanguageName() {
        return "R script";
    }

    @Override
    public String getExtension() {
        return ".R";
    }

    @Override
    public ScriptLanguage getLanguage() {
        return null;
    }
}
