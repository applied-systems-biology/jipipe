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

package org.hkijena.jipipe.desktop.commons.components.search;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * Filters entries via a {@link JIPipeDesktopSearchTextField} that can contain search keys, but also a filter expression to filter
 * specific columns. This class correctly maps the {@link org.hkijena.jipipe.api.data.JIPipeDataTable} annotations into strings.
 */
public class JIPipeDesktopExtendedDataTableSearchTextFieldTableRowFilter extends RowFilter<TableModel, Integer> {
    private final JIPipeDesktopSearchTextField searchTextField;

    /**
     * @param searchTextField the search field
     */
    public JIPipeDesktopExtendedDataTableSearchTextFieldTableRowFilter(JIPipeDesktopSearchTextField searchTextField) {
        this.searchTextField = searchTextField;
    }

    private String getTrueStringValue(Entry<? extends TableModel, ? extends Integer> entry, int index) {
        Object value = entry.getValue(index);
        if (value instanceof JIPipeTextAnnotation) {
            return ((JIPipeTextAnnotation) value).getValue();
        } else {
            return "" + value;
        }
    }

    @Override
    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        for (int i = 0; i < entry.getValueCount(); i++) {
            variables.set(entry.getModel().getColumnName(i), getTrueStringValue(entry, i));
        }
        try {
            JIPipeExpressionParameter expressionParameter = new JIPipeExpressionParameter(searchTextField.getText());
            return expressionParameter.test(variables);
        } catch (Exception e) {
            return variables.values().stream().anyMatch(o -> searchTextField.test(StringUtils.nullToEmpty(o)));
        }
    }
}
