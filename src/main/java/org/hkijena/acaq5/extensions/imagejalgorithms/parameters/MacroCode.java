package org.hkijena.acaq5.extensions.imagejalgorithms.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Encapsulates ImageJ macro code to be detected by the parameter system
 */
public class MacroCode {
    private String code = "";

    /**
     * Creates a new empty code
     */
    public MacroCode() {
    }

    /**
     * Copies the code
     *
     * @param other the original
     */
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
