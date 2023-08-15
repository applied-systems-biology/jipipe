package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.utils.json.JsonUtils;

/**
 * Extend parameter types with this interface to render custom text descriptions instead of JSON serialized string
 */
public interface JIPipeCustomTextDescriptionParameter {
    /**
     * Gets the parameter as custom text description
     * @return the text description
     */
    String getTextDescription();

    /**
     * Get the text description of an object.
     * Adapts to JIPipeCustomTextDescriptionParameter if the object implements it
     * @param obj the object
     * @return the text description or JSON serialized string
     */
    static String getTexDescriptionOf(Object obj) {
        if(obj != null) {
            if (obj instanceof JIPipeCustomTextDescriptionParameter) {
                return ((JIPipeCustomTextDescriptionParameter) obj).getTextDescription();
            } else {
                return JsonUtils.toJsonString(obj);
            }
        }
        else {
            return "[Not set]";
        }
    }
}
