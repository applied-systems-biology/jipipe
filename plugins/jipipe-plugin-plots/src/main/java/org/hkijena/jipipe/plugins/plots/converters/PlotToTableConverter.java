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
import org.hkijena.jipipe.plugins.plots.datatypes.PlotData;
import org.hkijena.jipipe.plugins.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

public class PlotToTableConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return PlotData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ResultsTableData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        PlotData plotData = (PlotData) input;
        ResultsTableData resultsTableData = new ResultsTableData();
        int index = 1;
        for (PlotDataSeries series : plotData.getSeries()) {
            ResultsTableData copy = new ResultsTableData(series);
            copy.setColumnToValue("#Series", StringUtils.orElse(series.getName(), "" + index));
            resultsTableData.addRows(copy);
            ++index;
        }
        return resultsTableData;
    }
}
