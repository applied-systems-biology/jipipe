package org.hkijena.jipipe.ui.components.search;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * Filters entries via a {@link SearchTextField} that can contain search keys, but also a filter expression to filter
 * specific columns. This class correctly maps the {@link org.hkijena.jipipe.api.data.JIPipeDataTable} annotations into strings.
 */
public class ExtendedDataTableSearchTextFieldTableRowFilter extends RowFilter<TableModel, Integer> {
    private final SearchTextField searchTextField;

    /**
     * @param searchTextField the search field
     */
    public ExtendedDataTableSearchTextFieldTableRowFilter(SearchTextField searchTextField) {
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
        ExpressionVariables variables = new ExpressionVariables();
        for (int i = 0; i < entry.getValueCount(); i++) {
            variables.set(entry.getModel().getColumnName(i), getTrueStringValue(entry, i));
        }
        try {
            DefaultExpressionParameter expressionParameter = new DefaultExpressionParameter(searchTextField.getText());
            return expressionParameter.test(variables);
        } catch (Exception e) {
            return variables.values().stream().anyMatch(o -> searchTextField.test(StringUtils.nullToEmpty(o)));
        }
    }
}
