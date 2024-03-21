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

package org.hkijena.jipipe.extensions.tables.operations.integrating;

import org.hkijena.jipipe.extensions.tables.IntegratingColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Implements calculating the sum
 */
public class MergeToJsonIntegratingColumnOperation implements IntegratingColumnOperation {
    @Override
    public TableColumn apply(TableColumn column) {
        Set<Object> values = new HashSet<>();
        for (int i = 0; i < column.getRows(); i++) {
            if (column.isNumeric())
                values.add(column.getRowAsDouble(i));
            else
                values.add(column.getRowAsString(i));
        }
        if (values.size() > 0) {
            return new StringArrayTableColumn(new String[]{JsonUtils.toJsonString(values)}, column.getLabel());
        } else if (!values.isEmpty()) {
            if (column.isNumeric())
                return new DoubleArrayTableColumn(new double[]{column.getRowAsDouble(0)}, column.getLabel());
            else
                return new StringArrayTableColumn(new String[]{column.getRowAsString(0)}, column.getLabel());
        } else {
            if (column.isNumeric())
                return new DoubleArrayTableColumn(new double[]{0}, column.getLabel());
            else
                return new StringArrayTableColumn(new String[]{""}, column.getLabel());
        }
    }
}
