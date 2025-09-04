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

package org.hkijena.jipipe.plugins.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.OptionalJIPipeAuthorMetadata;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollectionVisibilities;
import org.hkijena.jipipe.api.project.JIPipeProjectDirectories;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceDesktopParameterEditorUI;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceParameter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.layout.JIPipepGraphAutoLayoutMethod;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.plugins.nodetemplate.JIPipeNodeTemplateDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.*;
import org.hkijena.jipipe.plugins.parameters.api.functions.FunctionDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.functions.FunctionParameter;
import org.hkijena.jipipe.plugins.parameters.api.functions.StringPatternExtractionFunction;
import org.hkijena.jipipe.plugins.parameters.api.matrix.Matrix2D;
import org.hkijena.jipipe.plugins.parameters.api.matrix.Matrix2DDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.plugins.parameters.api.scripts.ScriptDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.scripts.ScriptParameter;
import org.hkijena.jipipe.plugins.parameters.library.auth.PasswordDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.auth.PasswordParameter;
import org.hkijena.jipipe.plugins.parameters.library.collections.DesktopParameterCollectionListEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.colors.*;
import org.hkijena.jipipe.plugins.parameters.library.editors.JIPipeParameterCollectionVisibilitiesDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.*;
import org.hkijena.jipipe.plugins.parameters.library.graph.GraphNodeReferenceParameter;
import org.hkijena.jipipe.plugins.parameters.library.graph.GraphNodeReferenceParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.images.ImageDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.*;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLTextDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.matrix.Matrix2DFloat;
import org.hkijena.jipipe.plugins.parameters.library.pairs.*;
import org.hkijena.jipipe.plugins.parameters.library.patterns.StringPatternExtraction;
import org.hkijena.jipipe.plugins.parameters.library.patterns.StringPatternExtractionDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.primitives.*;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.*;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.*;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRangeDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.*;
import org.hkijena.jipipe.plugins.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.QuantityDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.ranges.FloatNumberRangeParameter;
import org.hkijena.jipipe.plugins.parameters.library.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.plugins.parameters.library.ranges.NumberRangeDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.ranges.NumberRangeParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.*;
import org.hkijena.jipipe.plugins.parameters.library.roi.*;
import org.hkijena.jipipe.plugins.parameters.library.scripts.ImageJMacro;
import org.hkijena.jipipe.plugins.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.plugins.parameters.library.table.DesktopParameterTableEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.table.ParameterTable;
import org.hkijena.jipipe.plugins.parameters.library.util.LogicalOperation;
import org.hkijena.jipipe.plugins.parameters.library.util.SortOrder;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.SizeFitMode;
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
@Plugin(type = JIPipeJavaPlugin.class, priority = Priority.FIRST)
public class StandardParametersPlugin extends JIPipePrepackagedDefaultJavaPlugin {

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
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        // Fallback editor for any parameter collection
        registerParameterEditor(JIPipeParameterCollection.class, JIPipeParameterCollectionDesktopParameterEditorUI.class);
        registerParameterType("jipipe:imagej-update-site",
                JIPipeImageJUpdateSiteDependency.class,
                JIPipeParameterArchetype.Value, JIPipeImageJUpdateSiteDependency.List.class,
                null,
                null,
                "ImageJ update site",
                "An ImageJ update site",
                null);
        registerEnumParameterType("graph-wrapper:iteration-mode",
                JIPipeGraphWrapperAlgorithm.IterationMode.class,
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
        registerImageParameters();
        registerVectorParameters();

        registerMenuExtension(ParameterTesterJIPipeDesktopMenuExtension.class);
        registerMenuExtension(ExpressionTesterJIPipeDesktopMenuExtension.class);
    }

    private void registerVectorParameters() {
        registerParameterType("vector2d", Vector2dParameter.class, JIPipeParameterArchetype.Value, "2D Vector (Double)", "2D vector containing double values", VectorDesktopParameterEditorUI.class);
        registerParameterType("vector2i", Vector2iParameter.class, JIPipeParameterArchetype.Value, "2D Vector (Integer)", "2D vector containing integer values", VectorDesktopParameterEditorUI.class);
        registerParameterType("vector3d", Vector3dParameter.class, JIPipeParameterArchetype.Value, "3D Vector (Double)", "3D vector containing double values", VectorDesktopParameterEditorUI.class);
        registerParameterType("vector3i", Vector3iParameter.class, JIPipeParameterArchetype.Value, "3D Vector (Integer)", "3D vector containing integer values", VectorDesktopParameterEditorUI.class);
        registerParameterType("optional-vector2d", OptionalVector2dParameter.class, JIPipeParameterArchetype.OptionalValue, "Optional 2D Vector (Double)", "2D vector containing double values");
        registerParameterType("optional-vector2i", OptionalVector2iParameter.class, JIPipeParameterArchetype.OptionalValue, "Optional 2D Vector (Integer)", "2D vector containing integers values");
        registerParameterType("optional-vector3d", OptionalVector3dParameter.class, JIPipeParameterArchetype.OptionalValue, "Optional 3D Vector (Double)", "3D vector containing double values");
        registerParameterType("optional-vector3i", OptionalVector3iParameter.class, JIPipeParameterArchetype.OptionalValue, "Optional 3D Vector (Double)", "3D vector containing integers values");
        registerParameterType("vector2d-list", Vector2dParameter.List.class, JIPipeParameterArchetype.List, "2D Vector list (Double)", "2D vector containing double values");
        registerParameterType("vector2i-list", Vector2iParameter.List.class, JIPipeParameterArchetype.List, "2D Vector list (Integer)", "2D vector containing integers values");
        registerParameterType("vector3d-list", Vector3dParameter.List.class, JIPipeParameterArchetype.List, "3D Vector list (Double)", "3D vector containing double values");
        registerParameterType("vector3i-list", Vector3iParameter.List.class, JIPipeParameterArchetype.List, "3D Vector list (Double)", "3D vector containing integers values");
    }

    private void registerImageParameters() {
        registerParameterType("image", ImageParameter.class, JIPipeParameterArchetype.Value, "Image", "An image", ImageDesktopParameterEditorUI.class);
    }

    private void registerQuantityParameters() {
        registerParameterType("quantity", Quantity.class, JIPipeParameterArchetype.Value, "Quantity", "A value with a unit", QuantityDesktopParameterEditorUI.class);
        registerParameterType("optional-quantity", OptionalQuantity.class, JIPipeParameterArchetype.OptionalValue, "Optional quantity", "A value with a unit");
        registerEnumParameterType("quantity-image-unit", Quantity.LengthUnit.class, "Length unit", "A length unit");
    }

    private void registerRangeParameters() {
        registerParameterEditor(NumberRangeParameter.class, NumberRangeDesktopParameterEditorUI.class);
        registerParameterType("number-range-float",
                FloatNumberRangeParameter.class,
                JIPipeParameterArchetype.Range, null,
                null,
                "Number range (float)",
                "A range of numbers",
                null);
        registerParameterType("number-range-integer",
                IntNumberRangeParameter.class,
                JIPipeParameterArchetype.Range, null,
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

        registerParameterEditor(ScriptParameter.class, ScriptDesktopParameterEditorUI.class);
        registerParameterType("ij-macro-code",
                ImageJMacro.class,
                JIPipeParameterArchetype.Value, null,
                null,
                "ImageJ macro",
                "An ImageJ macro code",
                null);
        registerParameterType("python-code",
                PythonScript.class,
                JIPipeParameterArchetype.Value, null,
                null,
                "Python script",
                "A Python script",
                null);
    }

    private void registerPatternParameters() {
        registerParameterType("string-pattern-extraction",
                StringPatternExtraction.class,
                JIPipeParameterArchetype.Value, StringPatternExtraction.List.class,
                null,
                null,
                "String pattern extraction",
                "Allows to extract a string from another string",
                StringPatternExtractionDesktopParameterEditorUI.class);
    }

    private void registerFunctionParameters() {
        registerParameterEditor(FunctionParameter.class, FunctionDesktopParameterEditorUI.class);
        registerParameterType("jipipe-trait:string-pattern-extraction:jipipe-trait:function",
                StringPatternExtractionFunction.class,
                JIPipeParameterArchetype.Value,
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
        registerParameterEditor(Matrix2D.class, Matrix2DDesktopParameterEditorUI.class);
        registerParameterType("matrix2d-float",
                Matrix2DFloat.class,
                JIPipeParameterArchetype.Value, Matrix2DFloat::new,
                p -> new Matrix2DFloat((Matrix2DFloat) p),
                "2D matrix (float)",
                "A matrix containing float numbers",
                null);
    }

    private void registerCollectionParameters() {
        // Collection parameters
        registerParameterEditor(ListParameter.class, ListDesktopParameterEditorUI.class);

        registerParameterType("parameter-collection-list",
                ParameterCollectionList.class,
                JIPipeParameterArchetype.List, "Parameter collection list",
                "A list of parameter collections",
                DesktopParameterCollectionListEditorUI.class);
    }

    private void registerMiscParameters() {
        // Other JIPipe parameters
        registerParameterType("parameter-visibilities",
                JIPipeParameterCollectionVisibilities.class,
                JIPipeParameterArchetype.MultiSelect, JIPipeParameterCollectionVisibilities::new,
                v -> new JIPipeParameterCollectionVisibilities((JIPipeParameterCollectionVisibilities) v),
                "Parameter visibilities",
                "Determines which parameters are visible to users",
                JIPipeParameterCollectionVisibilitiesDesktopParameterEditorUI.class);
        registerParameterType("parameter-table",
                ParameterTable.class,
                JIPipeParameterArchetype.Value, ParameterTable::new,
                t -> new ParameterTable((ParameterTable) t),
                "Parameter table",
                "A table that contains parameters",
                DesktopParameterTableEditorUI.class);
        registerParameterType("int-modification",
                NumericFunctionExpression.class,
                JIPipeParameterArchetype.Value, NumericFunctionExpression::new,
                t -> new NumericFunctionExpression((NumericFunctionExpression) t),
                "Integer modification",
                "Modifies an integer",
                null);
        registerParameterType("margin",
                Margin.class,
                JIPipeParameterArchetype.Value, Margin.List.class,
                null,
                null,
                "Margin",
                "Defines a rectangular area within a region",
                MarginDesktopParameterEditorUI.class);
        registerParameterType("fixed-margin",
                FixedMargin.class,
                JIPipeParameterArchetype.Value, FixedMargin.List.class,
                null,
                null,
                "Margin (fixed size)",
                "Places fixed-size objects into an area",
                FixedMarginEditorUI.class);
        registerParameterType("inner-margin",
                InnerMargin.class,
                JIPipeParameterArchetype.Value, InnerMargin.List.class,
                null,
                null,
                "Margin (inner)",
                "An inner margin (left, top, right, bottom)",
                InnerMarginEditorUIDesktop.class);
        registerParameterType("anchor",
                Anchor.class,
                JIPipeParameterArchetype.Value, null,
                null,
                "Anchor",
                "An anchor to a position within a rectangle",
                AnchorDesktopParameterEditorUI.class);
    }

    private void registerOptionalParameters() {
        // Optional parameters
        registerParameterEditor(OptionalParameter.class, OptionalDesktopParameterEditorUI.class);
        registerParameterType("optional-boolean",
                OptionalBooleanParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalBooleanParameter::new,
                o -> new OptionalBooleanParameter((OptionalBooleanParameter) o),
                "Optional boolean",
                "An optional boolean value",
                null);
        registerParameterType("optional-byte",
                OptionalByteParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalByteParameter::new,
                o -> new OptionalByteParameter((OptionalByteParameter) o),
                "Optional byte",
                "An optional byte value",
                null);
        registerParameterType("optional-double",
                OptionalDoubleParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalDoubleParameter::new,
                o -> new OptionalDoubleParameter((OptionalDoubleParameter) o),
                "Optional double",
                "An optional double value",
                null);
        registerParameterType("optional-float",
                OptionalFloatParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalFloatParameter::new,
                o -> new OptionalFloatParameter((OptionalFloatParameter) o),
                "Optional float",
                "An optional float value",
                null);
        registerParameterType("optional-integer",
                OptionalIntegerParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalIntegerParameter::new,
                o -> new OptionalIntegerParameter((OptionalIntegerParameter) o),
                "Optional integer",
                "An optional integer value",
                null);
        registerParameterType("optional-long",
                OptionalLongParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalLongParameter::new,
                o -> new OptionalLongParameter((OptionalLongParameter) o),
                "Optional long",
                "An optional long value",
                null);
        registerParameterType("optional-short",
                OptionalShortParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalShortParameter::new,
                o -> new OptionalShortParameter((OptionalShortParameter) o),
                "Optional short",
                "An optional short value",
                null);
        registerParameterType("optional-string",
                OptionalStringParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalStringParameter::new,
                o -> new OptionalStringParameter((OptionalStringParameter) o),
                "Optional string",
                "An optional string value",
                null);
        registerParameterType("optional-color",
                OptionalColorParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalColorParameter::new,
                o -> new OptionalColorParameter((OptionalColorParameter) o),
                "Optional color",
                "An optional color value",
                null);
        registerParameterType("optional-color-map",
                OptionalColorMapParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalColorMapParameter::new,
                o -> new OptionalColorMapParameter((OptionalColorMapParameter) o),
                "Optional color map",
                "An optional color map",
                null);
        registerParameterType("optional-path",
                OptionalPathParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalPathParameter::new,
                o -> new OptionalPathParameter((OptionalPathParameter) o),
                "Optional path",
                "An optional path",
                null);
        registerParameterType("optional-int-modification",
                OptionalIntModificationParameter.class,
                JIPipeParameterArchetype.OptionalValue, OptionalIntModificationParameter::new,
                o -> new OptionalIntModificationParameter((OptionalIntModificationParameter) o),
                "Optional integer modification",
                "An optional integer modification",
                null);
        registerParameterType("optional-annotation-name",
                OptionalTextAnnotationNameParameter.class,
                JIPipeParameterArchetype.OptionalValue, null,
                null,
                "Optional annotation name",
                "An optional annotation name",
                null);
        registerParameterType("optional-data-annotation-name",
                OptionalDataAnnotationNameParameter.class,
                JIPipeParameterArchetype.OptionalValue, null,
                null,
                "Optional data annotation name",
                "An optional data annotation name",
                null);
        registerParameterType("optional-integer-range",
                OptionalIntegerRange.class,
                JIPipeParameterArchetype.OptionalValue, null,
                null,
                "Optional integer range",
                "An optional range of integers",
                null);
    }

    private void registerEnumParameters() {
        // Enum-like parameters
        registerParameterEditor(DynamicEnumParameter.class, DynamicEnumDesktopParameterEditorUI.class);
        registerParameterEditor(DynamicSetParameter.class, DynamicSetDesktopParameterEditorUI.class);
        registerParameterType("string-enum",
                DynamicStringEnumParameter.class,
                JIPipeParameterArchetype.SingleSelect, DynamicStringEnumParameter::new,
                p -> new DynamicStringEnumParameter((DynamicStringEnumParameter) p),
                "String selection",
                "A selection of available strings",
                null);
        registerParameterType("data-display-operation-id-enum",
                DynamicDataDisplayOperationIdEnumParameter.class,
                JIPipeParameterArchetype.SingleSelect, null,
                null,
                "Data display operation",
                "A selection of data display operations",
                null);
        registerParameterType("data-import-operation-id-enum",
                DynamicDataImportOperationIdEnumParameter.class,
                JIPipeParameterArchetype.SingleSelect, null,
                null,
                "Data import operation",
                "A selection of data import operations",
                null);
        registerParameterType("dynamic-string-set",
                DynamicStringSetParameter.class,
                JIPipeParameterArchetype.MultiSelect, "String set selection",
                "A set of strings from which a subset can be selected");
        registerParameterType("font-family-enum",
                FontFamilyParameter.class,
                JIPipeParameterArchetype.SingleSelect, FontFamilyParameter::new,
                p -> new FontFamilyParameter((FontFamilyParameter) p),
                "Font family",
                "Available font families",
                null);
        registerEnumParameterType("font-style",
                FontStyleParameter.class,
                "Font style",
                "Available font styles");

        // Enums
        registerEnumParameterType("color-map",
                ColorMap.class,
                "Color map",
                "Available color maps that convert a scalar to a color");
        registerEnumParameterType("column-matching",
                JIPipeIterationStepTextAnnotationColumMatching.class,
                "Column matching strategy",
                "Determines how columns for dataset matching are selected");
        registerEnumParameterType("jipipe:ui:graph-editor-auto-layout",
                JIPipepGraphAutoLayoutMethod.class,
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

        registerParameterType("plugin-categories-enum",
                PluginCategoriesEnumParameter.class,
                JIPipeParameterArchetype.SingleSelect, PluginCategoriesEnumParameter.List.class,
                null,
                null,
                "Categories",
                "ImageJ categories",
                null);

        registerParameterType("size-fit-mode",
                SizeFitMode.class,
                JIPipeParameterArchetype.SingleSelect, "Size fit mode",
                "Algorithms to fit objects into other objects");
    }

    private void registerPairParameters() {
        // Pair-like parameters
        registerParameterEditor(PairParameter.class, PairDesktopParameterEditorUI.class);
        registerParameterType("string-query-expression:string:pair",
                StringQueryExpressionAndStringPairParameter.class,
                JIPipeParameterArchetype.Value, StringQueryExpressionAndStringPairParameter.List.class,
                StringQueryExpressionAndStringPairParameter::new,
                r -> new StringQueryExpressionAndStringPairParameter((StringQueryExpressionAndStringPairParameter) r),
                "String query / string pair",
                "A pair of a string query and a string",
                null);
        registerParameterType("string-query-expression:string-query-expression:pair",
                StringQueryExpressionAndStringQueryPairParameter.class,
                JIPipeParameterArchetype.Value, StringQueryExpressionAndStringQueryPairParameter.List.class,
                StringQueryExpressionAndStringQueryPairParameter::new,
                r -> new StringQueryExpressionAndStringQueryPairParameter((StringQueryExpressionAndStringQueryPairParameter) r),
                "String query pair",
                "A pair of two string queries",
                null);
        registerParameterType("integer:integer:pair",
                IntegerAndIntegerPairParameter.class,
                JIPipeParameterArchetype.Value, IntegerAndIntegerPairParameter.List.class,
                IntegerAndIntegerPairParameter::new,
                r -> new IntegerAndIntegerPairParameter((IntegerAndIntegerPairParameter) r),
                "Integer pair",
                "A pair of integers",
                null);
        registerParameterType("double:double:pair",
                DoubleAndDoublePairParameter.class,
                JIPipeParameterArchetype.Value, DoubleAndDoublePairParameter.List.class,
                null,
                null,
                "Double pair",
                "A pair of 64-bit floating point numbers",
                null);
        registerParameterType("string-query-expression:sort-order:pair",
                StringQueryExpressionAndSortOrderPairParameter.class,
                JIPipeParameterArchetype.Value, StringQueryExpressionAndSortOrderPairParameter.List.class,
                StringQueryExpressionAndSortOrderPairParameter::new,
                r -> new StringQueryExpressionAndSortOrderPairParameter((StringQueryExpressionAndSortOrderPairParameter) r),
                "String predicate to sort order",
                "Mapping from a string predicate to a sort order",
                null);
        registerParameterType("string:string:pair",
                StringAndStringPairParameter.class,
                JIPipeParameterArchetype.Value, StringAndStringPairParameter.List.class,
                null,
                null,
                "String pair",
                "A pair of strings",
                null);
        registerParameterType("expression:expression:pair",
                ExpressionAndExpressionPairParameter.class,
                JIPipeParameterArchetype.Value, ExpressionAndExpressionPairParameter.List.class,
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
                JIPipeParameterArchetype.Value, IntegerRange::new,
                f -> new IntegerRange((IntegerRange) f),
                "Integer range string",
                "Describes a range of whole numbers via a string. The string must have following format: " +
                        "[range];[range];... with range being an integer or [from]-[to]. [from] and [to] are inclusive borders. Negative numbers must be " +
                        "encased with (brackets). [from] and [to] can be in inverse order, generating numbers in inverse order. Spaces are ignored. " +
                        "Example: 0-10;5;3-(-1)",
                IntegerRangeDesktopParameterEditorUI.class);
        registerParameterType("integer-range:int:pair",
                IntRangeAndIntegerPairParameter.class,
                JIPipeParameterArchetype.Value, IntRangeAndIntegerPairParameter.List.class,
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
                JIPipeParameterArchetype.Value, JIPipeDataInfoRef::new,
                r -> new JIPipeDataInfoRef((JIPipeDataInfoRef) r),
                "Data type",
                "Reference to a data type",
                JIPipeDataInfoRefDesktopParameterEditorUI.class);
        registerParameterType("optional-data-type",
                OptionalDataInfoRefParameter.class,
                JIPipeParameterArchetype.OptionalValue, null,
                null,
                "Optional data type",
                "Optional reference to a data type",
                null);
        registerParameterType("algorithm-type",
                JIPipeNodeInfoRef.class,
                JIPipeParameterArchetype.Value, JIPipeNodeInfoRef::new,
                r -> new JIPipeNodeInfoRef((JIPipeNodeInfoRef) r),
                "Algorithm type",
                "Reference to an algorithm type",
                JIPipeNodeInfoRefDesktopParameterEditorUI.class);
        registerParameterType("optional-node-type",
                OptionalNodeInfoRefParameter.class,
                JIPipeParameterArchetype.OptionalValue, null,
                null,
                "Optional node type",
                "Optional reference to a node type",
                null);
        registerParameterType("imagej-importer",
                ImageJDataImporterRef.class,
                JIPipeParameterArchetype.Value, "ImageJ importer",
                "Operation that import data from ImageJ",
                ImageJDataImporterRefDesktopParameterEditorUI.class);
        registerParameterType("imagej-exporter",
                ImageJDataExporterRef.class,
                JIPipeParameterArchetype.Value, "ImageJ exporter",
                "Operation that exports data into ImageJ",
                ImageJDataExporterRefDesktopParameterEditorUI.class);
        registerParameterType("imagej-importer-configurable",
                ImageJDataImportOperationRef.class,
                JIPipeParameterArchetype.Value, "ImageJ importer (configurable)",
                "Operation that import data from ImageJ",
                ImageJDataImportOperationRefDesktopParameterEditorUI.class);
        registerParameterType("imagej-exporter-configurable",
                ImageJDataExportOperationRef.class,
                JIPipeParameterArchetype.Value, "ImageJ exporter (configurable)",
                "Operation that exports data into ImageJ",
                ImageJDataExportOperationRefDesktopParameterEditorUI.class);
        registerParameterType("parameter-type",
                JIPipeParameterTypeInfoRef.class,
                JIPipeParameterArchetype.Value, "Parameter type",
                "A parameter type",
                JIPipeParameterTypeInfoRefDesktopParameterEditorUI.class);
        registerParameterType("runtime-partition-reference",
                RuntimePartitionReferenceParameter.class,
                JIPipeParameterArchetype.Reference, "Runtime partition",
                "A reference to a project runtime partition",
                RuntimePartitionReferenceDesktopParameterEditorUI.class);
        registerParameterType("artifact-query",
                JIPipeArtifactQueryParameter.class,
                JIPipeParameterArchetype.Value, JIPipeArtifactQueryParameter::new,
                p -> new JIPipeArtifactQueryParameter((JIPipeArtifactQueryParameter) p),
                "Artifact query",
                "Queries an artifact from the artifact repository. " +
                        "Should be formatted as GroupId.ArtifactId:Version-Classifier",
                JIPipeDesktopArtifactQueryParameterEditorUI.class);
        registerEnumParameterType("project-directory-role",
                JIPipeProjectDirectories.Role.class,
                "Project directory role",
                "Determines the role of a project directory (input/output/unspecified)");

        // Icon types
        registerParameterType("algorithm-type-icon",
                IconRef.class,
                JIPipeParameterArchetype.Value, IconRef::new,
                r -> new IconRef((IconRef) r),
                "Algorithm type icon",
                "Reference to an algorithm type icon",
                IconRefDesktopParameterEditorUI.class);

        // Metadata
        registerParameterType("author",
                JIPipeAuthorMetadata.class,
                JIPipeParameterArchetype.Value, JIPipeAuthorMetadata.List.class,
                null,
                null,
                "Author",
                "An author with affiliations",
                JIPipeAuthorMetadataDesktopParameterEditorUI.class);
        registerParameterType("optional-author",
                OptionalJIPipeAuthorMetadata.class,
                JIPipeParameterArchetype.OptionalValue, OptionalJIPipeAuthorMetadata.List.class,
                null,
                null,
                "Optional author",
                "An author with affiliations",
                null);
        registerParameterType("organization",
                JIPipeOrganizationMetadata.class,
                JIPipeParameterArchetype.Value, JIPipeOrganizationMetadata.List.class,
                null,
                null,
                "Organization",
                "An organization/institute/university",
                JIPipeOrganizationMetadataDesktopParameterEditorUI.class);

        // Node templates
        registerParameterType("node-template",
                JIPipeNodeTemplate.class,
                JIPipeParameterArchetype.Value, JIPipeNodeTemplate.List.class,
                null,
                null,
                "Node template",
                "Stores a copy of a node",
                JIPipeNodeTemplateDesktopParameterEditorUI.class);

        // File chooser
        registerParameterType("file-chooser-bookmark",
                FileChooserBookmark.class,
                JIPipeParameterArchetype.Value, FileChooserBookmarkList.class,
                null,
                null,
                "File chooser bookmark",
                "Bookmark for a path",
                null);

        // Graph node reference
        registerParameterType("graph-node-reference",
                GraphNodeReferenceParameter.class,
                JIPipeParameterArchetype.Reference, GraphNodeReferenceParameter.List.class,
                null,
                null,
                "Graph node reference",
                "Reference to a graph node",
                GraphNodeReferenceParameterEditorUI.class);

        // Theming
        registerParameterType("theme-style",
                JIPipeModernThemeStyleParameter.class,
                JIPipeParameterArchetype.SingleSelect,
                "JIPipe modern theme style",
                "A style for the modern theming system");
    }

    private void registerCommonJavaTypes() {
        // Register other common Java classes
        registerParameterEditor(Enum.class, EnumDesktopParameterEditorUI.class);
        registerParameterType("string", String.class, JIPipeParameterArchetype.Value, StringList.class, () -> "", s -> s, "String", "A text value", StringDesktopParameterEditorUI.class);
        registerParameterType("password", PasswordParameter.class, JIPipeParameterArchetype.Value, null, null, "Password", "A password", PasswordDesktopParameterEditorUI.class);
        registerParameterType("path", Path.class, JIPipeParameterArchetype.Value, PathList.class, () -> Paths.get(""), p -> p, "Filesystem path", "A path", FilePathDesktopParameterEditorUI.class);
        registerParameterType("file", File.class, JIPipeParameterArchetype.Value, () -> new File(""), f -> f, "Filesystem path", "A path", FileDesktopParameterEditorUI.class);
        registerParameterType("color", Color.class, JIPipeParameterArchetype.Value, () -> Color.WHITE, c -> c, "Color", "A color", ColorDesktopParameterEditorUI.class);
        registerParameterType("color-list", ColorListParameter.class, JIPipeParameterArchetype.Value, "Color list", "A list of colors");
        registerParameterType("rectangle", Rectangle.class, JIPipeParameterArchetype.Value, RectangleList.class, Rectangle::new, o -> new Rectangle((Rectangle) o), "Rectangle", "A rectangle", RectangleDesktopParameterEditorUI.class);
        registerColorJsonSerializer();
        registerRectangleJsonSerializer();
        // Compound types
        registerParameterType("string-or-double",
                StringOrDouble.class,
                JIPipeParameterArchetype.Value, null,
                null,
                "String/Double",
                "An object that can either hold a string or double.",
                StringOrDoubleDesktopParameterEditorUI.class);
        registerParameterType("html-text",
                HTMLText.class,
                JIPipeParameterArchetype.Value, null,
                null,
                "HTML text",
                "A formatted text",
                HTMLTextDesktopParameterEditorUI.class);
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
        registerParameterType(new BooleanPrimitiveParameterTypeInfo(), BooleanDesktopParameterEditorUI.class);
        registerParameterType(new BooleanParameterTypeInfo(), BooleanDesktopParameterEditorUI.class);

        // Register numbers
        registerParameterType(new BytePrimitiveParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new ShortPrimitiveParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new IntPrimitiveParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new LongPrimitiveParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new FloatPrimitiveParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new DoublePrimitiveParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new ByteParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new ShortParameterTypeInfo(), NumberDesktopParameterEditorUI.class);
        registerParameterType(new IntParameterTypeInfo(), IntegerList.class, NumberDesktopParameterEditorUI.class);
        registerParameterType(new LongParameterTypeInfo(), LongList.class, NumberDesktopParameterEditorUI.class);
        registerParameterType(new FloatParameterTypeInfo(), FloatList.class, NumberDesktopParameterEditorUI.class);
        registerParameterType(new DoubleParameterTypeInfo(), DoubleList.class, NumberDesktopParameterEditorUI.class);
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

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
