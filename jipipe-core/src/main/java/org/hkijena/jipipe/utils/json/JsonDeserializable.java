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

package org.hkijena.jipipe.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Allows the object to be deserialized in-place from JSON data
 */
public interface JsonDeserializable {
    /**
     * Loads JSON data into the current object instance
     *
     * @param node the JSON data
     */
    void fromJson(JsonNode node);
}
