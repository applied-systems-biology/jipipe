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

package org.hkijena.acaq5.utils;

import java.util.Collection;

public class StringUtils {
    private StringUtils() {

    }

    public static String makeUniqueString(String input, Collection<String> existing) {
        if(!existing.contains(input))
            return input;
        int index = 1;
        while(existing.contains(input + " " + index)) {
            ++index;
        }
        return input + " " + index;
    }
}
