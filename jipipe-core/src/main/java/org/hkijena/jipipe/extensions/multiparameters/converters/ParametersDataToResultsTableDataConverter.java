package org.hkijena.jipipe.extensions.multiparameters.converters;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

public class ParametersDataToResultsTableDataConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return ParametersData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ResultsTableData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        ParametersData parametersData = (ParametersData) input;
        ResultsTableData result = new ResultsTableData();
        result.addRow(parametersData.getParameterData());
        return result;
    }
}
