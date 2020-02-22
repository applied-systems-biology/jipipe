/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.api;

import java.util.*;

/**
 * Report about the validity of an object
 */
public class ACAQValidityReport {
    private List<String> categories = new ArrayList<>();
    private Map<String, Response> data = new HashMap<>();

    public ACAQValidityReport() {

    }

    public List<String> getInvalidResponses() {
        List<String> result = new ArrayList<>();
        for(Map.Entry<String, Response> entry : data.entrySet()) {
            if(entry.getValue() == Response.Invalid)
                result.add(entry.getKey());
        }
        return result;
    }

    public boolean isValid() {
        return data.values().stream().allMatch(r -> r == Response.Valid);
    }

    public List<String> getCategories() {
        return categories;
    }

    public enum Response {
        Valid,
        Invalid
    }
}
