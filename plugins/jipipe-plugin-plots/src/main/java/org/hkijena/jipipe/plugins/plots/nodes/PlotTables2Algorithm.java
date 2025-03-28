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

package org.hkijena.jipipe.plugins.plots.nodes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotColumn;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotData;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotDataSeries;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotMetadata;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm that creates {@link JFreeChartPlotData} from {@link ResultsTableData}
 * This is an improved version of {@link PlotTables2Algorithm} that is linked directly to a specific plot
 */
public class PlotTables2Algorithm extends JIPipeMergingAlgorithm {

    private final JFreeChartPlotData plotTypeParameters;

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
        plotTypeParameters = (JFreeChartPlotData) ReflectionUtils.newInstance(info.getPlotDataType().getDataClass());
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
        this.plotTypeParameters = (JFreeChartPlotData) other.plotTypeParameters.duplicate(new JIPipeProgressInfo());
        this.inputColumns = new JIPipeDynamicParameterCollection(other.inputColumns);
        this.seriesName = new StringQueryExpression(other.seriesName);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JFreeChartPlotMetadata plotMetadata = plotType.getDataClass().getAnnotation(JFreeChartPlotMetadata.class);
        Map<String, JFreeChartPlotColumn> plotColumns = new HashMap<>();
        for (JFreeChartPlotColumn column : plotMetadata.columns()) {
            plotColumns.put(column.name(), column);
        }

        JFreeChartPlotData plot = (JFreeChartPlotData) plotTypeParameters.duplicate(progressInfo);
        int seriesCounter = 0;
        for (int row : iterationStep.getInputRows(getFirstInputSlot())) {

            ResultsTableData inputData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);
            ResultsTableData seriesTable = new ResultsTableData();
            seriesTable.addRows(inputData.getRowCount());

            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
            List<JIPipeTextAnnotation> originalAnnotations = getFirstInputSlot().getTextAnnotations(row);
            for (JIPipeTextAnnotation annotation : originalAnnotations) {
                variables.set(annotation.getName(), annotation.getValue());
            }

            // Generate series
            for (Map.Entry<String, JIPipeParameterAccess> entry : inputColumns.getParameters().entrySet()) {
                TableColumnSourceExpressionParameter parameter = entry.getValue().get(TableColumnSourceExpressionParameter.class);
                seriesTable.setColumn(entry.getKey(), parameter.pickOrGenerateColumn(inputData, new JIPipeExpressionVariablesMap(iterationStep)), plotColumns.get(entry.getKey()).isNumeric());
            }

            JFreeChartPlotDataSeries series = new JFreeChartPlotDataSeries(seriesTable.getTable());
            series.setName(seriesName.generate(variables));
            plot.addSeries(series);

            // Increment the series counter
            seriesCounter += 1;
            if (seriesCounter >= plotMetadata.maxSeriesCount()) {
                progressInfo.log("Maximum number of series was reached (maximum is " + plotMetadata.maxSeriesCount() + "!). Creating a new plot.");
                iterationStep.addOutputData(getFirstOutputSlot(), plot, progressInfo);
                plot = (JFreeChartPlotData) plotTypeParameters.duplicate(progressInfo);
                seriesCounter = 0;
            }
        }
        if (!plot.getSeries().isEmpty()) {
            iterationStep.addOutputData(getFirstOutputSlot(), plot, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Plot parameters", "plot-parameters"), plotTypeParameters);
    }

    private void updateColumnAssignment() {
        inputColumns.beginModificationBlock();
        inputColumns.clear();
        JFreeChartPlotMetadata plotMetadata = plotType.getDataClass().getAnnotation(JFreeChartPlotMetadata.class);
        for (JFreeChartPlotColumn column : plotMetadata.columns()) {
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

    @SetJIPipeDocumentation(name = "Plot parameters")
    @JIPipeParameter("plot-parameters")
    public JFreeChartPlotData getPlotTypeParameters() {
        return plotTypeParameters;
    }

    @SetJIPipeDocumentation(name = "Input columns", description = "Please define which input table columns are copied into the plot. " +
            "To find out which columns are available, run the quick run on input data. You can also generate missing columns.<br/><strong>If you want to select existing columns, we recommend to put the names into double quotes, e.g., <code>\"Sepal.Length\"</code>.</strong>")
    @JIPipeParameter(value = "input-columns")
    public JIPipeDynamicParameterCollection getInputColumns() {
        return inputColumns;
    }

    @SetJIPipeDocumentation(name = "Series name", description = "Expression that is used to generate the series name")
    @JIPipeParameter("series-name")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public StringQueryExpression getSeriesName() {
        return seriesName;
    }

    @JIPipeParameter("series-name")
    public void setSeriesName(StringQueryExpression seriesName) {
        this.seriesName = seriesName;
    }

}
