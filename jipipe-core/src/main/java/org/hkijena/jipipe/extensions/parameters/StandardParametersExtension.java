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

package org.hkijena.jipipe.extensions.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeColumnGrouping;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollectionVisibilities;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.colors.ColorDeserializer;
import org.hkijena.jipipe.extensions.parameters.colors.ColorMap;
import org.hkijena.jipipe.extensions.parameters.colors.ColorParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.colors.ColorSerializer;
import org.hkijena.jipipe.extensions.parameters.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.extensions.parameters.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeAlgorithmIconRefParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeDataInfoRefParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeNodeInfoRefParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeParameterCollectionVisibilitiesParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.expressions.*;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.ContainsStringPredicateFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.EqualsStringPredicateFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.GetVariableFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.GlobStringPredicateFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.IfElseFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.RegexStringPredicateFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.functions.VariableExistsFunction;
import org.hkijena.jipipe.extensions.parameters.functions.FunctionParameter;
import org.hkijena.jipipe.extensions.parameters.functions.FunctionParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.functions.StringPatternExtractionFunction;
import org.hkijena.jipipe.extensions.parameters.generators.ByteRangeParameterGenerator;
import org.hkijena.jipipe.extensions.parameters.generators.DoubleRangeParameterGenerator;
import org.hkijena.jipipe.extensions.parameters.generators.FloatRangeParameterGenerator;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRangeParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRangeParameterGenerator;
import org.hkijena.jipipe.extensions.parameters.generators.LongRangeParameterGenerator;
import org.hkijena.jipipe.extensions.parameters.generators.ShortRangeParameterGenerator;
import org.hkijena.jipipe.extensions.parameters.matrix.Matrix2D;
import org.hkijena.jipipe.extensions.parameters.matrix.Matrix2DFloat;
import org.hkijena.jipipe.extensions.parameters.matrix.Matrix2DParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameter;
import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.pairs.DoubleAndDoublePairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.IntegerAndIntegerPairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringOrDoublePairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndSortOrderPairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.patterns.StringPatternExtraction;
import org.hkijena.jipipe.extensions.parameters.patterns.StringPatternExtractionParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.primitives.*;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeAlgorithmIconRef;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeNodeInfoRef;
import org.hkijena.jipipe.extensions.parameters.roi.*;
import org.hkijena.jipipe.extensions.parameters.scripts.ImageJMacro;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.parameters.scripts.ScriptParameter;
import org.hkijena.jipipe.extensions.parameters.scripts.ScriptParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.table.ParameterTable;
import org.hkijena.jipipe.extensions.parameters.table.ParameterTableEditorUI;
import org.hkijena.jipipe.extensions.parameters.util.JIPipeAuthorMetadataParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.util.JIPipeParameterCollectionParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.extensions.parameters.util.SortOrder;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.layout.GraphAutoLayout;
import org.hkijena.jipipe.utils.JsonUtils;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides some standard parameters
 */
@Plugin(type = JIPipeJavaExtension.class, priority = Priority.FIRST)
public class StandardParametersExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Standard parameter editors";
    }

    @Override
    public String getDescription() {
        return "User interfaces for editing common parameter types";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:parameter-editors";
    }

    @Override
    public String getDependencyVersion() {
        return "2020.10";
    }

    @Override
    public void register() {
        // Fallback editor for any parameter collection
        registerParameterEditor(JIPipeParameterCollection.class, JIPipeParameterCollectionParameterEditorUI.class);
        registerParameterType("jipipe:imagej-update-site",
                JIPipeImageJUpdateSiteDependency.class,
                JIPipeImageJUpdateSiteDependency.List.class,
                null,
                null,
                "ImageJ update site",
                "An ImageJ update site",
                null);


        registerPrimitives();
        registerCommonJavaTypes();
        registerJIPipeTypes();
        registerGeneratingParameters();
        registerPairParameters();
        registerEnumParameters();
        registerOptionalParameters();
        registerMiscParameters();
        registerCollectionParameters();
        registerFunctionParameters();
        registerMatrixParameters();
        registerParameterGenerators();
        registerPatternParameters();
        registerScriptParameters();
        registerExpressionParameters();

        registerMenuExtension(ParameterTesterMenuExtension.class);
        registerMenuExtension(ExpressionTesterMenuExtension.class);
    }

    private void registerExpressionParameters() {
        registerParameterType("expression",
                DefaultExpressionParameter.class,
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
    }

    private void registerScriptParameters() {
        // Register types for the editor
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/ijm", "org.scijava.ui.swing.script.highliters.ImageJMacroTokenMaker");
        atmf.putMapping("text/x-python", "org.fife.ui.rsyntaxtextarea.modes.PythonTokenMaker");

        registerParameterEditor(ScriptParameter.class, ScriptParameterEditorUI.class);
        registerParameterType("ij-macro-code",
                ImageJMacro.class,
                null,
                null,
                "ImageJ macro",
                "An ImageJ macro code",
                null);
        registerParameterType("python-code",
                PythonScript.class,
                null,
                null,
                "Python script",
                "A Python script",
                null);
    }

    private void registerPatternParameters() {
        registerParameterType("string-pattern-extraction",
                StringPatternExtraction.class,
                StringPatternExtraction.List.class,
                null,
                null,
                "String pattern extraction",
                "Allows to extract a string from another string",
                StringPatternExtractionParameterEditorUI.class);
    }

    private void registerFunctionParameters() {
        registerParameterEditor(FunctionParameter.class, FunctionParameterEditorUI.class);
        registerParameterType("jipipe-trait:string-pattern-extraction:jipipe-trait:function",
                StringPatternExtractionFunction.class,
                StringPatternExtractionFunction.List.class,
                null,
                null,
                "Annotation pattern extraction function",
                "A function that applies pattern extraction to an annotation",
                null);
    }

    private void registerParameterGenerators() {
        // Register generators
        registerParameterGenerator(byte.class, ByteRangeParameterGenerator.class, "Generate Byte sequence", "Generates numbers between " + Byte.MIN_VALUE + " and " + Byte.MAX_VALUE);
        registerParameterGenerator(short.class, ShortRangeParameterGenerator.class, "Generate Short sequence", "Generates numbers between " + Short.MIN_VALUE + " and " + Short.MAX_VALUE);
        registerParameterGenerator(int.class, IntegerRangeParameterGenerator.class, "Generate Integer sequence", "Generates numbers between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
        registerParameterGenerator(long.class, LongRangeParameterGenerator.class, "Generate Long sequence", "Generates numbers between " + Long.MIN_VALUE + " and " + Long.MAX_VALUE);
        registerParameterGenerator(float.class, FloatRangeParameterGenerator.class, "Generate Float sequence", "Generates floating point numbers (single precision)");
        registerParameterGenerator(double.class, DoubleRangeParameterGenerator.class, "Generate Double sequence", "Generates floating point numbers (double precision)");
        registerParameterGenerator(Byte.class, ByteRangeParameterGenerator.class, "Generate Byte sequence", "Generates numbers between " + Byte.MIN_VALUE + " and " + Byte.MAX_VALUE);
        registerParameterGenerator(Short.class, ShortRangeParameterGenerator.class, "Generate Short sequence", "Generates numbers between " + Short.MIN_VALUE + " and " + Short.MAX_VALUE);
        registerParameterGenerator(Integer.class, IntegerRangeParameterGenerator.class, "Generate Integer sequence", "Generates numbers between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
        registerParameterGenerator(Long.class, LongRangeParameterGenerator.class, "Generate Long sequence", "Generates numbers between " + Long.MIN_VALUE + " and " + Long.MAX_VALUE);
        registerParameterGenerator(Float.class, FloatRangeParameterGenerator.class, "Generate Float sequence", "Generates floating point numbers (single precision)");
        registerParameterGenerator(Double.class, DoubleRangeParameterGenerator.class, "Generate Double sequence", "Generates floating point numbers (double precision)");
    }

    private void registerMatrixParameters() {
        // Matrix parameters
        registerParameterEditor(Matrix2D.class, Matrix2DParameterEditorUI.class);
        registerParameterType("matrix2d-float",
                Matrix2DFloat.class,
                Matrix2DFloat::new,
                p -> new Matrix2DFloat((Matrix2DFloat) p),
                "2D matrix (float)",
                "A matrix containing float numbers",
                null);
    }

    private void registerCollectionParameters() {
        // Collection parameters
        registerParameterEditor(ListParameter.class, ListParameterEditorUI.class);
    }

    private void registerMiscParameters() {
        // Other JIPipe parameters
        registerParameterType("parameter-visibilities",
                JIPipeParameterCollectionVisibilities.class,
                JIPipeParameterCollectionVisibilities::new,
                v -> new JIPipeParameterCollectionVisibilities((JIPipeParameterCollectionVisibilities) v),
                "Parameter visibilities",
                "Determines which parameters are visible to users",
                JIPipeParameterCollectionVisibilitiesParameterEditorUI.class);
        registerParameterType("parameter-table",
                ParameterTable.class,
                ParameterTable::new,
                t -> new ParameterTable((ParameterTable) t),
                "Parameter table",
                "A table that contains parameters",
                ParameterTableEditorUI.class);
        registerParameterType("int-modification",
                IntModificationParameter.class,
                IntModificationParameter::new,
                t -> new IntModificationParameter((IntModificationParameter) t),
                "Integer modification",
                "Modifies an integer",
                IntModificationParameterEditorUI.class);
        registerParameterType("margin",
                Margin.class,
                Margin.List.class,
                null,
                null,
                "Margin",
                "Defines a rectangular area within a region",
                MarginParameterEditorUI.class);
        registerParameterType("anchor",
                Anchor.class,
                null,
                null,
                "Anchor",
                "An anchor to a position within a rectangle",
                AnchorParameterEditorUI.class);
    }

    private void registerOptionalParameters() {
        // Optional parameters
        registerParameterEditor(OptionalParameter.class, OptionalParameterEditorUI.class);
        registerParameterType("optional-boolean",
                OptionalBooleanParameter.class,
                OptionalBooleanParameter::new,
                o -> new OptionalBooleanParameter((OptionalBooleanParameter) o),
                "Optional boolean",
                "An optional boolean value",
                null);
        registerParameterType("optional-byte",
                OptionalByteParameter.class,
                OptionalByteParameter::new,
                o -> new OptionalByteParameter((OptionalByteParameter) o),
                "Optional byte",
                "An optional byte value",
                null);
        registerParameterType("optional-double",
                OptionalDoubleParameter.class,
                OptionalDoubleParameter::new,
                o -> new OptionalDoubleParameter((OptionalDoubleParameter) o),
                "Optional double",
                "An optional double value",
                null);
        registerParameterType("optional-float",
                OptionalFloatParameter.class,
                OptionalFloatParameter::new,
                o -> new OptionalFloatParameter((OptionalFloatParameter) o),
                "Optional float",
                "An optional float value",
                null);
        registerParameterType("optional-integer",
                OptionalIntegerParameter.class,
                OptionalIntegerParameter::new,
                o -> new OptionalIntegerParameter((OptionalIntegerParameter) o),
                "Optional integer",
                "An optional integer value",
                null);
        registerParameterType("optional-long",
                OptionalLongParameter.class,
                OptionalLongParameter::new,
                o -> new OptionalLongParameter((OptionalLongParameter) o),
                "Optional long",
                "An optional long value",
                null);
        registerParameterType("optional-short",
                OptionalShortParameter.class,
                OptionalShortParameter::new,
                o -> new OptionalShortParameter((OptionalShortParameter) o),
                "Optional short",
                "An optional short value",
                null);
        registerParameterType("optional-string",
                OptionalStringParameter.class,
                OptionalStringParameter::new,
                o -> new OptionalStringParameter((OptionalStringParameter) o),
                "Optional string",
                "An optional string value",
                null);
        registerParameterType("optional-color",
                OptionalColorParameter.class,
                OptionalColorParameter::new,
                o -> new OptionalColorParameter((OptionalColorParameter) o),
                "Optional color",
                "An optional color value",
                null);
        registerParameterType("optional-color-map",
                OptionalColorMapParameter.class,
                OptionalColorMapParameter::new,
                o -> new OptionalColorMapParameter((OptionalColorMapParameter) o),
                "Optional color map",
                "An optional color map",
                null);
        registerParameterType("optional-path",
                OptionalPathParameter.class,
                OptionalPathParameter::new,
                o -> new OptionalPathParameter((OptionalPathParameter) o),
                "Optional path",
                "An optional path",
                null);
        registerParameterType("optional-int-modification",
                OptionalIntModificationParameter.class,
                OptionalIntModificationParameter::new,
                o -> new OptionalIntModificationParameter((OptionalIntModificationParameter) o),
                "Optional integer modification",
                "An optional integer modification",
                null);
    }

    private void registerEnumParameters() {
        // Enum-like parameters
        registerParameterEditor(DynamicEnumParameter.class, DynamicEnumParameterEditorUI.class);
        registerParameterEditor(DynamicSetParameter.class, DynamicSetParameterEditorUI.class);
        registerParameterType("string-enum",
                DynamicStringEnumParameter.class,
                DynamicStringEnumParameter::new,
                p -> new DynamicStringEnumParameter((DynamicStringEnumParameter) p),
                "String selection",
                "A selection of available strings",
                null);

        // Enums
        registerEnumParameterType("color-map",
                ColorMap.class,
                "Color map",
                "Available color maps that convert a scalar to a color");
        registerEnumParameterType("column-matching",
                JIPipeColumnGrouping.class,
                "Column matching strategy",
                "Determines how columns for dataset matching are selected");
        registerEnumParameterType("jipipe:ui:graph-editor-view-mode",
                JIPipeGraphViewMode.class,
                "Graph editor view mode",
                "Determines how the graphs are displayed");
        registerEnumParameterType("jipipe:ui:graph-editor-auto-layout",
                GraphAutoLayout.class,
                "Graph auto layout",
                "Determines which method is used to apply graph auto-layout");
        registerEnumParameterType("annotation-merge-strategy",
                JIPipeAnnotationMergeStrategy.class,
                "Annotation merge strategy",
                "Determines how annotations are merged.");
        registerEnumParameterType("logical-operation",
                LogicalOperation.class,
                "Logical operation",
                "Available logical operations");
        registerEnumParameterType("sort-order",
                SortOrder.class,
                "Sort order",
                "Available sort orders");
    }

    private void registerPairParameters() {
        // Pair-like parameters
        registerParameterEditor(PairParameter.class, PairParameterEditorUI.class);
        registerParameterType("string-query-expression:string:pair",
                StringQueryExpressionAndStringPairParameter.class,
                StringQueryExpressionAndStringPairParameter.List.class,
                StringQueryExpressionAndStringPairParameter::new,
                r -> new StringQueryExpressionAndStringPairParameter((StringQueryExpressionAndStringPairParameter) r),
                "String pair",
                "A pair of a string predicate and a string",
                null);
        registerParameterType("integer:integer:pair",
                IntegerAndIntegerPairParameter.class,
                IntegerAndIntegerPairParameter.List.class,
                IntegerAndIntegerPairParameter::new,
                r -> new IntegerAndIntegerPairParameter((IntegerAndIntegerPairParameter) r),
                "Integer pair",
                "A pair of integers",
                null);
        registerParameterType("double:double:pair",
                DoubleAndDoublePairParameter.class,
                DoubleAndDoublePairParameter.List.class,
                null,
                null,
                "Double pair",
                "A pair of 64-bit floating point numbers",
                null);
        registerParameterType("string-query-expression:sort-order:pair",
                StringQueryExpressionAndSortOrderPairParameter.class,
                StringQueryExpressionAndSortOrderPairParameter.List.class,
                StringQueryExpressionAndSortOrderPairParameter::new,
                r -> new StringQueryExpressionAndSortOrderPairParameter((StringQueryExpressionAndSortOrderPairParameter) r),
                "String predicate to sort order",
                "Mapping from a string predicate to a sort order",
                null);
        registerParameterType("string:string:pair",
                StringAndStringPairParameter.class,
                StringAndStringPairParameter.List.class,
                null,
                null,
                "String pair",
                "A pair of strings",
                null);
        registerParameterType("string:string-or-double:pair",
                StringAndStringOrDoublePairParameter.class,
                StringAndStringOrDoublePairParameter.List.class,
                null,
                null,
                "String and String/Double pair",
                "A pair of string and a String/Double",
                null);
    }

    private void registerGeneratingParameters() {
        // Generating parameters
        registerParameterType("integer-range",
                IntegerRange.class,
                IntegerRange::new,
                f -> new IntegerRange((IntegerRange) f),
                "Integer range string",
                "Describes a range of whole numbers via a string. The string must have following format: " +
                        "[range];[range];... with range being an integer or [from]-[to]. [from] and [to] are inclusive borders. Negative numbers must be " +
                        "encased with (brackets). [from] and [to] can be in inverse order, generating numbers in inverse order. Spaces are ignored. " +
                        "Example: 0-10;5;3-(-1)",
                IntegerRangeParameterEditorUI.class);
    }

    private void registerJIPipeTypes() {
        // JIPipe registry reference types
        registerParameterType("data-type",
                JIPipeDataInfoRef.class,
                JIPipeDataInfoRef::new,
                r -> new JIPipeDataInfoRef((JIPipeDataInfoRef) r),
                "Data type",
                "Reference to a data type",
                JIPipeDataInfoRefParameterEditorUI.class);
        registerParameterType("algorithm-type",
                JIPipeNodeInfoRef.class,
                JIPipeNodeInfoRef::new,
                r -> new JIPipeNodeInfoRef((JIPipeNodeInfoRef) r),
                "Algorithm type",
                "Reference to an algorithm type",
                JIPipeNodeInfoRefParameterEditorUI.class);

        // Icon types
        registerParameterType("algorithm-type-icon",
                JIPipeAlgorithmIconRef.class,
                JIPipeAlgorithmIconRef::new,
                r -> new JIPipeAlgorithmIconRef((JIPipeAlgorithmIconRef) r),
                "Algorithm type icon",
                "Reference to an algorithm type icon",
                JIPipeAlgorithmIconRefParameterEditorUI.class);

        // Metadata
        registerParameterType("author",
                JIPipeAuthorMetadata.class,
                JIPipeAuthorMetadata.List.class,
                null,
                null,
                "Author",
                "An author with affiliations",
                JIPipeAuthorMetadataParameterEditorUI.class);
    }

    private void registerCommonJavaTypes() {
        // Register other common Java classes
        registerParameterEditor(Enum.class, EnumParameterEditorUI.class);
        registerParameterType("string", String.class, StringList.class, () -> "", s -> s, "String", "A text value", StringParameterEditorUI.class);
        registerParameterType("password", PasswordParameter.class, null, null, "Password", "A password", PasswordParameterEditorUI.class);
        registerParameterType("path", Path.class, PathList.class, () -> Paths.get(""), p -> p, "Filesystem path", "A path", FilePathParameterEditorUI.class);
        registerParameterEditor(PathList.class, PathListParameterEditorUI.class);
        registerParameterType("file", File.class, () -> new File(""), f -> f, "Filesystem path", "A path", FileParameterEditorUI.class);
        registerParameterType("color", Color.class, () -> Color.WHITE, c -> c, "Color", "A color", ColorParameterEditorUI.class);
        registerParameterType("rectangle", Rectangle.class, RectangleList.class, Rectangle::new, o -> new Rectangle((Rectangle) o), "Rectangle", "A rectangle", RectangleParameterEditorUI.class);
        registerColorJsonSerializer();
        registerRectangleJsonSerializer();
        // Compound types
        registerParameterType("string-or-double",
                StringOrDouble.class,
                null,
                null,
                "String/Double",
                "An object that can either hold a string or double.",
                StringOrDoubleParameterEditorUI.class);
    }

    private void registerPrimitives() {
        // Register boolean
        registerParameterType(new BooleanPrimitiveParameterTypeInfo(), BooleanParameterEditorUI.class);
        registerParameterType(new BooleanParameterTypeInfo(), BooleanParameterEditorUI.class);

        // Register numbers
        registerParameterType(new BytePrimitiveParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new ShortPrimitiveParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new IntPrimitiveParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new LongPrimitiveParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new FloatPrimitiveParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new DoublePrimitiveParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new ByteParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new ShortParameterTypeInfo(), NumberParameterEditorUI.class);
        registerParameterType(new IntParameterTypeInfo(), IntegerList.class, NumberParameterEditorUI.class);
        registerParameterType(new LongParameterTypeInfo(), LongList.class, NumberParameterEditorUI.class);
        registerParameterType(new FloatParameterTypeInfo(), FloatList.class, NumberParameterEditorUI.class);
        registerParameterType(new DoubleParameterTypeInfo(), DoubleList.class, NumberParameterEditorUI.class);
    }

    private void registerColorJsonSerializer() {
        // Serializer for color type
        SimpleModule module = new SimpleModule();
        module.addSerializer(Color.class, new ColorSerializer());
        module.addDeserializer(Color.class, new ColorDeserializer());
        JsonUtils.getObjectMapper().registerModule(module);
    }

    private void registerRectangleJsonSerializer() {
        // Serializer for color type
        SimpleModule module = new SimpleModule();
        module.addSerializer(Rectangle.class, new RectangleSerializer());
        module.addDeserializer(Rectangle.class, new RectangleDeserializer());
        JsonUtils.getObjectMapper().registerModule(module);
    }
}
