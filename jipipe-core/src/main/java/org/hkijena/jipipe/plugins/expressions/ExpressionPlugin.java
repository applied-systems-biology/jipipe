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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.expressions.functions.*;
import org.hkijena.jipipe.plugins.expressions.functions.collections.*;
import org.hkijena.jipipe.plugins.expressions.functions.color.*;
import org.hkijena.jipipe.plugins.expressions.functions.control.*;
import org.hkijena.jipipe.plugins.expressions.functions.convert.*;
import org.hkijena.jipipe.plugins.expressions.functions.datetime.*;
import org.hkijena.jipipe.plugins.expressions.functions.filesystem.*;
import org.hkijena.jipipe.plugins.expressions.functions.functions.DefineUserFunction;
import org.hkijena.jipipe.plugins.expressions.functions.functions.EvaluateUserFunction;
import org.hkijena.jipipe.plugins.expressions.functions.math.*;
import org.hkijena.jipipe.plugins.expressions.functions.quantities.QuantityConvertFunction;
import org.hkijena.jipipe.plugins.expressions.functions.quantities.QuantityGetUnitFunction;
import org.hkijena.jipipe.plugins.expressions.functions.quantities.QuantityGetValueFunction;
import org.hkijena.jipipe.plugins.expressions.functions.scripts.JavaScriptFunction;
import org.hkijena.jipipe.plugins.expressions.functions.scripts.JythonScriptFunction;
import org.hkijena.jipipe.plugins.expressions.functions.statistics.*;
import org.hkijena.jipipe.plugins.expressions.functions.string.*;
import org.hkijena.jipipe.plugins.expressions.functions.util.PrintFunction;
import org.hkijena.jipipe.plugins.expressions.functions.variables.*;
import org.hkijena.jipipe.plugins.expressions.functions.vector.*;
import org.hkijena.jipipe.plugins.expressions.ui.JIPipeExpressionDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class ExpressionPlugin extends JIPipePrepackagedDefaultJavaPlugin {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Expression parameters";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides mathematical expressions as parameters");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:expressions";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerParameterType("expression",
                JIPipeExpressionParameter.class,
                JIPipeExpressionParameter.List.class,
                null,
                null,
                "Expression",
                "A mathematical or conditional logic expression",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterEditor(JIPipeExpressionParameter.class, JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("optional-expression",
                OptionalJIPipeExpressionParameter.class,
                "Optional expression",
                "A mathematical or conditional logic expression");
        registerParameterType("table-column-source",
                TableColumnSourceExpressionParameter.class,
                TableColumnSourceExpressionParameter.List.class,
                null,
                null,
                "Column source",
                "Defines a column source",
                null);
        registerEnumParameterType("table-column-source:type",
                TableColumnSourceExpressionParameter.TableSourceType.class,
                "Column source type",
                "Type of column source");
        registerParameterType("annotation-query-expression",
                AnnotationQueryExpression.class,
                null,
                null,
                "Annotation query expression",
                "An expression that is used to filter annotations",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("data-annotation-query-expression",
                DataAnnotationQueryExpression.class,
                null,
                null,
                "Data annotation query expression",
                "An expression that is used to filter annotations",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("string-query-expression",
                StringQueryExpression.class,
                null,
                null,
                "String query expression",
                "An expression that is used to filter strings",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("table-cell-value-query-expression",
                TableCellValueQueryExpression.class,
                null,
                null,
                "Table cell value query expression",
                "An expression that tests for table cells",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("path-query-expression",
                PathQueryExpression.class,
                null,
                null,
                "Path query expression",
                "An expression that is used to filter paths",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("named-string-query-expression",
                NamedStringQueryExpression.class,
                NamedStringQueryExpression.List.class,
                null,
                null,
                "Named string query expression",
                "Used to query named strings",
                null);
        registerParameterType("string-map-query-expression",
                StringMapQueryExpression.class,
                null,
                null,
                "String map query expression",
                "An expression that is used to query string-string key value pairs",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("annotation-generator-expression",
                AnnotationGeneratorExpression.class,
                null,
                null,
                "Annotation generator expression",
                "An expression that is used to generate annotations",
                JIPipeExpressionDesktopParameterEditorUI.class);
        registerParameterType("named-annotation-generator-expression",
                NamedTextAnnotationGeneratorExpression.class,
                NamedTextAnnotationGeneratorExpression.List.class,
                null,
                null,
                "Named annotation generator expression",
                "Used to generate annotations",
                null);
        registerParameterType("data-row-query-expression",
                DataRowQueryExpression.class,
                "Data row query",
                "");
        registerParameterType("data-export-expression",
                DataExportExpressionParameter.class,
                "Data export expression",
                "Used for exporting data",
                DataExportExpressionParameterEditorUI.class);
        registerExpressionFunction(new ContainsStringPredicateFunction());
        registerExpressionFunction(new EqualsStringPredicateFunction());
        registerExpressionFunction(new GlobStringPredicateFunction());
        registerExpressionFunction(new RegexStringPredicateFunction());
        registerExpressionFunction(new GetVariableFunction());
        registerExpressionFunction(new IfElseFunction());
        registerExpressionFunction(new SwitchCaseFunction());
        registerExpressionFunction(new VariableExistsFunction());
        registerExpressionFunction(new ToNumberFunction());
        registerExpressionFunction(new ToIntegerFunction());
        registerExpressionFunction(new ToStringFunction());
        registerExpressionFunction(new ToBooleanFunction());
        registerExpressionFunction(new CreateArrayFunction());
        registerExpressionFunction(new StringSplitFunction());
        registerExpressionFunction(new StringJoinFunction());
        registerExpressionFunction(new LengthFunction());
        registerExpressionFunction(new GetFirstItemFunction());
        registerExpressionFunction(new GetLastItemFunction());
        registerExpressionFunction(new RemoveDuplicatesFunction());
        registerExpressionFunction(new SortAscendingArrayFunction());
        registerExpressionFunction(new SortDescendingArrayFunction());
        registerExpressionFunction(new InvertFunction());
        registerExpressionFunction(new ExtractRegexMatchesFunction());
        registerExpressionFunction(new CreatePairArrayFunction());
        registerExpressionFunction(new CreateMapFunction());
        registerExpressionFunction(new GetMapKeysFunction());
        registerExpressionFunction(new GetMapValuesFunction());
        registerExpressionFunction(new StringReplaceFunction());
        registerExpressionFunction(new StringRegexReplaceFunction());
        registerExpressionFunction(new ToJsonFunction());
        registerExpressionFunction(new ParseJsonFunction());
        registerExpressionFunction(new PathExistsFunction());
        registerExpressionFunction(new IsDirectoryFunction());
        registerExpressionFunction(new IsFileFunction());
        registerExpressionFunction(new PathCombineFunction());
        registerExpressionFunction(new GetParentDirectoryFunction());
        registerExpressionFunction(new GetPathNameFunction());
        registerExpressionFunction(new GetDateMonthFunction());
        registerExpressionFunction(new GetDateYearFunction());
        registerExpressionFunction(new GetDateDayFunction());
        registerExpressionFunction(new GetTimeHoursFunction());
        registerExpressionFunction(new GetTimeMinutesFunction());
        registerExpressionFunction(new GetTimeSecondsFunction());
        registerExpressionFunction(new GetItemFunction());
        registerExpressionFunction(new CopyNFunction());
        registerExpressionFunction(new EvaluateFunction());
        registerExpressionFunction(new ForEachFunction());
        registerExpressionFunction(new SequenceFunction());
        registerExpressionFunction(new FirstIndexOfFunction());
        registerExpressionFunction(new LastIndexOfFunction());
        registerExpressionFunction(new WhereFunction());
        registerExpressionFunction(new HistogramFunction());
        registerExpressionFunction(new HistogramThresholdHuang());
        registerExpressionFunction(new HistogramThresholdImageJDefault());
        registerExpressionFunction(new HistogramThresholdIntermodes());
        registerExpressionFunction(new HistogramThresholdImageJIsoData());
        registerExpressionFunction(new HistogramThresholdIsoData());
        registerExpressionFunction(new HistogramThresholdLi());
        registerExpressionFunction(new HistogramThresholdMaxEntropy());
        registerExpressionFunction(new HistogramThresholdMean());
        registerExpressionFunction(new HistogramThresholdMinError());
        registerExpressionFunction(new HistogramThresholdMinimum());
        registerExpressionFunction(new HistogramThresholdMoments());
        registerExpressionFunction(new HistogramThresholdOtsu());
        registerExpressionFunction(new HistogramThresholdPercentile());
        registerExpressionFunction(new HistogramThresholdRenyi());
        registerExpressionFunction(new HistogramThresholdShanbhag());
        registerExpressionFunction(new HistogramThresholdTriangle());
        registerExpressionFunction(new HistogramThresholdYen());
        registerExpressionFunction(new AccumulateFunction());
        registerExpressionFunction(new RunFunctionFunction());
        registerExpressionFunction(new PercentileFunction());
        registerExpressionFunction(new StringFirstIndexOfFunction());
        registerExpressionFunction(new StringLastIndexOfFunction());
        registerExpressionFunction(new StringStartsWithPredicateFunction());
        registerExpressionFunction(new StringEndsWithPredicateFunction());
        registerExpressionFunction(new StringToLowerCaseFunction());
        registerExpressionFunction(new StringToUpperCaseFunction());
        registerExpressionFunction(new StringSliceFunction());
        registerExpressionFunction(new StringSliceBeforeFunction());
        registerExpressionFunction(new StringTruncateFunction());
        registerExpressionFunction(new ClampFunction());
        registerExpressionFunction(new GetVariableKeysFunction());
        registerExpressionFunction(new GetVariableValuesFunction());
        registerExpressionFunction(new SummarizeVariablesFunction());
        registerExpressionFunction(new SummarizeMapFunction());
        registerExpressionFunction(new GetVariablesAsMapFunction());
        registerExpressionFunction(new QuantityGetUnitFunction());
        registerExpressionFunction(new QuantityGetValueFunction());
        registerExpressionFunction(new QuantityConvertFunction());
        registerExpressionFunction(new SwitchFunction());
        registerExpressionFunction(new CaseFunction());
        registerExpressionFunction(new PercentageFunction());
        registerExpressionFunction(new SummarizeAnnotationsMapFunction());
        registerExpressionFunction(new RoundToDecimalsFunction());
        registerExpressionFunction(new CreateRGBColorFunction());
        registerExpressionFunction(new CreateHSBColorFunction());
        registerExpressionFunction(new CreateLABColorFunction());
        registerExpressionFunction(new RGBToHexFunction());
        registerExpressionFunction(new HexToRGBFunction());
        registerExpressionFunction(new RGBToLABFunction());
        registerExpressionFunction(new RGBToHSBFunction());
        registerExpressionFunction(new LABToHSBFunction());
        registerExpressionFunction(new LABToRGBFunction());
        registerExpressionFunction(new HSBToLABFunction());
        registerExpressionFunction(new HSBToRGBFunction());
        registerExpressionFunction(new FalseColorsFunction());
        registerExpressionFunction(new IsNullFunction());
        registerExpressionFunction(new PrintFunction());
        registerExpressionFunction(new HasVariableFunction());
        registerExpressionFunction(new SetMissingVariableFunction());
        registerExpressionFunction(new SetVariableFunction());
        registerExpressionFunction(new SetVariableIfFunction());
        registerExpressionFunction(new RandomFunction());
        registerExpressionFunction(new SliceFunction());
        registerExpressionFunction(new ExpressionSequenceFunction());
        registerExpressionFunction(new IfElseExprFunction());
        registerExpressionFunction(new SetVariablesFunction());
        registerExpressionFunction(new DefineUserFunction());
        registerExpressionFunction(new EvaluateUserFunction());
        registerExpressionFunction(new CumulativeTransformArrayFunction());
        registerExpressionFunction(new TransformArrayFunction());
        registerExpressionFunction(new DoubleToStringFunction());
        registerExpressionFunction(new StringFormatFunction());
        registerExpressionFunction(new ToNumber2Function());
        registerExpressionFunction(new VectorAddFunction());
        registerExpressionFunction(new VectorEuclideanNormFunction());
        registerExpressionFunction(new VectorManhattanNormFunction());
        registerExpressionFunction(new VectorMaximumNormFunction());
        registerExpressionFunction(new VectorMultiplyScalarFunction());
        registerExpressionFunction(new VectorNormalizeFunction());
        registerExpressionFunction(new VectorScalarProductFunction());
        registerExpressionFunction(new VectorSubtractFunction());
        registerExpressionFunction(new StringFixFileNameFunction());
        registerExpressionFunction(new IsNaNFunction());
        registerExpressionFunction(new IsFiniteFunction());
        registerExpressionFunction(new IsInfiniteFunction());
        registerExpressionFunction(new JavaScriptFunction());
        registerExpressionFunction(new JythonScriptFunction());
        registerExpressionFunction(new SliceBeforeFunction());
        registerExpressionFunction(new SliceAfterFunction());
        registerExpressionFunction(new GetItemOrDefaultFunction());
        registerExpressionFunction(new StringTrimFunction());
        registerExpressionFunction(new SwitchMapFunction());
        registerExpressionFunction(new RoundToEvenFunction());
        registerExpressionFunction(new RoundToOddFunction());
        registerExpressionFunction(new NaNToNumFunction());
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
