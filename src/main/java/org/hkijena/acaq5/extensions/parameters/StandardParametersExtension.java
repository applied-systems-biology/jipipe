package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefList;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.parameters.collections.*;
import org.hkijena.acaq5.extensions.parameters.editors.*;
import org.hkijena.acaq5.extensions.parameters.filters.IntegerRenaming;
import org.hkijena.acaq5.extensions.parameters.filters.PathFilter;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.extensions.parameters.filters.StringRenaming;
import org.hkijena.acaq5.extensions.parameters.generators.*;
import org.hkijena.acaq5.extensions.parameters.primitives.*;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitIconRef;
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

        // Register other common Java classes
        registerParameterEditor(Enum.class, EnumParameterEditorUI.class);
        registerParameterType("string", String.class, () -> "", s -> s, "String", "A text value", StringParameterEditorUI.class);
        registerParameterType("path", Path.class, () -> Paths.get(""), p -> p, "Filesystem path", "A path", FilePathParameterEditorUI.class);
        registerParameterType("file", File.class, () -> new File(""), f -> f, "Filesystem path", "A path", FileParameterEditorUI.class);
        registerParameterType("color", Color.class, () -> Color.WHITE, c -> c, "Color", "A color", ColorParameterEditorUI.class);
        registerColorJsonSerializer();

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

        // Special ACAQ5 reference types
        registerParameterType("trait-type-icon",
                ACAQTraitIconRef.class,
                ACAQTraitIconRef::new,
                r -> new ACAQTraitIconRef((ACAQTraitIconRef) r),
                "Annotation type icon",
                "Reference to an annotation type icon",
                ACAQTraitIconRefParameterEditorUI.class);

        // Filter parameters
        registerParameterType("path-filter",
                PathFilter.class,
                PathFilter::new,
                f -> new PathFilter((PathFilter) f),
                "Path filter",
                "A filter for file or folder names",
                PathFilterParameterEditorUI.class);
        registerParameterType("string-filter",
                StringFilter.class,
                StringFilter::new,
                f -> new StringFilter((StringFilter) f),
                "String filter",
                "A filter for text values",
                StringFilterParameterEditorUI.class);

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

        // Map-like parameters
        registerParameterEditor(KeyValuePairParameter.class, KeyValuePairParameterEditorUI.class);
        registerParameterType("string-renaming",
                StringRenaming.class,
                StringRenaming::new,
                r -> new StringRenaming((StringRenaming) r),
                "Text replacement",
                "Replaces a matched string by the target string",
                null);
        registerParameterType("integer-renaming",
                IntegerRenaming.class,
                IntegerRenaming::new,
                r -> new IntegerRenaming((IntegerRenaming) r),
                "Integer replacement",
                "Replaces a number with another number",
                null);

        // Enum-like parameters
        registerParameterEditor(DynamicEnumParameter.class, DynamicEnumParameterEditorUI.class);
        registerParameterType("string-enum",
                DynamicStringEnumParameter.class,
                DynamicStringEnumParameter::new,
                p -> new DynamicStringEnumParameter((DynamicStringEnumParameter) p),
                "String selection",
                "A selection of available strings",
                null);

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
                PathListParameter.class,
                PathListParameter::new,
                t -> new PathListParameter((PathListParameter) t),
                "Path list",
                "A list of file or folder paths",
                PathCollectionParameterEditorUI.class);


        // Collection parameters
        registerParameterEditor(ListParameter.class, CollectionParameterEditorUI.class);
        registerParameterType("integer-list", IntListParameter.class,
                IntListParameter::new,
                l -> new IntListParameter((IntListParameter) l),
                "List of integers",
                "A list of integers",
                null);
        registerParameterType("float-list", FloatListParameter.class,
                FloatListParameter::new,
                l -> new FloatListParameter((FloatListParameter) l),
                "List of float",
                "A list of float",
                null);
        registerParameterType("double-list", DoubleListParameter.class,
                DoubleListParameter::new,
                l -> new DoubleListParameter((DoubleListParameter) l),
                "List of double",
                "A list of double",
                null);
        registerParameterType("string-ist", StringListParameter.class,
                StringListParameter::new,
                l -> new StringListParameter((StringListParameter) l),
                "List of strings",
                "A list of strings",
                null);
        registerParameterType("path-filter-list", PathFilterListParameter.class,
                PathFilterListParameter::new,
                l -> new PathFilterListParameter((PathFilterListParameter) l),
                "List of path filters",
                "A list of filters that filter folder or file names",
                null);
        registerParameterType("string-filter-list", StringFilterListParameter.class,
                StringFilterListParameter::new,
                l -> new StringFilterListParameter((StringFilterListParameter) l),
                "List of string filters",
                "A list of filters for strings",
                null);
        registerParameterType("trait-type-list", ACAQTraitDeclarationRefList.class,
                ACAQTraitDeclarationRefList::new,
                l -> new ACAQTraitDeclarationRefList((ACAQTraitDeclarationRefList) l),
                "List of annotation types",
                "A list of annotation types",
                ACAQTraitDeclarationRefCollectionParameterEditorUI.class);
        registerParameterType("string-renaming-list", StringRenamingList.class,
                StringRenamingList::new,
                l -> new StringRenamingList((StringRenamingList) l),
                "List of string renaming operations",
                "A list of operations that rename strings",
                null);
        registerParameterType("integer-renaming-list", IntegerRenamingList.class,
                IntegerRenamingList::new,
                l -> new IntegerRenamingList((IntegerRenamingList) l),
                "List of integer replacement operations",
                "A list of operations that replace integers",
                null);

        // Matrix parameters
        registerParameterEditor(Matrix2DParameter.class, Matrix2DParameterEditorUI.class);
        registerParameterType("matrix2d-float",
                Matrix2DFloatParameter.class,
                Matrix2DFloatParameter::new,
                p -> new Matrix2DFloatParameter((Matrix2DFloatParameter) p),
                "2D matrix (float)",
                "A matrix containing float numbers",
                null);

        // Enums
        registerEnumParameterType("path-filter:mode", PathFilter.Mode.class, "Mode", "Available modes");
        registerEnumParameterType("string-filter:mode", StringFilter.Mode.class, "Mode", "Available modes");
        registerEnumParameterType("acaq:iterating-algorithm:column-matching", ACAQIteratingAlgorithm.ColumnMatching.class, "Column matching strategy", "Determines how columns for dataset matching are selected");
        registerEnumParameterType("acaq:ui:graph-editor-view-mode", ACAQAlgorithmGraphCanvasUI.ViewMode.class, "Graph editor view mode", "Determines how the graphs are displayed");

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

    private void registerColorJsonSerializer() {
        // Serializer for color type
        SimpleModule module = new SimpleModule();
        module.addSerializer(Color.class, new ColorSerializer());
        module.addDeserializer(Color.class, new ColorDeserializer());
        JsonUtils.getObjectMapper().registerModule(module);
    }
}
