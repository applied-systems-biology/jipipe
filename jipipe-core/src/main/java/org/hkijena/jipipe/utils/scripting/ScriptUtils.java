/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils.scripting;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.plugins.strings.StringData;

/**
 * Utility functions for macros
 */
public class ScriptUtils {

    public static final String SLOT_SCRIPT_NAME = "Script";

    private ScriptUtils() {

    }

    /**
     * Makes a string macro-compatible
     *
     * @param string the string
     * @return formatted string
     */
    public static String makeMacroCompatible(String string) {
        if (string.isEmpty())
            return "_";
        if (Character.isDigit(string.charAt(0)))
            string = "_" + string;
        return string.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Returns true if the variable name is valid
     *
     * @param key parameter name
     * @return if the name is valid
     */
    public static boolean isValidVariableName(String key) {
        return !key.isEmpty() && key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * Escapes a string to be used within macros
     * Will not add quotes around the string
     *
     * @param value unescaped string
     * @return escaped string
     */
    public static String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Ensures that an input slot 'Script' is present in the node and has the provided script class. Should only be run within the constructor.
     * Not effective if the slot configuration is not of the type {@link org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration}.
     * @param scriptClass the script class
     */
    public static void ensureScriptSlot(JIPipeGraphNode node, Class<? extends StringData> scriptClass) {
        if(node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration slotConfiguration) {
            JIPipeDataSlotInfo existingSlot = slotConfiguration.getInputSlots().get(SLOT_SCRIPT_NAME);
            if(existingSlot != null) {
                if(existingSlot.getDataClass() != scriptClass) {
                    existingSlot.setDataClass(scriptClass);
                    existingSlot.setUserModifiable(false);
                    existingSlot.setOptional(false);
                    existingSlot.setRole(JIPipeDataSlotRole.Parameters);
                }
            }
            else {
                slotConfiguration.addSlot(JIPipeDataSlotInfo.builder().dataClass(scriptClass).slotType(JIPipeSlotType.Input).name(SLOT_SCRIPT_NAME).userModifiable(false).build(),
                        false);
            }
        }
    }
}
