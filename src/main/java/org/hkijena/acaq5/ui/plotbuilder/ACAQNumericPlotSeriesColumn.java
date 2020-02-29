/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder;

import java.util.ArrayList;
import java.util.List;

public class ACAQNumericPlotSeriesColumn extends ACAQPlotSeriesColumn<Double> {
    @SafeVarargs
    public ACAQNumericPlotSeriesColumn(List<ACAQPlotSeriesData> seriesDataList, ACAQPlotSeriesGenerator<Double> defaultGenerator, ACAQPlotSeriesGenerator<Double>... additionalGenerators) {
        super(seriesDataList, defaultGenerator, additionalGenerators);
    }

    @Override
    protected List<Double> getValuesFromTable() {
        List<Double> result = new ArrayList<>(getSeriesData().getSize());
        for (Object value : getSeriesData().getData()) {
            if (value == null)
                continue;
            if (value instanceof Number) {
                result.add(((Number) value).doubleValue());
            } else {
                try {
                    result.add(Double.parseDouble("" + value));
                } catch (NumberFormatException e) {
                    result.add(0.0);
                }
            }
        }
        return result;
    }
}
