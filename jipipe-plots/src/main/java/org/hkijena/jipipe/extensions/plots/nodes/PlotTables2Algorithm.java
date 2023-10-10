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

package org.hkijena.jipipe.extensions.plots.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.utils.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotColumn;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm that creates {@link PlotData} from {@link ResultsTableData}
 * This is an improved version of {@link PlotTables2Algorithm} that is linked directly to a specific plot
 */
public class PlotTables2Algorithm extends JIPipeMergingAlgorithm {

    private final PlotData plotTypeParameters;

    private final JIPipeDataInfo plotType;
    private JIPipeDynamicParameterCollection inputColumns = new JIPipeDynamicParameterCollection(false);
    private StringQueryExpression seriesName = new StringQueryExpression("SUMMARIZE_VARIABLES()");

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public PlotTables2Algorithm(PlotTables2AlgorithmInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "Tables that will be plotted", ResultsTableData.class)
                .addOutputSlot("Output", "Generated plots", info.getPlotDataType().getDataClass())
                .seal()
                .build());
        plotType = info.getPlotDataType();
        plotTypeParameters = (PlotData) ReflectionUtils.newInstance(info.getPlotDataType().getDataClass());
        registerSubParameter(inputColumns);
        updateColumnAssignment();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PlotTables2Algorithm(PlotTables2Algorithm other) {
        super(other);
        this.plotType = other.plotType;
        this.plotTypeParameters = (PlotData) other.plotTypeParameters.duplicate(new JIPipeProgressInfo());
        this.inputColumns = new JIPipeDynamicParameterCollection(other.inputColumns);
        this.seriesName = new StringQueryExpression(other.seriesName);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        PlotMetadata plotMetadata = plotType.getDataClass().getAnnotation(PlotMetadata.class);
        Map<String, PlotColumn> plotColumns = new HashMap<>();
        for (PlotColumn column : plotMetadata.columns()) {
            plotColumns.put(column.name(), column);
        }

        PlotData plot = (PlotData) plotTypeParameters.duplicate(progressInfo);
        int seriesCounter = 0;
        for (int row : dataBatch.getInputRows(getFirstInputSlot())) {

            ResultsTableData inputData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);
            ResultsTableData seriesTable = new ResultsTableData();
            seriesTable.addRows(inputData.getRowCount());

            ExpressionVariables variables = new ExpressionVariables();
            List<JIPipeTextAnnotation> originalAnnotations = getFirstInputSlot().getTextAnnotations(row);
            for (JIPipeTextAnnotation annotation : originalAnnotations) {
                variables.set(annotation.getName(), annotation.getValue());
            }

            // Generate series
            for (Map.Entry<String, JIPipeParameterAccess> entry : inputColumns.getParameters().entrySet()) {
                TableColumnSourceExpressionParameter parameter = entry.getValue().get(TableColumnSourceExpressionParameter.class);
                seriesTable.setColumn(entry.getKey(), parameter.pickOrGenerateColumn(inputData), plotColumns.get(entry.getKey()).isNumeric());
            }

            PlotDataSeries series = new PlotDataSeries(seriesTable.getTable());
            series.setName(seriesName.generate(variables));
            plot.addSeries(series);

            // Increment the series counter
            seriesCounter += 1;
            if (seriesCounter >= plotMetadata.maxSeriesCount()) {
                progressInfo.log("Maximum number of series was reached (maximum is " + plotMetadata.maxSeriesCount() + "!). Creating a new plot.");
                dataBatch.addOutputData(getFirstOutputSlot(), plot, progressInfo);
                plot = (PlotData) plotTypeParameters.duplicate(progressInfo);
                seriesCounter = 0;
            }
        }
        if (!plot.getSeries().isEmpty()) {
            dataBatch.addOutputData(getFirstOutputSlot(), plot, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        report.report(new ParameterValidationReportContext(context, this, "Plot parameters", "plot-parameters"), plotTypeParameters);
    }

    private void updateColumnAssignment() {
        inputColumns.beginModificationBlock();
        inputColumns.clear();
        PlotMetadata plotMetadata = plotType.getDataClass().getAnnotation(PlotMetadata.class);
        for (PlotColumn column : plotMetadata.columns()) {
            JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess();
            parameterAccess.setKey(column.name());
            parameterAccess.setName(column.name());
            parameterAccess.setImportant(true);
            parameterAccess.setFieldClass(TableColumnSourceExpressionParameter.class);
            TableColumnSourceExpressionParameter initialValue = new TableColumnSourceExpressionParameter();
            parameterAccess.set(initialValue);
            parameterAccess.setDescription(column.description() + " " + (column.isNumeric() ? "(Numeric column)" : "(String column)"));
            inputColumns.addParameter(parameterAccess);
        }
        inputColumns.endModificationBlock();
    }

    @JIPipeDocumentation(name = "Plot parameters")
    @JIPipeParameter("plot-parameters")
    public PlotData getPlotTypeParameters() {
        return plotTypeParameters;
    }

    @JIPipeDocumentation(name = "Input columns", description = "Please define which input table columns are copied into the plot. " +
            "To find out which columns are available, run the quick run on input data. You can also generate missing columns.<br/><strong>If you want to select existing columns, we recommend to put the names into double quotes, e.g., <code>\"Sepal.Length\"</code>.</strong>")
    @JIPipeParameter(value = "input-columns")
    public JIPipeDynamicParameterCollection getInputColumns() {
        return inputColumns;
    }

    @JIPipeDocumentation(name = "Series name", description = "Expression that is used to generate the series name")
    @JIPipeParameter("series-name")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public StringQueryExpression getSeriesName() {
        return seriesName;
    }

    @JIPipeParameter("series-name")
    public void setSeriesName(StringQueryExpression seriesName) {
        this.seriesName = seriesName;
    }

}
