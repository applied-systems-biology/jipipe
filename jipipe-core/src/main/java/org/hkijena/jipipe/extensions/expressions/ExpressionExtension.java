package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.expressions.functions.*;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class ExpressionExtension extends JIPipePrepackagedDefaultJavaExtension {
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
    public String getDependencyVersion() {
        return "2021.5";
    }

    @Override
    public void register() {
        registerParameterType("expression",
                DefaultExpressionParameter.class,
                DefaultExpressionParameter.List.class,
                null,
                null,
                "Expression",
                "A mathematical or conditional logic expression",
                DefaultExpressionParameterEditorUI.class);
        registerParameterEditor(DefaultExpressionParameter.class, DefaultExpressionParameterEditorUI.class);
        registerParameterType("table-column-source",
                TableColumnSourceExpressionParameter.class,
                TableColumnSourceExpressionParameter::new,
                p -> new TableColumnSourceExpressionParameter((TableColumnSourceExpressionParameter) p),
                "Column source",
                "Defines a column source",
                DefaultExpressionParameterEditorUI.class);
        registerParameterType("annotation-query-expression",
                AnnotationQueryExpression.class,
                null,
                null,
                "Annotation query expression",
                "An expression that is used to filter annotations",
                DefaultExpressionParameterEditorUI.class);
        registerParameterType("string-query-expression",
                StringQueryExpression.class,
                null,
                null,
                "String query expression",
                "An expression that is used to filter strings",
                DefaultExpressionParameterEditorUI.class);
        registerParameterType("table-cell-value-query-expression",
                TableCellValueQueryExpression.class,
                null,
                null,
                "Table cell value query expression",
                "An expression that tests for table cells",
                DefaultExpressionParameterEditorUI.class);
        registerParameterType("path-query-expression",
                PathQueryExpression.class,
                null,
                null,
                "Path query expression",
                "An expression that is used to filter paths",
                DefaultExpressionParameterEditorUI.class);
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
                DefaultExpressionParameterEditorUI.class);
        registerParameterType("annotation-generator-expression",
                AnnotationGeneratorExpression.class,
                null,
                null,
                "Annotation generator expression",
                "An expression that is used to generate annotations",
                DefaultExpressionParameterEditorUI.class);
        registerParameterType("named-annotation-generator-expression",
                NamedAnnotationGeneratorExpression.class,
                NamedAnnotationGeneratorExpression.List.class,
                null,
                null,
                "Named annotation generator expression",
                "Used to generate annotations",
                null);
        registerExpressionFunction(new ContainsStringPredicateFunction());
        registerExpressionFunction(new EqualsStringPredicateFunction());
        registerExpressionFunction(new GlobStringPredicateFunction());
        registerExpressionFunction(new RegexStringPredicateFunction());
        registerExpressionFunction(new GetVariableFunction());
        registerExpressionFunction(new IfElseFunction());
        registerExpressionFunction(new VariableExistsFunction());
        registerExpressionFunction(new ToNumberFunction());
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
        registerExpressionFunction(new ClampFunction());
        registerExpressionFunction(new GetVariableKeysFunction());
        registerExpressionFunction(new GetVariableValuesFunction());
        registerExpressionFunction(new SummarizeVariablesFunction());
        registerExpressionFunction(new SummarizeMapFunction());
        registerExpressionFunction(new GetVariablesAsMapFunction());
    }
}