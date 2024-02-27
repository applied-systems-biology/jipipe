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

import java.util.Objects;

public class JIPipeExpressionParameterVariableInfo {

    public static final JIPipeExpressionParameterVariableInfo ANNOTATIONS_VARIABLE = new JIPipeExpressionParameterVariableInfo("", "<Annotations>",
            "Text annotations are available as variables named after their column names (use Update Cache to find the list of annotations)"
    );
    private final String name;
    private final String description;
    private final String key;

    public JIPipeExpressionParameterVariableInfo(String key, String name, String description) {
        this.name = name;
        this.description = description;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeExpressionParameterVariableInfo that = (JIPipeExpressionParameterVariableInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, key);
    }
}
