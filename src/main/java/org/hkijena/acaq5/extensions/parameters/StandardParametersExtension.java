package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameterEditorUI;
import org.hkijena.acaq5.extensions.parameters.colors.*;
import org.hkijena.acaq5.extensions.parameters.editors.*;
import org.hkijena.acaq5.extensions.parameters.functions.StringPatternExtractionFunction;
import org.hkijena.acaq5.extensions.parameters.functions.FunctionParameter;
import org.hkijena.acaq5.extensions.parameters.functions.FunctionParameterEditorUI;
import org.hkijena.acaq5.extensions.parameters.generators.*;
import org.hkijena.acaq5.extensions.parameters.matrix.Matrix2D;
import org.hkijena.acaq5.extensions.parameters.matrix.Matrix2DFloat;
import org.hkijena.acaq5.extensions.parameters.matrix.Matrix2DParameterEditorUI;
import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameter;
import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameterEditorUI;
import org.hkijena.acaq5.extensions.parameters.pairs.*;
import org.hkijena.acaq5.extensions.parameters.patterns.StringPatternExtraction;
import org.hkijena.acaq5.extensions.parameters.patterns.StringPatternExtractionParameterEditorUI;
import org.hkijena.acaq5.extensions.parameters.predicates.*;
import org.hkijena.acaq5.extensions.parameters.primitives.*;
import org.hkijena.acaq5.extensions.parameters.references.*;
import org.hkijena.acaq5.extensions.parameters.roi.*;
import org.hkijena.acaq5.extensions.parameters.table.ParameterTable;
import org.hkijena.acaq5.extensions.parameters.table.ParameterTableEditorUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.jfree.chart.util.BooleanList;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides some standard parameters
 */
@Plugin(type = ACAQJavaExtension.class, priority = Priority.FIRST)
public class StandardParametersExtension extends ACAQPrepackagedDefaultJavaExtension {

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
        return "org.hkijena.acaq5:parameter-editors";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerPrimitives();
        registerCommonJavaTypes();
        registerACAQ5ReferenceTypes();
        registerFilterParameters();
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
        registerParameterType("acaq-trait:string-pattern-extraction:acaq-trait:function",
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
        registerParameterGenerator(byte.class, ByteRangeParameterGenerator.class, "Generate 8-bit integral number sequence", "Generates 8-bit integral numbers");
        registerParameterGenerator(short.class, ShortRangeParameterGenerator.class, "Generate 16-bit integral number sequence", "Generates 16-bit integral numbers");
        registerParameterGenerator(int.class, IntegerRangeParameterGenerator.class, "Generate 32-bit integral number sequence", "Generates 32-bit integral numbers");
        registerParameterGenerator(long.class, LongRangeParameterGenerator.class, "Generate 64-bit integral number sequence", "Generates 64-bit integral numbers");
        registerParameterGenerator(float.class, FloatRangeParameterGenerator.class, "Generate single precision floating point number sequence", "Generates 32-bit floating point numbers");
        registerParameterGenerator(double.class, DoubleRangeParameterGenerator.class, "Generate double precision floating point number sequence", "Generates 64-bit floating point numbers");
        registerParameterGenerator(Byte.class, ByteRangeParameterGenerator.class, "Generate 8-bit integral number sequence", "Generates 8-bit integral numbers");
        registerParameterGenerator(Short.class, ShortRangeParameterGenerator.class, "Generate 16-bit integral number sequence", "Generates 16-bit integral numbers");
        registerParameterGenerator(Integer.class, IntegerRangeParameterGenerator.class, "Generate 32-bit integral number sequence", "Generates 32-bit integral numbers");
        registerParameterGenerator(Long.class, LongRangeParameterGenerator.class, "Generate 64-bit integral number sequence", "Generates 64-bit integral numbers");
        registerParameterGenerator(Float.class, FloatRangeParameterGenerator.class, "Generate single precision floating point number sequence", "Generates 32-bit floating point numbers");
        registerParameterGenerator(Double.class, DoubleRangeParameterGenerator.class, "Generate double precision floating point number sequence", "Generates 64-bit floating point numbers");
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
        // Other ACAQ5 parameters
        registerParameterType("parameter-visibilities",
                ACAQParameterCollectionVisibilities.class,
                ACAQParameterCollectionVisibilities::new,
                v -> new ACAQParameterCollectionVisibilities((ACAQParameterCollectionVisibilities) v),
                "Parameter visibilities",
                "Determines which parameters are visible to users",
                ACAQParameterCollectionVisibilitiesParameterEditorUI.class);
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
    }

    private void registerEnumParameters() {
        // Enum-like parameters
        registerParameterEditor(DynamicEnumParameter.class, DynamicEnumParameterEditorUI.class);
        registerParameterType("string-enum",
                DynamicStringEnumParameter.class,
                DynamicStringEnumParameter::new,
                p -> new DynamicStringEnumParameter((DynamicStringEnumParameter) p),
                "String selection",
                "A selection of available strings",
                null);

        // Enums
        registerEnumParameterType("color-map", ColorMap.class, "Color map", "Available color maps that convert a scalar to a color");
        registerEnumParameterType("rectangle-roi:anchor", Margin.Anchor.class, "Anchor", "Available rectangle anchors");
        registerEnumParameterType("path-filter:mode", PathPredicate.Mode.class, "Mode", "Available modes");
        registerEnumParameterType("string-filter:mode", StringPredicate.Mode.class, "Mode", "Available modes");
        registerEnumParameterType("acaq:iterating-algorithm:column-matching", ACAQIteratingAlgorithm.ColumnMatching.class, "Column matching strategy", "Determines how columns for dataset matching are selected");
        registerEnumParameterType("acaq:ui:graph-editor-view-mode", ACAQAlgorithmGraphCanvasUI.ViewMode.class, "Graph editor view mode", "Determines how the graphs are displayed");

    }

    private void registerPairParameters() {
        // Pair-like parameters
        registerParameterEditor(Pair.class, PairParameterEditorUI.class);
        registerParameterType("string-renaming",
                StringFilterAndStringPair.class,
                StringFilterAndStringPair.List.class,
                StringFilterAndStringPair::new,
                r -> new StringFilterAndStringPair((StringFilterAndStringPair) r),
                "Text replacement",
                "Replaces a matched string by the target string",
                null);
        registerParameterType("integer-integer-pair",
                IntegerAndIntegerPair.class,
                IntegerAndIntegerPair.List.class,
                IntegerAndIntegerPair::new,
                r -> new IntegerAndIntegerPair((IntegerAndIntegerPair) r),
                "Integer replacement",
                "Replaces a number with another number",
                null);
        registerParameterType("string-filter:string-or-double-filter:pair",
                StringFilterAndStringOrDoubleFilterPair.class,
                StringFilterAndStringOrDoubleFilterPair.List.class,
                StringFilterAndStringOrDoubleFilterPair::new,
                r -> new StringFilterAndStringOrDoubleFilterPair((StringFilterAndStringOrDoubleFilterPair) r),
                "String filter to string/double filter",
                "Mapping from a string filter to a string/double filter",
                null);
        registerParameterType("string-filter:sort-order:pair",
                StringFilterAndSortOrderPair.class,
                StringFilterAndSortOrderPair.List.class,
                StringFilterAndSortOrderPair::new,
                r -> new StringFilterAndSortOrderPair((StringFilterAndSortOrderPair) r),
                "String filter to sort order",
                "Mapping from a string filter to a sort order",
                null);
        registerParameterType("string:string-predicate:pair",
                StringAndStringPredicatePair.class,
                StringAndStringPredicatePair.List.class,
                null,
                null,
                "String to string predicate",
                "Mapping from a string to a string predicate",
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

    private void registerFilterParameters() {
        // Filter parameters
        registerParameterType("path-predicate",
                PathPredicate.class,
                PathPredicate.List.class,
                PathPredicate::new,
                f -> new PathPredicate((PathPredicate) f),
                "Path filter",
                "A filter for file or folder names",
                PathPredicateParameterEditorUI.class);
        registerParameterType("string-predicate",
                StringPredicate.class,
                StringPredicate.List.class,
                StringPredicate::new,
                f -> new StringPredicate((StringPredicate) f),
                "String filter",
                "A filter for text values",
                StringPredicateParameterEditorUI.class);
        registerParameterType("double-predicate",
                DoublePredicate.class,
                DoublePredicate.List.class,
                DoublePredicate::new,
                f -> new DoublePredicate((DoublePredicate) f),
                "Double filter",
                "A filter for numbers",
                DoublePredicateParameterEditorUI.class);
        registerParameterType("string-or-double-predicate",
                StringOrDoublePredicate.class,
                StringOrDoublePredicate.List.class,
                StringOrDoublePredicate::new,
                f -> new StringOrDoublePredicate((StringOrDoublePredicate) f),
                "String/Double filter",
                "A filter for numbers or strings",
                StringOrDoublePredicateParameterEditorUI.class);
    }

    private void registerACAQ5ReferenceTypes() {
        // ACAQ5 registry reference types
        registerParameterType("data-type",
                ACAQDataDeclarationRef.class,
                ACAQDataDeclarationRef::new,
                r -> new ACAQDataDeclarationRef((ACAQDataDeclarationRef) r),
                "Data type",
                "Reference to a data type",
                ACAQDataDeclarationRefParameterEditorUI.class);
        registerParameterType("algorithm-type",
                ACAQAlgorithmDeclarationRef.class,
                ACAQAlgorithmDeclarationRef::new,
                r -> new ACAQAlgorithmDeclarationRef((ACAQAlgorithmDeclarationRef) r),
                "Algorithm type",
                "Reference to an algorithm type",
                ACAQAlgorithmDeclarationRefParameterEditorUI.class);

        // Icon types

        registerParameterType("algorithm-type-icon",
                ACAQAlgorithmIconRef.class,
                ACAQAlgorithmIconRef::new,
                r -> new ACAQAlgorithmIconRef((ACAQAlgorithmIconRef) r),
                "Algorithm type icon",
                "Reference to an algorithm type icon",
                ACAQAlgorithmIconRefParameterEditorUI.class);
    }

    private void registerCommonJavaTypes() {
        // Register other common Java classes
        registerParameterEditor(Enum.class, EnumParameterEditorUI.class);
        registerParameterType("string", String.class, () -> "", s -> s, "String", "A text value", StringParameterEditorUI.class);
        registerParameterType("path", Path.class, PathList.class, () -> Paths.get(""), p -> p, "Filesystem path", "A path", FilePathParameterEditorUI.class);
        registerParameterEditor(PathList.class, PathListParameterEditorUI.class);
        registerParameterType("file", File.class, () -> new File(""), f -> f, "Filesystem path", "A path", FileParameterEditorUI.class);
        registerParameterType("color", Color.class, () -> Color.WHITE, c -> c, "Color", "A color", ColorParameterEditorUI.class);
        registerParameterType("rectangle", Rectangle.class, RectangleList.class, Rectangle::new, o -> new Rectangle((Rectangle) o), "Rectangle", "A rectangle", RectangleParameterEditorUI.class);
        registerColorJsonSerializer();
        registerRectangleJsonSerializer();
    }

    private void registerPrimitives() {
        // Register boolean
        registerParameterType(new BooleanPrimitiveParameterTypeDeclaration(), BooleanParameterEditorUI.class);
        registerParameterType(new BooleanParameterTypeDeclaration(), BooleanList.class, BooleanParameterEditorUI.class);

        // Register numbers
        registerParameterType(new BytePrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new ShortPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new IntPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new LongPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new FloatPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new DoublePrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new ByteParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new ShortParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new IntParameterTypeDeclaration(), IntegerList.class, NumberParameterEditorUI.class);
        registerParameterType(new LongParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new FloatParameterTypeDeclaration(), FloatList.class, NumberParameterEditorUI.class);
        registerParameterType(new DoubleParameterTypeDeclaration(), DoubleList.class, NumberParameterEditorUI.class);
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
