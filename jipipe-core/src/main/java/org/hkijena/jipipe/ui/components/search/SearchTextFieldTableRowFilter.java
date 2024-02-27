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

package org.hkijena.jipipe.ui.components.search;

import org.hkijena.jipipe.utils.StringUtils;

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
