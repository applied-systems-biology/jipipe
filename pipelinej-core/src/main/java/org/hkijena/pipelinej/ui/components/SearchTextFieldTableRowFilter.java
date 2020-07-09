package org.hkijena.pipelinej.ui.components;

import org.hkijena.pipelinej.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * Filters string entries via a {@link SearchTextField}
 */
public class SearchTextFieldTableRowFilter extends RowFilter<TableModel, Integer> {
    private final SearchTextField searchTextField;

    /**
     * @param searchTextField the search field
     */
    public SearchTextFieldTableRowFilter(SearchTextField searchTextField) {
        this.searchTextField = searchTextField;
    }

    @Override
    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
        for (int i = 0; i < entry.getValueCount(); i++) {
            if (searchTextField.test(StringUtils.orElse(entry.getStringValue(i), ""))) {
                return true;
            }
        }
        return false;
    }
}
