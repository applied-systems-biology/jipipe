package org.hkijena.acaq5.extensions.parametereditors;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.api.data.ACAQDataDeclarationRef;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefCollection;
import org.hkijena.acaq5.api.traits.ACAQTraitIconRef;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.parametereditors.editors.*;
import org.hkijena.acaq5.extensions.parametereditors.generators.*;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.nio.file.Path;

/**
 * Provides some standard parameters
 */
@Plugin(type = ACAQJavaExtension.class, priority = Priority.FIRST)
public class StandardParameterEditorsExtension extends ACAQPrepackagedDefaultJavaExtension {

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
        registerParameterType(boolean.class, BooleanParameterEditorUI.class, "Boolean value", "A boolean value (true/false)");
        registerParameterType(Boolean.class, BooleanParameterEditorUI.class, "Boolean value", "A boolean value (true/false)");

        // Register numbers
        registerParameterType(byte.class, NumberParameterEditorUI.class, "8-bit integer number", "A 8-bit integral number ranging from " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
        registerParameterType(short.class, NumberParameterEditorUI.class, "16-bit integer number", "A 16-bit integral number ranging from " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
        registerParameterType(int.class, NumberParameterEditorUI.class, "Integer number", "A 32-bit integral number ranging from " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
        registerParameterType(long.class, NumberParameterEditorUI.class, "64-bit integer number", "A 64-bit integral number ranging from " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
        registerParameterType(float.class, NumberParameterEditorUI.class, "Floating point number (single)", "A floating point number with single precision");
        registerParameterType(double.class, NumberParameterEditorUI.class, "Floating point number (double)", "A floating point number with double precision");
        registerParameterType(Byte.class, NumberParameterEditorUI.class, "8-bit integer number", "A 8-bit integral number ranging from " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
        registerParameterType(Short.class, NumberParameterEditorUI.class, "16-bit integer number", "A 16-bit integral number ranging from " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
        registerParameterType(Integer.class, NumberParameterEditorUI.class, "Integer number", "A integral number ranging from " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
        registerParameterType(Long.class, NumberParameterEditorUI.class, "64-bit integer number", "A 64-bit integral number ranging from " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
        registerParameterType(Float.class, NumberParameterEditorUI.class, "Floating point number (single)", "A floating point number with single precision");
        registerParameterType(Double.class, NumberParameterEditorUI.class, "Floating point number (double)", "A floating point number with double precision");

        // Register other common Java classes
        registerParameterType(Enum.class, EnumParameterEditorUI.class, null, "A selection of different values");
        registerParameterType(String.class, StringParameterEditorUI.class, "String", "A text value");
        registerParameterType(Path.class, FilePathParameterEditorUI.class, "Filesystem path", "A path");
        registerParameterType(File.class, FileParameterEditorUI.class, "Filesystem path (legacy)", "A path (legacy)");

        // Register custom ACAQ5 parameters
        registerParameterType(PathCollection.class, PathCollectionParameterEditorUI.class, "Path collection", "A list of multiple filesystem paths");
        registerParameterType(PathFilter.class, PathFilterParameterEditorUI.class, "Path filter", "A filter for filenames or folder names");
        registerParameterType(StringFilter.class, StringFilterParameterEditorUI.class, "String filter", "A filter for strings");
        registerParameterType(ACAQTraitDeclarationRef.class, ACAQTraitDeclarationRefParameterEditorUI.class, "Annotation type", "An ACAQ5 annotation type");
        registerParameterType(ACAQTraitDeclarationRefCollection.class, ACAQTraitDeclarationRefCollectionParameterEditorUI.class, "Annotation type collection", "A list of ACAQ5 annotation types");
        registerParameterType(ACAQTrait.class, ACAQTraitParameterEditorUI.class, "Annotation", "An ACAQ5 annotation");
        registerParameterType(ACAQDataDeclarationRef.class, ACAQDataDeclarationRefParameterEditorUI.class, "Data type", "An ACAQ5 data type");
        registerParameterType(ACAQParameterCollectionVisibilities.class, ACAQParameterCollectionVisibilitiesParameterEditorUI.class, "Parameter visibilities", "ACAQ5 parameter visibilities");
        registerParameterType(ACAQTraitIconRef.class, ACAQTraitIconRefParameterEditorUI.class, "Annotation icon", "An icon of an annotation type");
        registerParameterType(CollectionParameter.class, CollectionParameterEditorUI.class, "Collection", "A collection of parameters");
        registerParameterType(ACAQAlgorithmDeclarationRef.class, ACAQAlgorithmDeclarationRefParameterEditorUI.class, "Algorithm type", "An algorithm type");
        registerParameterType(ParameterTable.class, ParameterTableEditorUI.class, "Parameter table", "A table that contains parameters");
        registerParameterType(DynamicEnumParameter.class, DynamicEnumParameterEditorUI.class, null, "A selection of different values");

        // Register generators
        registerParameterGenerator(byte.class, ByteParameterGenerator.class, "Generate 8-bit integral number sequence", "Generates 8-bit integral numbers");
        registerParameterGenerator(short.class, ShortParameterGenerator.class, "Generate 16-bit integral number sequence", "Generates 16-bit integral numbers");
        registerParameterGenerator(int.class, IntegerParameterGenerator.class, "Generate 32-bit integral number sequence", "Generates 32-bit integral numbers");
        registerParameterGenerator(long.class, LongParameterGenerator.class, "Generate 64-bit integral number sequence", "Generates 64-bit integral numbers");
        registerParameterGenerator(float.class, FloatParameterGenerator.class, "Generate single precision floating point number sequence", "Generates 32-bit floating point numbers");
        registerParameterGenerator(double.class, DoubleParameterGenerator.class, "Generate double precision floating point number sequence", "Generates 64-bit floating point numbers");
        registerParameterGenerator(Byte.class, ByteParameterGenerator.class, "Generate 8-bit integral number sequence", "Generates 8-bit integral numbers");
        registerParameterGenerator(Short.class, ShortParameterGenerator.class, "Generate 16-bit integral number sequence", "Generates 16-bit integral numbers");
        registerParameterGenerator(Integer.class, IntegerParameterGenerator.class, "Generate 32-bit integral number sequence", "Generates 32-bit integral numbers");
        registerParameterGenerator(Long.class, LongParameterGenerator.class, "Generate 64-bit integral number sequence", "Generates 64-bit integral numbers");
        registerParameterGenerator(Float.class, FloatParameterGenerator.class, "Generate single precision floating point number sequence", "Generates 32-bit floating point numbers");
        registerParameterGenerator(Double.class, DoubleParameterGenerator.class, "Generate double precision floating point number sequence", "Generates 64-bit floating point numbers");
    }
}
