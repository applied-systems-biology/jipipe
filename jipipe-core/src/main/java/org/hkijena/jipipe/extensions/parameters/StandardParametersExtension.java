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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeColumMatching;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollectionVisibilities;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.extensions.nodetemplate.JIPipeNodeTemplateParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.api.enums.*;
import org.hkijena.jipipe.extensions.parameters.api.functions.FunctionParameter;
import org.hkijena.jipipe.extensions.parameters.api.functions.FunctionParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.api.functions.StringPatternExtractionFunction;
import org.hkijena.jipipe.extensions.parameters.api.matrix.Matrix2D;
import org.hkijena.jipipe.extensions.parameters.api.matrix.Matrix2DParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.api.scripts.ScriptParameter;
import org.hkijena.jipipe.extensions.parameters.api.scripts.ScriptParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.auth.PasswordParameter;
import org.hkijena.jipipe.extensions.parameters.library.auth.PasswordParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.colors.*;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeAlgorithmIconRefParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeDataInfoRefParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeNodeInfoRefParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeParameterCollectionVisibilitiesParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.FileParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.FilePathParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathListParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.JIPipeAuthorMetadataParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.JIPipeParameterCollectionParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLTextParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.matrix.Matrix2DFloat;
import org.hkijena.jipipe.extensions.parameters.library.pairs.*;
import org.hkijena.jipipe.extensions.parameters.library.patterns.StringPatternExtraction;
import org.hkijena.jipipe.extensions.parameters.library.patterns.StringPatternExtractionParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.primitives.*;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.*;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.*;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRangeParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.extensions.parameters.library.quantities.QuantityParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.ranges.FloatNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.NumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.NumberRangeParameterEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.references.*;
import org.hkijena.jipipe.extensions.parameters.library.roi.*;
import org.hkijena.jipipe.extensions.parameters.library.scripts.ImageJMacro;
import org.hkijena.jipipe.extensions.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.extensions.parameters.library.table.ParameterTable;
import org.hkijena.jipipe.extensions.parameters.library.table.ParameterTableEditorUI;
import org.hkijena.jipipe.extensions.parameters.library.util.LogicalOperation;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.layout.GraphAutoLayout;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.awt.*;
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
    public HTMLText getDescription() {
        return new HTMLText("User interfaces for editing common parameter types");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:parameter-editors";
    }

    @Override
    public String getDependencyVersion() {
        return "1.66.0";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
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
        registerEnumParameterType("graph-wrapper:iteration-mode",
                GraphWrapperAlgorithm.IterationMode.class,
                "Iteration mode",
                "Determines how the wrapped graph is executed.");


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
        registerRangeParameters();
        registerQuantityParameters();

        registerMenuExtension(ParameterTesterJIPipeMenuExtension.class);
        registerMenuExtension(ExpressionTesterJIPipeMenuExtension.class);
    }

    private void registerQuantityParameters() {
        registerParameterType("quantity", Quantity.class, "Quantity", "A value with a unit", QuantityParameterEditorUI.class);
        registerParameterType("optional-quantity", OptionalQuantity.class, "Optional quantity", "A value with a unit");
    }

    private void registerRangeParameters() {
        registerParameterEditor(NumberRangeParameter.class, NumberRangeParameterEditorUI.class);
        registerParameterType("number-range-float",
                FloatNumberRangeParameter.class,
                null,
                null,
                "Number range (float)",
                "A range of numbers",
                null);
        registerParameterType("number-range-integer",
                IntNumberRangeParameter.class,
                null,
                null,
                "Number range (int)",
                "A range of numbers",
                null);
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
        registerParameterGenerator(byte.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(short.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(int.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(long.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(float.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(double.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(Byte.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(Short.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(Integer.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(Long.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(Float.class, new NumberRangeParameterGenerator());
        registerParameterGenerator(Double.class, new NumberRangeParameterGenerator());
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
                NumericFunctionExpression.class,
                NumericFunctionExpression::new,
                t -> new NumericFunctionExpression((NumericFunctionExpression) t),
                "Integer modification",
                "Modifies an integer",
                null);
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
        registerParameterType("optional-annotation-name",
                OptionalAnnotationNameParameter.class,
                null,
                null,
                "Optional annotation name",
                "An optional annotation name",
                null);
        registerParameterType("optional-data-annotation-name",
                OptionalDataAnnotationNameParameter.class,
                null,
                null,
                "Optional data annotation name",
                "An optional data annotation name",
                null);
        registerParameterType("optional-integer-range",
                OptionalIntegerRange.class,
                null,
                null,
                "Optional integer range",
                "An optional range of integers",
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
        registerParameterType("data-display-operation-id-enum",
                DynamicDataDisplayOperationIdEnumParameter.class,
                null,
                null,
                "Data display operation",
                "A selection of data display operations",
                null);
        registerParameterType("data-import-operation-id-enum",
                DynamicDataImportOperationIdEnumParameter.class,
                null,
                null,
                "Data import operation",
                "A selection of data import operations",
                null);
        registerParameterType("dynamic-string-set",
                DynamicStringSetParameter.class,
                "String set selection",
                "A set of strings from which a subset can be selected");

        // Enums
        registerEnumParameterType("color-map",
                ColorMap.class,
                "Color map",
                "Available color maps that convert a scalar to a color");
        registerEnumParameterType("column-matching",
                JIPipeColumMatching.class,
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
                "String query / string pair",
                "A pair of a string query and a string",
                null);
        registerParameterType("string-query-expression:string-query-expression:pair",
                StringQueryExpressionAndStringQueryPairParameter.class,
                StringQueryExpressionAndStringQueryPairParameter.List.class,
                StringQueryExpressionAndStringQueryPairParameter::new,
                r -> new StringQueryExpressionAndStringQueryPairParameter((StringQueryExpressionAndStringQueryPairParameter) r),
                "String query pair",
                "A pair of two string queries",
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
        registerParameterType("expression:expression:pair",
                ExpressionAndExpressionPairParameter.class,
                ExpressionAndExpressionPairParameter.List.class,
                null,
                null,
                "Expression pair",
                "A pair of expressions",
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
        registerParameterType("integer-range:int:pair",
                IntRangeAndIntegerPairParameter.class,
                IntRangeAndIntegerPairParameter.List.class,
                null,
                null,
                "Integer range to integer pair",
                "Describes a range of whole numbers via a string. The string must have following format: " +
                        "[range];[range];... with range being an integer or [from]-[to]. [from] and [to] are inclusive borders. Negative numbers must be " +
                        "encased with (brackets). [from] and [to] can be in inverse order, generating numbers in inverse order. Spaces are ignored. " +
                        "Example: 0-10;5;3-(-1)",
                null);
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
        registerParameterType("optional-data-type",
                OptionalDataInfoRefParameter.class,
                null,
                null,
                "Optional data type",
                "Optional reference to a data type",
                null);
        registerParameterType("algorithm-type",
                JIPipeNodeInfoRef.class,
                JIPipeNodeInfoRef::new,
                r -> new JIPipeNodeInfoRef((JIPipeNodeInfoRef) r),
                "Algorithm type",
                "Reference to an algorithm type",
                JIPipeNodeInfoRefParameterEditorUI.class);
        registerParameterType("optional-node-type",
                OptionalNodeInfoRefParameter.class,
                null,
                null,
                "Optional node type",
                "Optional reference to a node type",
                null);

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
        registerEnumParameterType("data-by-metadata-exporter:mode",
                JIPipeDataByMetadataExporter.Mode.class,
                "Exporter mode",
                "Allows you to choose between automatic or manual name generation.");

        // Node templates
        registerParameterType("node-template",
                JIPipeNodeTemplate.class,
                JIPipeNodeTemplate.List.class,
                null,
                null,
                "Node template",
                "Stores a copy of a node",
                JIPipeNodeTemplateParameterEditorUI.class);
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
        registerParameterType("color-list", ColorListParameter.class, "Color list", "A list of colors");
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
        registerParameterType("html-text",
                HTMLText.class,
                null,
                null,
                "HTML text",
                "A formatted text",
                HTMLTextParameterEditorUI.class);
        registerEnumParameterType("path-io-mode",
                PathIOMode.class,
                "Path I/O mode",
                "If a path should be opened or saved.");
        registerEnumParameterType("path-type",
                PathType.class,
                "Path type",
                "Type of filesystem path.");
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
