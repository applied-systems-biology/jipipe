package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class TableColumnSourceExpressionParameterList extends JIPipeListParameter<TableColumnSourceExpressionParameter> {
    public TableColumnSourceExpressionParameterList() {
        super(TableColumnSourceExpressionParameter.class);
    }

    public TableColumnSourceExpressionParameterList(TableColumnSourceExpressionParameterList other) {
        super(TableColumnSourceExpressionParameter.class);
        for (TableColumnSourceExpressionParameter parameter : other) {
            add(new TableColumnSourceExpressionParameter(parameter));
        }
    }
}
