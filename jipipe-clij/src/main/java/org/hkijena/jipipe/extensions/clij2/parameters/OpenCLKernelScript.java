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

package org.hkijena.jipipe.extensions.clij2.parameters;

import org.hkijena.jipipe.extensions.parameters.api.scripts.ScriptParameter;
import org.scijava.script.ScriptLanguage;

/**
 * A OpenCL kernel script
 */
public class OpenCLKernelScript extends ScriptParameter {

    /**
     * Creates a new instance
     */
    public OpenCLKernelScript() {
    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public OpenCLKernelScript(OpenCLKernelScript other) {
        super(other);
    }

    @Override
    public String getMimeType() {
        return "text/x-opencl-src";
    }

    @Override
    public String getLanguageName() {
        return "OpenCL Kernel";
    }

    @Override
    public String getExtension() {
        return ".cl";
    }

    @Override
    public ScriptLanguage getLanguage() {
        return null;
    }
}
