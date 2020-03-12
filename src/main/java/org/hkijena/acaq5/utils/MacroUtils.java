package org.hkijena.acaq5.utils;

public class MacroUtils {

    private MacroUtils() {

    }

    /**
     * Returns true if the variable name is valid
     * @param key
     * @return
     */
    public static boolean isValidVariableName(String key) {
       return key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * Escapes a string to be used within macros
     * @param value
     * @return
     */
    public static String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
