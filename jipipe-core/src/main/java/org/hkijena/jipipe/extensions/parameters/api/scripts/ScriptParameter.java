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

package org.hkijena.jipipe.extensions.parameters.api.scripts;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalPathParameter;
import org.scijava.script.ScriptLanguage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * A parameter that contains some kind of script
 */
public abstract class ScriptParameter {
    private String code = "";
    private boolean collapsed = false;
    private OptionalPathParameter externalScriptFile = new OptionalPathParameter(Paths.get(""), false);
    private String externalCodeBuffer;
    private FileTime externalCodeBufferLastUpdate;

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
        this.externalScriptFile = other.externalScriptFile;
    }

    @JsonGetter("code")
    public String getCode() {
        if (externalScriptFile.isEnabled()) {
            try {
                if (externalCodeBuffer == null || !Objects.equals(externalCodeBufferLastUpdate, Files.getLastModifiedTime(externalScriptFile.getContent()))) {
                    externalCodeBuffer = new String(Files.readAllBytes(externalScriptFile.getContent()), StandardCharsets.UTF_8);
                }
                return externalCodeBuffer;
            } catch (Exception e) {
                e.printStackTrace();
                return code;
            }
        }
        return code;
    }

    @JsonSetter("code")
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the code. Supports relative external script paths
     *
     * @param workDirectory the work directory
     * @return the code
     */
    public String getCode(Path workDirectory) {
        if (workDirectory == null)
            return getCode();
        if (externalScriptFile.isEnabled()) {
            Path externalScriptPath = externalScriptFile.getContent();
            if (!externalScriptPath.isAbsolute()) {
                externalScriptPath = workDirectory.resolve(externalScriptPath);
            }
            try {
                if (externalCodeBuffer == null || !Objects.equals(externalCodeBufferLastUpdate, Files.getLastModifiedTime(externalScriptPath))) {
                    externalCodeBuffer = new String(Files.readAllBytes(externalScriptPath), StandardCharsets.UTF_8);
                }
                return externalCodeBuffer;
            } catch (Exception e) {
                e.printStackTrace();
                return code;
            }
        }
        return code;
    }

    /**
     * Converts the external script file reference into a relative path
     *
     * @param workDirectory the current work directory
     */
    public void makeExternalScriptFileRelative(Path workDirectory) {
        if (externalScriptFile.isEnabled() && workDirectory != null && externalScriptFile.getContent() != null) {
            if (externalScriptFile.getContent().isAbsolute() && externalScriptFile.getContent().startsWith(workDirectory)) {
                externalScriptFile.setContent(workDirectory.relativize(externalScriptFile.getContent()));
            }
        }
    }

    /**
     * Returns the MIME type of this script
     *
     * @return the MIME type of this script
     */
    public abstract String getMimeType();

    /**
     * Returns the human-readable name of the language
     *
     * @return the human-readable name of the language
     */
    public abstract String getLanguageName();

    /**
     * Returns the default extension for this script
     * @return the default extension (including the dot)
     */
    public abstract String getExtension();

    @JsonGetter("collapsed")
    public boolean isCollapsed() {
        return collapsed;
    }

    @JsonSetter("collapsed")
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @JsonGetter("external-script-file")
    public OptionalPathParameter getExternalScriptFile() {
        return externalScriptFile;
    }

    @JsonSetter("external-script-file")
    public void setExternalScriptFile(OptionalPathParameter externalScriptFile) {
        this.externalScriptFile = externalScriptFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptParameter that = (ScriptParameter) o;
        return collapsed == that.collapsed && Objects.equals(code, that.code) && Objects.equals(externalScriptFile, that.externalScriptFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, collapsed, externalScriptFile);
    }

    /**
     * The language info. Optional.
     *
     * @return The language info
     */
    public abstract ScriptLanguage getLanguage();
}
