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

package org.hkijena.jipipe.extensions.parameters.enums;

import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicStringEnumParameter;

import javax.swing.*;

public class DynamicCategoryEnumParameter extends DynamicStringEnumParameter {

    public DynamicCategoryEnumParameter() {
        super();
    }

    public DynamicCategoryEnumParameter(DynamicStringEnumParameter other) {
        super(other);
    }

    public DynamicCategoryEnumParameter(String value) {
        super(value);
    }

    @Override
    public String renderLabel(Object value) {
        JIPipeNodeTypeCategory category = JIPipeNodeRegistry.getInstance().getRegisteredCategories().getOrDefault("" + value, null);
        if(category != null) {
            return category.getName();
        }
        else {
            return super.renderLabel(value);
        }
    }

    @Override
    public Icon renderIcon(Object value) {
        JIPipeNodeTypeCategory category = JIPipeNodeRegistry.getInstance().getRegisteredCategories().getOrDefault("" + value, null);
        if(category != null) {
            return category.getIcon();
        }
        else {
            return super.renderIcon(value);
        }
    }
}
