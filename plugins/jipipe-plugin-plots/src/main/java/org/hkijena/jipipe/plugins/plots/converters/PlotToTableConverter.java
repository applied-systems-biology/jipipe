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

package org.hkijena.jipipe.plugins.plots.converters;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotData;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotDataSeries;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

public class PlotToTableConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return JFreeChartPlotData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ResultsTableData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        JFreeChartPlotData plotData = (JFreeChartPlotData) input;
        ResultsTableData resultsTableData = new ResultsTableData();
        int index = 1;
        for (JFreeChartPlotDataSeries series : plotData.getSeries()) {
            ResultsTableData copy = new ResultsTableData(series);
            copy.setColumnToValue("#Series", StringUtils.orElse(series.getName(), "" + index));
            resultsTableData.addRows(copy);
            ++index;
        }
        return resultsTableData;
    }
}
