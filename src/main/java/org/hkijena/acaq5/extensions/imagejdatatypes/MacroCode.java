package org.hkijena.acaq5.extensions.imagejdatatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class MacroCode {
    private String code = "";

    public MacroCode() {
    }

    public MacroCode(MacroCode other) {
        this.code = other.code;
    }

    @JsonGetter("code")
    public String getCode() {
        return code;
    }

    @JsonSetter("code")
    public void setCode(String code) {
        this.code = code;
    }
}
