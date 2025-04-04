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

package org.hkijena.jipipe.plugins.tables;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.annotation.AnnotationsPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.tables.datatypes.*;
import org.hkijena.jipipe.plugins.tables.display.OpenResultsTableInImageJDataDisplayOperation;
import org.hkijena.jipipe.plugins.tables.display.OpenResultsTableInJIPipeTabDataDisplayOperation;
import org.hkijena.jipipe.plugins.tables.display.TableColumnDataViewer;
import org.hkijena.jipipe.plugins.tables.nodes.*;
import org.hkijena.jipipe.plugins.tables.nodes.annotations.AddAnnotationColumnsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.annotations.AnnotateByTablePropertiesAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.annotations.AnnotateDataWithTableValues;
import org.hkijena.jipipe.plugins.tables.nodes.annotations.ConvertAnnotationTableToAnnotatedTables;
import org.hkijena.jipipe.plugins.tables.nodes.columns.*;
import org.hkijena.jipipe.plugins.tables.nodes.filter.FilterTableRowsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.filter.FilterTablesAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.filter.SliceTableRowsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.merge.*;
import org.hkijena.jipipe.plugins.tables.nodes.rows.AddMissingRowsInSeriesAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.rows.ApplyExpressionPerRowAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.rows.ApplyExpressionPerRowAlgorithm2;
import org.hkijena.jipipe.plugins.tables.nodes.rows.SortTableRowsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.split.SplitTableByColumnsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.split.SplitTableIntoColumnsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.split.SplitTableIntoRowsAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.statistics.DBSCANClusteringAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.statistics.DistanceMeasures;
import org.hkijena.jipipe.plugins.tables.nodes.statistics.FuzzyKMeansClusteringAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.statistics.KMeansClusteringAlgorithm;
import org.hkijena.jipipe.plugins.tables.nodes.transform.*;
import org.hkijena.jipipe.plugins.tables.operations.converting.*;
import org.hkijena.jipipe.plugins.tables.operations.integrating.*;
import org.hkijena.jipipe.plugins.tables.parameters.ResultsTableDataDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.tables.parameters.collections.*;
import org.hkijena.jipipe.plugins.tables.parameters.enums.TableColumnConversionParameter;
import org.hkijena.jipipe.plugins.tables.parameters.enums.TableColumnGeneratorDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.tables.parameters.enums.TableColumnGeneratorParameter;
import org.hkijena.jipipe.plugins.tables.parameters.enums.TableColumnIntegrationParameter;
import org.hkijena.jipipe.plugins.tables.parameters.processors.*;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Standard set of table operations
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class TablesPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:table-operations",
            JIPipe.getJIPipeVersion(),
            "Standard table operations");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(TablesPlugin.class, "org/hkijena/jipipe/plugins/tables");

    public TablesPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_STATISTICS, PluginCategoriesEnumParameter.CATEGORY_DATA_PROCESSING);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, AnnotationsPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Rueden, C. T.; Schindelin, J. & Hiner, M. C. et al. (2017), \"ImageJ2: ImageJ for the next generation of scientific image data\", " +
                "BMC Bioinformatics 18:529");
        result.add("Schneider, C. A.; Rasband, W. S. & Eliceiri, K. W. (2012), \"NIH Image to ImageJ: 25 years of image analysis\", " +
                "Nature methods 9(7): 671-675");
        return result;
    }

    @Override
    public String getName() {
        return "Standard table operations";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Common table operations");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:table-operations";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        // Register the two base column types
        registerDataTypes();

        // Register generating data sources
        registerColumnSources();
        registerMenuExtension(NewTableJIPipeDesktopMenuExtension.class);
        registerColumnOperations();

        registerParameters();
        registerAlgorithms();

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    private void registerColumnSources() {
        registerDatatype("table-column-row-index",
                RowIndexTableColumnData.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"));
        registerDatatype("table-column-zero",
                ZeroTableColumnData.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"));
    }

    private void registerDataTypes() {
        registerDatatype("table-column",
                TableColumnData.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                new OpenResultsTableInImageJDataDisplayOperation(),
                new OpenResultsTableInJIPipeTabDataDisplayOperation());
        registerDatatype("table-column-numeric",
                DoubleArrayTableColumnData.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                new OpenResultsTableInImageJDataDisplayOperation(),
                new OpenResultsTableInJIPipeTabDataDisplayOperation());
        registerDatatype("table-column-string",
                StringArrayTableColumnData.class,
                ResourceUtils.getPluginResource("icons/data-types/table-column.png"),
                new OpenResultsTableInImageJDataDisplayOperation(),
                new OpenResultsTableInJIPipeTabDataDisplayOperation());
        registerDatatypeConversion(new DoubleArrayColumnToTableConverter());
        registerDatatypeConversion(new StringArrayColumnToTableConverter());
        registerDefaultDataTypeViewer(TableColumnData.class, TableColumnDataViewer.class);
    }

    private void registerAlgorithms() {
        registerNodeType("table-add-columns-generate", GenerateColumnAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit-table-insert-column-right.png"));
        registerNodeType("table-remove-columns", RemoveColumnAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit-table-delete-column.png"));
        registerNodeType("table-rename-columns", RenameTableColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("table-rename-columns-2", RenameTableColumns2Algorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("table-rename-columns-to-annotation", RenameTableColumnsToAnnotationAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("table-rename-single-columns", RenameSingleColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("table-integrate-columns", IntegrateColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-convert-columns", ConvertColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/formula.png"));
        registerNodeType("table-apply-expression-per-row", ApplyExpressionPerRowAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-apply-expression-per-row-v2", ApplyExpressionPerRowAlgorithm2.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-split-into-columns", SplitTableIntoColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("table-split-into-rows", SplitTableIntoRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("table-merge-from-columns", MergeColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("table-merge-tables", MergeTableRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("table-merge-columns-supplement", MergeTableColumnsSupplementAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("table-merge-columns-supplement-merge", MergeTableColumnsSupplementMergingAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("table-split-by-columns", SplitTableByColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("table-filter", FilterTableRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("table-filter-2", FilterTablesAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("table-slice-rows", SliceTableRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("table-sort", SortTableRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-sort.png"));
        registerNodeType("table-sort-columns", SortTableColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-sort.png"));
        registerNodeType("table-add-annotation-columns", AddAnnotationColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit-table-insert-column-right.png"));
        registerNodeType("convert-annotation-table-to-annotated-tables", ConvertAnnotationTableToAnnotatedTables.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));
        registerNodeType("modify-tables", ModifyTablesScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("modify-and-merge-tables", ModifyAndMergeTablesScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("tables-from-script", TablesFromScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("tables-from-expression", GenerateTableFromExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("define-tables", DefineTablesAlgorithm.class, UIUtils.getIconURLFromResources("data-types/results-table.png"));
        registerNodeType("table-rename-columns-expression", ModifyTableColumnNamesAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("table-merge-columns", MergeTableColumnsSimpleAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("table-annotate-by-merged-columns", ColumnsToAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("data-types/results-table.png"));
        registerNodeType("annotate-data-with-table-values", AnnotateDataWithTableValues.class, UIUtils.getIconURLFromResources("data-types/results-table.png"));
        registerNodeType("table-column-to-string", ColumnToStringAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-column-to-numeric", ColumnToNumericAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-process-columns", ApplyExpressionToColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-apply-expression-to-table-by-columns", ApplyExpressionToTableByColumnAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-melt", MeltTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-unmelt", UnMeltTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-annotate-with-properties", AnnotateByTablePropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("table-add-missing-rows-in-series", AddMissingRowsInSeriesAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-sort-ascending.png"));
        registerNodeType("table-convert-to-histogram-key-value", ApplyExpressionPerLabelAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-convert-to-histogram", TableToHistogramAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-convert-to-histogram-2", TableToHistogram2Algorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-convert-to-count-column-value-occurrences", TableColumnToCountAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-rows-kmeans-clustering", KMeansClusteringAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-rows-dbscan-clustering", DBSCANClusteringAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("table-rows-fuzzy-kmeans-clustering", FuzzyKMeansClusteringAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        // Parameters
        registerEnumParameterType("table-statistics-distance-measures", DistanceMeasures.class, "Distance measure", "A distance measure (for N-dimensional points)");
        registerEnumParameterType("table-rows-kmeans-clustering:empty-cluster-strategy", KMeansPlusPlusClusterer.EmptyClusterStrategy.class, "K-Means empty cluster strategy", "An empty cluster strategy");
    }

    private void registerParameters() {
        registerEnumParameterType("table-column-content-type",
                ColumnContentType.class,
                "Column content type",
                "Determines if the column is numeric or contains text values");
        registerParameterType("table-column-generator",
                TableColumnGeneratorParameter.class,
                TableColumnGeneratorParameter::new,
                p -> new TableColumnGeneratorParameter((TableColumnGeneratorParameter) p),
                "Column generator",
                "Defines a column generator",
                TableColumnGeneratorDesktopParameterEditorUI.class);
        registerParameterType("results-table",
                ResultsTableData.class,
                ResultsTableDataList.class,
                null,
                null,
                "Results table",
                "A table",
                ResultsTableDataDesktopParameterEditorUI.class);

        // Operators
        registerParameterType("integrating-table-column-operator", TableColumnIntegrationParameter.class, "Column integration operation", "Operation that integrates columns");
        registerParameterType("converting-table-column-operator", TableColumnConversionParameter.class, "Column converting operation", "Operation that converts columns");

        // Processors
        registerParameterType("integrating-table-column-processor",
                IntegratingTableColumnProcessorParameter.class,
                IntegratingTableColumnProcessorParameter::new,
                p -> new IntegratingTableColumnProcessorParameter((IntegratingTableColumnProcessorParameter) p),
                "Column integration processor",
                "Defines a processor that integrates a column",
                null);
        registerParameterType("integrating-table-column-processor-list",
                IntegratingTableColumnProcessorParameterList.class,
                IntegratingTableColumnProcessorParameterList::new,
                p -> new IntegratingTableColumnProcessorParameterList((IntegratingTableColumnProcessorParameterList) p),
                "Column integration processor list",
                "Defines processors that integrate columns",
                null);

        registerParameterType("converting-table-column-processor",
                ConvertingTableColumnProcessorParameter.class,
                ConvertingTableColumnProcessorParameter::new,
                p -> new ConvertingTableColumnProcessorParameter((ConvertingTableColumnProcessorParameter) p),
                "Column integration processor",
                "Defines a processor that apply a function to each cell",
                null);
        registerParameterType("expression-table-column-processor",
                ExpressionTableColumnProcessorParameter.class,
                ExpressionTableColumnProcessorParameterList.class,
                ExpressionTableColumnProcessorParameter::new,
                p -> new ExpressionTableColumnProcessorParameter((ExpressionTableColumnProcessorParameter) p),
                "Column expression processor",
                "Defines a processor that apply an expression function to each cell",
                null);
        registerParameterType("converting-table-column-processor-list",
                ConvertingTableColumnProcessorParameterList.class,
                ConvertingTableColumnProcessorParameterList::new,
                p -> new ConvertingTableColumnProcessorParameterList((ConvertingTableColumnProcessorParameterList) p),
                "Column conversion processor list",
                "Defines processors that apply a function to each cell",
                null);
        registerParameterType("table-column-generator-column-processor",
                TableColumnGeneratorProcessor.class,
                TableColumnGeneratorProcessor::new,
                p -> new TableColumnGeneratorProcessor((TableColumnGeneratorProcessor) p),
                "Column generator processor",
                "Defines a processor that generates a column",
                null);
        registerParameterType("table-column-generator-column-processor-list",
                TableColumnGeneratorProcessorParameterList.class,
                TableColumnGeneratorProcessorParameterList::new,
                p -> new TableColumnGeneratorProcessorParameterList((TableColumnGeneratorProcessorParameterList) p),
                "Column generator processor list",
                "Defines multiple columns to be generated",
                null);
        registerParameterType("table-column-expression-generator-column-processor",
                ExpressionTableColumnGeneratorProcessor.class,
                ExpressionTableColumnGeneratorProcessorParameterList.class,
                null,
                null,
                "Column generator (expression)",
                "Generates a column via an expression",
                null);

        registerEnumParameterType("table-column-row-normalization",
                TableColumnNormalization.class,
                "Column normalization",
                "Operations for normalizing the number of rows in columns");
    }

    private void registerColumnOperations() {
        // Converters
        registerTableColumnOperation("convert-to-numeric",
                new ToNumericColumnOperation(),
                "Convert to numeric",
                "numeric",
                "Converts string columns into numeric columns. Attempts to convert a string into a numeric value. If this fails, zero is returned for this item.");
        registerTableColumnOperation("convert-to-string",
                new ToStringColumnOperation(),
                "Convert to string",
                "string",
                "Converts numeric columns into string columns.");
        registerTableColumnOperation("convert-factorize",
                new FactorizeColumnOperation(),
                "Factorize",
                "factors",
                "Converts each item into a numeric factor. The numeric factor is equal for equal items.");
        registerTableColumnOperation("convert-occurrences",
                new OccurrencesColumnOperation(),
                "Occurrences",
                "occ",
                "Converts each item to the count of equal items within the column.");
        registerTableColumnOperation("convert-sort-ascending",
                new SortAscendingColumnOperation(),
                "Sort ascending",
                "sorta",
                "Sorts the numeric items in ascending order. String values are sorted in lexicographic order.");
        registerTableColumnOperation("convert-sort-descending",
                new SortDescendingColumnOperation(),
                "Sort descending",
                "sortd",
                "Sorts the numeric items in descending order. String values are sorted in lexicographic order.");
        registerTableColumnOperation("convert-remove-nan",
                new RemoveNaNColumnOperation(),
                "Remove NaN",
                "rmnan",
                "Replaces all NaN values by a zero. The same applies to string values.");
        registerTableColumnOperationAndExpressionFunction("convert-absolute",
                new AbsoluteColumnOperation(),
                "Absolute value",
                "abs",
                "Calculates the absolute values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-ceiling",
                new CeilingColumnOperation(),
                "Ceiling",
                "ceil",
                "Rounds the values to the higher integer value. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-floor",
                new FloorColumnOperation(),
                "Floor",
                "floor",
                "Rounds the values to the lower integer value. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-clamp-negative",
                new ClampNegativeColumnOperation(),
                "Set positive to zero",
                "clampn",
                "Sets positives numbers to zero. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-clamp-positive",
                new ClampPositiveColumnOperation(),
                "Set negative to zero",
                "clampp",
                "Sets negative numbers to zero. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-degree-to-radians",
                new DegreeToRadiansColumnOperation(),
                "Degree to radians",
                "deg2rad",
                "Converts the value from degree to radians. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-radians-to-degree",
                new RadiansToDegreeColumnOperation(),
                "Radians to degree",
                "rad2deg",
                "Converts the value from radians to degree. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-exp",
                new ExpColumnOperation(),
                "Exponent (base e)",
                "exp",
                "Calculates e^x for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-ln",
                new LnColumnOperation(),
                "Logarithm (base e)",
                "ln",
                "Calculates ln(x) for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-sqrt",
                new SqrtColumnOperation(),
                "Square root",
                "sqrt",
                "Calculates sqrt(x) for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-square",
                new SquareColumnOperation(),
                "Square",
                "sqr",
                "Calculates x^2 for each value x. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("convert-acos",
                new ArcusCosineColumnOperation(),
                "Arc cosine",
                "acos",
                "Returns the arc cosine of each value; the returned angle is in the range 0.0 through pi");
        registerTableColumnOperationAndExpressionFunction("convert-asin",
                new ArcusSineColumnOperation(),
                "Arc sine",
                "asin",
                "Returns the arc sine of each value; the returned angle is in the range -pi/2 through pi/2");
        registerTableColumnOperationAndExpressionFunction("convert-atan",
                new ArcusSineColumnOperation(),
                "Arc tangent",
                "atan",
                "Returns the arc tangent of each value; the returned angle is in the range -pi/2 through pi/2");
        registerTableColumnOperationAndExpressionFunction("convert-cos",
                new CosineColumnOperation(),
                "Cosine",
                "cos",
                "Returns the cosine of each value");
        registerTableColumnOperationAndExpressionFunction("convert-cosh",
                new HyperbolicCosineColumnOperation(),
                "Hyperbolic cosine",
                "cosh",
                "Returns the hyperbolic cosine of each value");
        registerTableColumnOperationAndExpressionFunction("convert-sinh",
                new HyperbolicSineColumnOperation(),
                "Hyperbolic sine",
                "sinh",
                "Returns the hyperbolic cosine of each value");
        registerTableColumnOperationAndExpressionFunction("convert-tanh",
                new HyperbolicTangentColumnOperation(),
                "Hyperbolic tangent",
                "tanh",
                "Returns the hyperbolic tangent of each value");
        registerTableColumnOperationAndExpressionFunction("convert-log10",
                new Log10ColumnOperation(),
                "Logarithm (base 10)",
                "log10",
                "Returns the logarithm to the base of 10 of each value");
        registerTableColumnOperationAndExpressionFunction("convert-round",
                new RoundColumnOperation(),
                "Round",
                "round",
                "Rounds to the closest integer number");
        registerTableColumnOperationAndExpressionFunction("convert-sign",
                new SignColumnOperation(),
                "Signum",
                "sign",
                "Returns -1 for negative values, 1 for positive values, and zero otherwise");
        registerTableColumnOperationAndExpressionFunction("convert-sin",
                new SineColumnOperation(),
                "Sine",
                "sin",
                "Returns the sine of each value");
        registerTableColumnOperationAndExpressionFunction("convert-tan",
                new TangentColumnOperation(),
                "Tangent",
                "tan",
                "Returns the tangent of each value");

        // Integrating functions
        registerTableColumnOperationAndExpressionFunction("statistics-average",
                new StatisticsAverageSummarizingColumnOperation(),
                "Average",
                "avg",
                "Calculates the average value of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-count",
                new StatisticsCountSummarizingColumnOperation(),
                "Count",
                "count",
                "Outputs the number of input rows.");
        registerTableColumnOperationAndExpressionFunction("statistics-count-non-zero",
                new StatisticsCountNonZeroSummarizingColumnOperation(),
                "Count non-zero",
                "countn0",
                "Counts all values that are not zero. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-cumulative-sum",
                new StatisticsCumulativeSumColumnOperation(),
                "Cumulative sum",
                "cumsum",
                "Calculates the cumulative sum of the rows in their order.");
        registerTableColumnOperationAndExpressionFunction("statistics-geometric-mean",
                new StatisticsGeometricMeanSummarizingColumnOperation(),
                "Geometric mean",
                "geomean",
                "Calculates the geometric mean of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-kurtosis",
                new StatisticsKurtosisSummarizingColumnOperation(),
                "Kurtosis",
                "kurt",
                "Calculates the kurtosis of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-min",
                new StatisticsMinSummarizingColumnOperation(),
                "Minimum",
                "min",
                "Calculates the minimum of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-max",
                new StatisticsMaxSummarizingColumnOperation(),
                "Maximum",
                "max",
                "Calculates the maximum of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-median",
                new StatisticsMedianSummarizingColumnOperation(),
                "Median",
                "median",
                "Calculates the median of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-mode",
                new StatisticsModeSummarizingColumnOperation(),
                "Mode",
                "mode",
                "Calculates the mode of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-skewness",
                new StatisticsSkewnessSummarizingColumnOperation(),
                "Skewness",
                "skewness",
                "Calculates the skewness of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-standard-deviation",
                new StatisticsStandardDeviationSummarizingColumnOperation(),
                "Standard deviation",
                "stdev",
                "Calculates the standard deviation of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-variance",
                new StatisticsVarianceSummarizingColumnOperation(),
                "Variance",
                "var",
                "Calculates the variance of all numeric values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-weighted-sum",
                new StatisticsWeightedSumColumnOperation(),
                "Weighted sum",
                "wsum",
                "Calculates the weighted sum of all values. The weight is the row index (beginning with 1). String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("statistics-sum",
                new SumSummarizingColumnOperation(),
                "Sum",
                "sum",
                "Calculates the sum of all values. String values are converted to numbers or zero.");
        registerTableColumnOperationAndExpressionFunction("integrate-get-first",
                new GetFirstSummarizingColumnOperation(),
                "Get first",
                "first",
                "Gets the first item if there multiple rows");
        registerTableColumnOperationAndExpressionFunction("integrate-get-last",
                new GetLastSummarizingColumnOperation(),
                "Get last",
                "last",
                "Gets the last item if there multiple rows");
        registerTableColumnOperationAndExpressionFunction("integrate-merge",
                new MergeToJsonSummarizingColumnOperation(),
                "Merge",
                "merge",
                "If there are multiple rows, merge them into a JSON string. Otherwise returns the value.");
    }
}

