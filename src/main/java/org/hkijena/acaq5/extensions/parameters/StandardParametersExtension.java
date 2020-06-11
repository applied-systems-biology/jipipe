package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.parameters.collections.*;
import org.hkijena.acaq5.extensions.parameters.colors.*;
import org.hkijena.acaq5.extensions.parameters.editors.*;
import org.hkijena.acaq5.extensions.parameters.pairs.*;
import org.hkijena.acaq5.extensions.parameters.predicates.DoublePredicate;
import org.hkijena.acaq5.extensions.parameters.predicates.PathPredicate;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.parameters.predicates.StringOrDoublePredicate;
import org.hkijena.acaq5.extensions.parameters.functions.FunctionParameter;
import org.hkijena.acaq5.extensions.parameters.generators.*;
import org.hkijena.acaq5.extensions.parameters.matrix.Matrix2DFloat;
import org.hkijena.acaq5.extensions.parameters.matrix.Matrix2D;
import org.hkijena.acaq5.extensions.parameters.primitives.*;
import org.hkijena.acaq5.extensions.parameters.references.*;
import org.hkijena.acaq5.extensions.parameters.roi.*;
import org.hkijena.acaq5.extensions.parameters.table.ParameterTable;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.JsonUtils;
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
    }

    private void registerFunctionParameters() {
        registerParameterEditor(FunctionParameter.class, FunctionParameterEditorUI.class);
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
        registerParameterType("integer-list", IntegerList.class,
                IntegerList::new,
                l -> new IntegerList((IntegerList) l),
                "List of integers",
                "A list of integers",
                null);
        registerParameterType("float-list", FloatList.class,
                FloatList::new,
                l -> new FloatList((FloatList) l),
                "List of float",
                "A list of float",
                null);
        registerParameterType("double-list", DoubleList.class,
                DoubleList::new,
                l -> new DoubleList((DoubleList) l),
                "List of double",
                "A list of double",
                null);
        registerParameterType("string-ist", StringList.class,
                StringList::new,
                l -> new StringList((StringList) l),
                "List of strings",
                "A list of strings",
                null);
        registerParameterType("path-filter-list", PathPredicate.List.class,
                List::new,
                l -> new PathPredicate.List((PathPredicate.List) l),
                "List of path filters",
                "A list of filters that filter folder or file names",
                null);
        registerParameterType("string-filter-list", StringPredicate.List.class,
                List::new,
                l -> new StringPredicate.List((StringPredicate.List) l),
                "List of string filters",
                "A list of filters for strings",
                null);
        registerParameterType("string-or-double-filter-list", StringOrDoublePredicate.List.class,
                List::new,
                l -> new StringOrDoublePredicate.List((StringOrDoublePredicate.List) l),
                "List of string/double filters",
                "A list of filters for strings/doubles",
                null);
        registerParameterType("string-filter:string-or-double-filter:list", StringFilterAndStringOrDoubleFilterPair.List.class,
                List::new,
                l -> new StringFilterAndStringOrDoubleFilterPair.List((StringFilterAndStringOrDoubleFilterPair.List) l),
                "String filter to String/double filter assignment list",
                "A list of string filter to strings/double filter pairs",
                null);
        registerParameterType("trait-type-list", ACAQTraitDeclarationRef.List.class,
                List::new,
                l -> new ACAQTraitDeclarationRef.List((ACAQTraitDeclarationRef.List) l),
                "List of annotation types",
                "A list of annotation types",
                ACAQTraitDeclarationRefCollectionParameterEditorUI.class);
        registerParameterType("string-renaming-list", StringFilterAndStringPair.List.class,
                List::new,
                l -> new StringFilterAndStringPair.List((StringFilterAndStringPair.List) l),
                "List of string renaming operations",
                "A list of operations that rename strings",
                null);
        registerParameterType("integer-integer-pair-list", IntegerAndIntegerPair.List.class,
                List::new,
                l -> new IntegerAndIntegerPair.List((IntegerAndIntegerPair.List) l),
                "List of integer replacement operations",
                "A list of operations that replace integers",
                null);
        registerParameterType("integer-renaming-list", IntRangeAndIntegerPair.List.class,
                List::new,
                l -> new IntRangeAndIntegerPair.List((IntRangeAndIntegerPair.List) l),
                "List of integer replacement operations",
                "A list of operations that replace integers",
                null);
        registerParameterType("rectangle-list", RectangleList.class,
                RectangleList::new,
                l -> new RectangleList((RectangleList) l),
                "Rectangle list",
                "A list of rectangles",
                null);
        registerParameterType("margin-list", Margin.List.class,
                List::new,
                l -> new Margin.List((Margin.List) l),
                "Margin list",
                "A list of margins",
                null);
        registerParameterType("string-filter:sort-order:list", StringFilterAndSortOrderPair.List.class,
                List::new,
                l -> new StringFilterAndSortOrderPair.List((StringFilterAndSortOrderPair.List) l),
                "List of string filter to sort order pairs",
                "A list of string filters associated to sort orders",
                null);
    }

    private void registerMiscParameters() {
        // Other ACAQ5 parameters
        registerParameterType("trait",
                ACAQTrait.class,
                () -> null,
                t -> ((ACAQTrait) t).duplicate(),
                "Annotation",
                "An annotation",
                ACAQTraitParameterEditorUI.class);
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
        registerParameterType("path-list",
                PathList.class,
                PathList::new,
                t -> new PathList((PathList) t),
                "Path list",
                "A list of file or folder paths",
                PathCollectionParameterEditorUI.class);
        registerParameterType("int-modification",
                IntModificationParameter.class,
                IntModificationParameter::new,
                t -> new IntModificationParameter((IntModificationParameter) t),
                "Integer modification",
                "Modifies an integer",
                IntModificationParameterEditorUI.class);
        registerParameterType("margin",
                Margin.class,
                Margin::new,
                r -> new Margin((Margin) r),
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
        // Map-like parameters
        registerParameterEditor(Pair.class, KeyValuePairParameterEditorUI.class);
        registerParameterType("string-renaming",
                StringFilterAndStringPair.class,
                StringFilterAndStringPair::new,
                r -> new StringFilterAndStringPair((StringFilterAndStringPair) r),
                "Text replacement",
                "Replaces a matched string by the target string",
                null);
        registerParameterType("integer-integer-pair",
                IntegerAndIntegerPair.class,
                IntegerAndIntegerPair::new,
                r -> new IntegerAndIntegerPair((IntegerAndIntegerPair) r),
                "Integer replacement",
                "Replaces a number with another number",
                null);
        registerParameterType("string-filter:string-or-double-filter:pair",
                StringFilterAndStringOrDoubleFilterPair.class,
                StringFilterAndStringOrDoubleFilterPair::new,
                r -> new StringFilterAndStringOrDoubleFilterPair((StringFilterAndStringOrDoubleFilterPair) r),
                "String filter to string/double filter",
                "Mapping from a string filter to a string/double filter",
                null);
        registerParameterType("string-filter:sort-order:pair",
                StringFilterAndSortOrderPair.class,
                StringFilterAndSortOrderPair::new,
                r -> new StringFilterAndSortOrderPair((StringFilterAndSortOrderPair) r),
                "String filter to sort order",
                "Mapping from a string filter to a sort order",
                null);
    }

    private void registerGeneratingParameters() {
        // Generating parameters
        registerParameterType("int-range-string",
                IntRangeStringParameter.class,
                IntRangeStringParameter::new,
                f -> new IntRangeStringParameter((IntRangeStringParameter) f),
                "Integer range string",
                "Describes a range of whole numbers via a string. The string must have following format: " +
                        "[range];[range];... with range being an integer or [from]-[to]. [from] and [to] are inclusive borders. Negative numbers must be " +
                        "encased with (brackets). [from] and [to] can be in inverse order, generating numbers in inverse order. Spaces are ignored. " +
                        "Example: 0-10;5;3-(-1)",
                IntRangeStringParameterEditorUI.class);
    }

    private void registerFilterParameters() {
        // Filter parameters
        registerParameterType("path-filter",
                PathPredicate.class,
                PathPredicate::new,
                f -> new PathPredicate((PathPredicate) f),
                "Path filter",
                "A filter for file or folder names",
                PathFilterParameterEditorUI.class);
        registerParameterType("string-filter",
                StringPredicate.class,
                StringPredicate::new,
                f -> new StringPredicate((StringPredicate) f),
                "String filter",
                "A filter for text values",
                StringFilterParameterEditorUI.class);
        registerParameterType("double-filter",
                DoublePredicate.class,
                DoublePredicate::new,
                f -> new DoublePredicate((DoublePredicate) f),
                "Double filter",
                "A filter for numbers",
                DoubleFilterParameterEditorUI.class);
        registerParameterType("string-or-double-filter",
                StringOrDoublePredicate.class,
                StringOrDoublePredicate::new,
                f -> new StringOrDoublePredicate((StringOrDoublePredicate) f),
                "String/Double filter",
                "A filter for numbers or strings",
                StringOrDoubleFilterParameterEditorUI.class);
    }

    private void registerACAQ5ReferenceTypes() {
        // ACAQ5 registry reference types
        registerParameterType("trait-type",
                ACAQTraitDeclarationRef.class,
                ACAQAlgorithmDeclarationRef::new,
                r -> new ACAQAlgorithmDeclarationRef((ACAQAlgorithmDeclaration) r),
                "Annotation type",
                "Reference to an annotation type",
                ACAQTraitDeclarationRefParameterEditorUI.class);
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
        registerParameterType("trait-type-icon",
                ACAQTraitIconRef.class,
                ACAQTraitIconRef::new,
                r -> new ACAQTraitIconRef((ACAQTraitIconRef) r),
                "Annotation type icon",
                "Reference to an annotation type icon",
                ACAQTraitIconRefParameterEditorUI.class);
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
        registerParameterType("path", Path.class, () -> Paths.get(""), p -> p, "Filesystem path", "A path", FilePathParameterEditorUI.class);
        registerParameterType("file", File.class, () -> new File(""), f -> f, "Filesystem path", "A path", FileParameterEditorUI.class);
        registerParameterType("color", Color.class, () -> Color.WHITE, c -> c, "Color", "A color", ColorParameterEditorUI.class);
        registerParameterType("rectangle", Rectangle.class, Rectangle::new, o -> new Rectangle((Rectangle) o), "Rectangle", "A rectangle", RectangleParameterEditorUI.class);
        registerColorJsonSerializer();
        registerRectangleJsonSerializer();
    }

    private void registerPrimitives() {
        // Register boolean
        registerParameterType(new BooleanPrimitiveParameterTypeDeclaration(), BooleanParameterEditorUI.class);
        registerParameterType(new BooleanParameterTypeDeclaration(), BooleanParameterEditorUI.class);

        // Register numbers
        registerParameterType(new BytePrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new ShortPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new IntPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new LongPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new FloatPrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new DoublePrimitiveParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new ByteParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new ShortParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new IntParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new LongParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new FloatParameterTypeDeclaration(), NumberParameterEditorUI.class);
        registerParameterType(new DoubleParameterTypeDeclaration(), NumberParameterEditorUI.class);
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
