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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;

import java.lang.reflect.Method;
import java.util.List;

public class OMEAccessorTemplate {
    private final String name;
    private final String description;
    private final Method method;
    private final JIPipeDynamicParameterCollection parameterCollection;
    private final List<String> parameterIds;

    public OMEAccessorTemplate(String name, String description, Method method, JIPipeDynamicParameterCollection parameterCollection, List<String> parameterIds) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.parameterCollection = parameterCollection;
        this.parameterIds = parameterIds;
    }

    public String getName() {
        return name;
    }

    public JIPipeDynamicParameterCollection getParameterCollection() {
        return parameterCollection;
    }

    public List<String> getParameterIds() {
        return parameterIds;
    }

    public String getDescription() {
        return description;
    }

    public Method getMethod() {
        return method;
    }
}
