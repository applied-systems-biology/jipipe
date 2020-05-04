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

package org.hkijena.acaq5.ui.plotbuilder_old;

import java.util.ArrayList;
import java.util.List;

/**
 * Column of strings
 */
public class ACAQLegacyStringPlotSeriesColumn extends ACAQLegacyPlotSeriesColumn {

    /**
     * @param seriesDataList       Series data
     * @param defaultGenerator     Generator for default values
     * @param additionalGenerators Additional generators
     */
    @SafeVarargs
    public ACAQLegacyStringPlotSeriesColumn(List<ACAQLegacyPlotSeriesData> seriesDataList, ACAQLegacyPlotSeriesGenerator defaultGenerator, ACAQLegacyPlotSeriesGenerator... additionalGenerators) {
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
