package org.hkijena.acaq5.extensions.tables.operations.integrating;

import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;

/**
 * Implements calculating the geometric mean
 */
public class StatisticsGeometricMeanIntegratingColumnOperation implements IntegratingColumnOperation {

    private static final GeometricMean geometricMean = new GeometricMean();

    @Override
    public TableColumn run(TableColumn column) {
        double result = geometricMean.evaluate(column.getDataAsDouble(column.getRows()));
        return new DoubleArrayTableColumn(new double[]{result}, column.getLabel());
    }
}
