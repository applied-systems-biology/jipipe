/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Column of strings
 */
public class ACAQStringPlotSeriesColumn extends ACAQPlotSeriesColumn<String> {

    /**
     * @param seriesDataList       Series data
     * @param defaultGenerator     Generator for default values
     * @param additionalGenerators Additional generators
     */
    @SafeVarargs
    public ACAQStringPlotSeriesColumn(List<ACAQPlotSeriesData> seriesDataList, ACAQPlotSeriesGenerator<String> defaultGenerator, ACAQPlotSeriesGenerator<String>... additionalGenerators) {
        super(seriesDataList, defaultGenerator, additionalGenerators);
    }

    @Override
    protected List<String> getValuesFromTable() {
        List<String> result = new ArrayList<>(getSeriesData().getSize());
        for (Object value : getSeriesData().getData()) {
            result.add("" + value);
        }
        return result;
    }
}
