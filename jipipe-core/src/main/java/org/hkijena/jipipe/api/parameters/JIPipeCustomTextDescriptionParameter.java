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

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.utils.json.JsonUtils;

/**
 * Extend parameter types with this interface to render custom text descriptions instead of JSON serialized string
 */
public interface JIPipeCustomTextDescriptionParameter {
    /**
     * Get the text description of an object.
     * Adapts to JIPipeCustomTextDescriptionParameter if the object implements it
     *
     * @param obj the object
     * @return the text description or JSON serialized string
     */
    static String getTextDescriptionOf(Object obj) {
        if (obj != null) {
            if (obj instanceof JIPipeCustomTextDescriptionParameter) {
                return ((JIPipeCustomTextDescriptionParameter) obj).getTextDescription();
            } else {
                return JsonUtils.toJsonString(obj);
            }
        } else {
            return "[Not set]";
        }
    }

    /**
     * Gets the parameter as custom text description
     *
     * @return the text description
     */
    String getTextDescription();
}
