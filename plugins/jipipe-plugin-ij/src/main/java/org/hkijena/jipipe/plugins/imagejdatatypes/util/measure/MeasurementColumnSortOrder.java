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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.measure;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.plugins.parameters.library.util.SortOrder;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.Comparator;

/**
 * A key-value pair parameter of {@link MeasurementColumn} to {@link SortOrder}
 */
public class MeasurementColumnSortOrder extends PairParameter<MeasurementColumn, SortOrder> {
    /**
     * Creates a new instance
     */
    public MeasurementColumnSortOrder() {
        super(MeasurementColumn.class, SortOrder.class);
        this.setKey(MeasurementColumn.Area);
        this.setValue(SortOrder.Ascending);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MeasurementColumnSortOrder(MeasurementColumnSortOrder other) {
        super(other);
    }

    /**
     * Creates a comparator for {@link ResultsTableData} rows that is equivalent to the column order in this object
     *
     * @param tableData the table
     * @return the comparator
     */
    public Comparator<Integer> getRowComparator(ResultsTableData tableData) {
        return (o1, o2) -> {
            double v1 = tableData.getTable().getValue(getKey().getColumnName(), o1);
            double v2 = tableData.getTable().getValue(getKey().getColumnName(), o2);
            if (getValue() == SortOrder.Ascending)
                return Double.compare(v1, v2);
            else
                return -Double.compare(v1, v2);
        };
    }

    /**
     * A list of {@link MeasurementColumnSortOrder}
     */
    public static class List extends ListParameter<MeasurementColumnSortOrder> {

        /**
         * Creates a new instance
         */
        public List() {
            super(MeasurementColumnSortOrder.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(MeasurementColumnSortOrder.class);
            for (MeasurementColumnSortOrder measurementFilter : other) {
                add(new MeasurementColumnSortOrder(measurementFilter));
            }
        }

        /**
         * Returns the integer value that describes which measurements to extract
         *
         * @return the integer value that describes which measurements to extract
         */
        public int getNativeMeasurementEnumValue() {
            int result = 0;
            for (MeasurementColumnSortOrder measurementFilter : this) {
                result |= measurementFilter.getKey().getNativeValue();
            }
            return result;
        }
    }
}
