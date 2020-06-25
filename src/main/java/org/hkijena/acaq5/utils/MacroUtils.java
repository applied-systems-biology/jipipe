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

package org.hkijena.acaq5.utils;

/**
 * Utility functions for macros
 */
public class MacroUtils {

    private MacroUtils() {

    }

    /**
     * Returns true if the variable name is valid
     *
     * @param key parameter name
     * @return if the name is valid
     */
    public static boolean isValidVariableName(String key) {
        return key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * Escapes a string to be used within macros
     *
     * @param value unescaped string
     * @return escaped string
     */
    public static String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
