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

package org.hkijena.jipipe.extensions.expressions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Information about the parameters
 */
public class ParameterInfo {
    private final String name;
    private final String description;
    private final Set<Class<?>> types;

    public ParameterInfo(String name, String description, Class<?>... types) {
        this.name = name;
        this.description = description;
        this.types = new HashSet<>(Arrays.asList(types));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Class<?>> getTypes() {
        return types;
    }
}
