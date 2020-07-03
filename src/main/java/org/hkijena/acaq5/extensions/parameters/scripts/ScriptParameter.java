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

package org.hkijena.acaq5.extensions.parameters.scripts;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.scijava.script.ScriptLanguage;

import java.util.Objects;

/**
 * A parameter that contains some kind of script
 */
public abstract class ScriptParameter {
    private String code = "";
    private boolean collapsed = false;

    /**
     * Creates a new empty code
     */
    public ScriptParameter() {
    }

    /**
     * Copies the code
     *
     * @param other the original
     */
    public ScriptParameter(ScriptParameter other) {
        this.code = other.code;
        this.collapsed = other.collapsed;
    }

    @JsonGetter("code")
    public String getCode() {
        return code;
    }

    @JsonSetter("code")
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the MIME type of this script
     * @return the MIME type of this script
     */
    public abstract String getMimeType();

    /**
     * Returns the human-readable name of the language
     * @return the human-readable name of the language
     */
    public abstract String getLanguageName();

    @JsonGetter("collapsed")
    public boolean isCollapsed() {
        return collapsed;
    }

    @JsonSetter("collapsed")
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptParameter that = (ScriptParameter) o;
        return collapsed == that.collapsed &&
                Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, collapsed);
    }

    /**
     * The language info. Optional.
     * @return The language info
     */
    public abstract ScriptLanguage getLanguage();
}
