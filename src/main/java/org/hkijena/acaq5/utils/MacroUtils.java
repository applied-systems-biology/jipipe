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
