package org.hkijena.jipipe.extensions.plots.converters;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

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
    public JIPipeData convert(JIPipeData input) {
        PlotData plotData = (PlotData) input;
        ResultsTableData resultsTableData = new ResultsTableData();
        for (PlotDataSeries series : plotData.getSeries()) {
            ResultsTableData copy = new ResultsTableData(series);
            copy.setColumnToValue("#Series", series.getName());
            resultsTableData.mergeWith(copy);
        }
        return resultsTableData;
    }
}
