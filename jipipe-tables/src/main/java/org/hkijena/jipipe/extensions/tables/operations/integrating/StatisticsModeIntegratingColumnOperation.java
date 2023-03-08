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

package org.hkijena.jipipe.extensions.tables.operations.integrating;

import gnu.trove.map.TDoubleIntMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TDoubleIntHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.hkijena.jipipe.extensions.tables.IntegratingColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

/**
 * Implements calculating the mode value (most common value)
 */
public class StatisticsModeIntegratingColumnOperation implements IntegratingColumnOperation {

    @Override
    public TableColumn apply(TableColumn column) {
        TDoubleIntMap map = new TDoubleIntHashMap();
        for (double v : column.getDataAsDouble(column.getRows())) {
            map.adjustOrPutValue(v, 1, 1);
        }
        double maxKey = Double.NaN;
        int maxCount = -1;
        for (double key : map.keys()) {
            int count = map.get(key);
            if(count > maxCount) {
                maxKey = key;
                maxCount = count;
            }
        }
        return new DoubleArrayTableColumn(new double[]{maxKey}, column.getLabel());
    }
}
