package org.hkijena.jipipe.extensions.r.parameters;

import org.hkijena.jipipe.extensions.parameters.scripts.ScriptParameter;
import org.scijava.script.ScriptLanguage;

public class RScriptParameter extends ScriptParameter {

    public RScriptParameter() {
    }

    public RScriptParameter(ScriptParameter other) {
        super(other);
    }

    @Override
    public String getMimeType() {
        return "text/x-r";
    }

    @Override
    public String getLanguageName() {
        return "R script";
    }

    @Override
    public ScriptLanguage getLanguage() {
        return null;
    }
}
