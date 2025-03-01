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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.plugins.plots.JIPipePlotDataClassFilter;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotColumn;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotData;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotDataSeries;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotMetadata;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.scijava.Priority;

import java.util.*;

/**
 * Algorithm that creates {@link JFreeChartPlotData} from {@link ResultsTableData}
 *
 * @deprecated Instead, the new dynamically generated plotting should be utilized
 */
@SetJIPipeDocumentation(name = "Plot tables", description = "Plots incoming tables. First, set the plot type via a parameter. This " +
        "will then show the available settings for this plot type, and a list of input columns for the plot. " +
        "Please ensure to correctly setup these input columns.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(ResultsTableData.class)
@AddJIPipeOutputSlot(JFreeChartPlotData.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Plot (JFreeChart)")
@LabelAsJIPipeHidden
@Deprecated
public class PlotTablesAlgorithm extends JIPipeMergingAlgorithm {

    private JIPipeDataInfoRef plotType = new JIPipeDataInfoRef();
    private JFreeChartPlotData plotTypeParameters;
    private JIPipeDynamicParameterCollection inputColumns = new JIPipeDynamicParameterCollection(false);
    private StringQueryExpression seriesName = new StringQueryExpression("SUMMARIZE_VARIABLES()");

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public PlotTablesAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "Tables that will be plotted", ResultsTableData.class)
                .addOutputSlot("Output", "Generated plots", JFreeChartPlotData.class)
                .seal()
                .build());
        registerSubParameter(inputColumns);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PlotTablesAlgorithm(PlotTablesAlgorithm other) {
        super(other);
        this.plotType = new JIPipeDataInfoRef(other.plotType);
        if (other.plotTypeParameters != null)
            this.plotTypeParameters = (JFreeChartPlotData) other.plotTypeParameters.duplicate(new JIPipeProgressInfo());
        this.inputColumns = new JIPipeDynamicParameterCollection(other.inputColumns);
        this.seriesName = new StringQueryExpression(other.seriesName);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JFreeChartPlotMetadata plotMetadata = plotType.getInfo().getDataClass().getAnnotation(JFreeChartPlotMetadata.class);
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
        if (getPlotType().getInfo() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Plot type", "plot-type"),
                    "Plot type not set!",
                    "Please choose a plot type"));
        }
        if (plotTypeParameters != null) {
            report.report(new ParameterValidationReportContext(reportContext, this, "Plot parameters", "plot-parameters"), plotTypeParameters);
        }
    }

    @SetJIPipeDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @JIPipeParameter(value = "plot-type", priority = Priority.HIGH, important = true)
    @JIPipeDataParameterSettings(dataBaseClass = JFreeChartPlotData.class, dataClassFilter = JIPipePlotDataClassFilter.class)
    public JIPipeDataInfoRef getPlotType() {
        if (plotType == null) {
            plotType = new JIPipeDataInfoRef();
        }
        return plotType;
    }

    @JIPipeParameter("plot-type")
    public void setPlotType(JIPipeDataInfoRef plotType) {
        this.plotType = plotType;


        updateOutputSlotType();
        updatePlotTypeParameters();
        updateColumnAssignment();
    }

    private void updateColumnAssignment() {
        inputColumns.beginModificationBlock();
        inputColumns.clear();
        if (plotType.getInfo() != null) {
            JFreeChartPlotMetadata plotMetadata = plotType.getInfo().getDataClass().getAnnotation(JFreeChartPlotMetadata.class);
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
        }
        inputColumns.endModificationBlock();
    }

    private void updatePlotTypeParameters() {
        if (plotTypeParameters == null || (plotType.getInfo() != null && !Objects.equals(plotType.getInfo().getDataClass(), plotTypeParameters.getClass()))) {
            if (plotType.getInfo() != null) {
                plotTypeParameters = (JFreeChartPlotData) JIPipe.createData(plotType.getInfo().getDataClass());
                emitParameterStructureChangedEvent();
            }
        } else if (plotType.getInfo() == null) {
            plotTypeParameters = null;
            emitParameterStructureChangedEvent();
        }
    }

    private void updateOutputSlotType() {
        if (plotType.getInfo() != null) {
            getFirstOutputSlot().setAcceptedDataType(plotType.getInfo().getDataClass());
        } else {
            getFirstOutputSlot().setAcceptedDataType(JFreeChartPlotData.class);
        }
        emitNodeSlotsChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Plot parameters")
    @JIPipeParameter("plot-parameters")
    public JFreeChartPlotData getPlotTypeParameters() {
        return plotTypeParameters;
    }

    @SetJIPipeDocumentation(name = "Input columns", description = "Please define which input table columns are copied into the plot. " +
            "To find out which columns are available, run the quick run on input data. You can also generate missing columns.")
    @JIPipeParameter(value = "input-columns", persistence = JIPipeParameterSerializationMode.Object)
    public JIPipeDynamicParameterCollection getInputColumns() {
        return inputColumns;
    }

    @SetJIPipeDocumentation(name = "Series name", description = "Expression that is used to generate the series name")
    @JIPipeParameter("series-name")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    public StringQueryExpression getSeriesName() {
        return seriesName;
    }

    @JIPipeParameter("series-name")
    public void setSeriesName(StringQueryExpression seriesName) {
        this.seriesName = seriesName;
    }

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
