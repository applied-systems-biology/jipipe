package org.hkijena.jipipe.extensions.r.parameters;

import org.hkijena.jipipe.extensions.parameters.api.scripts.ScriptParameter;
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
