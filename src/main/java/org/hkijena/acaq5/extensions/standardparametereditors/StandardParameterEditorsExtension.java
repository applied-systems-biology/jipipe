package org.hkijena.acaq5.extensions.standardparametereditors;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.parameters.PathCollection;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefCollection;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.*;
import org.hkijena.acaq5.utils.PathFilter;
import org.scijava.plugin.Plugin;

import java.nio.file.Path;

@Plugin(type = ACAQJavaExtension.class)
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
        // Register parameter editor UIs
        registerParameterType(Path.class, FilePathParameterEditorUI.class, "Filesystem path", "A path");
        registerParameterType(int.class, IntegerParameterEditorUI.class, "Integer number", "A integral number");
        registerParameterType(double.class, DoubleParameterEditorUI.class, "Floating point number (double)", "A floating point number with double precision");
        registerParameterType(float.class, FloatParameterEditorUI.class, "Floating point number (single)", "A floating point number with single precision");
        registerParameterType(boolean.class, BooleanParameterEditorUI.class, "Boolean value", "A boolean value (true/false)");
        registerParameterType(Integer.class, IntegerParameterEditorUI.class, "Integer number", "A integral number");
        registerParameterType(Double.class, DoubleParameterEditorUI.class, "Floating point number (double)", "A floating point number with double precision");
        registerParameterType(Float.class, FloatParameterEditorUI.class, "Floating point number (single)", "A floating point number with single precision");
        registerParameterType(Boolean.class, BooleanParameterEditorUI.class, "Boolean value", "A boolean value (true/false)");
        registerParameterType(String.class, StringParameterEditorUI.class, "String", "A text value");
        registerParameterType(Enum.class, EnumParameterEditorUI.class, null, "A selection of different values");
        registerParameterType(PathCollection.class, PathCollectionParameterEditorUI.class, "Path collection", "A list of multiple filesystem paths");
        registerParameterType(PathFilter.class, PathFilterParameterEditorUI.class, "Path filter", "A filter for filenames or folder names");
        registerParameterType(ACAQTraitDeclarationRef.class, ACAQTraitDeclarationRefParameterEditorUI.class, "Annotation type", "An ACAQ5 annotation type");
        registerParameterType(ACAQTraitDeclarationRefCollection.class, ACAQTraitDeclarationRefCollectionParameterEditorUI.class, "Annotation type collection", "A list of ACAQ5 annotation types");
        registerParameterType(ACAQTrait.class, ACAQTraitParameterEditorUI.class, "Annotation", "An ACAQ5 annotation");
        registerParameterType(ACAQParameterCollectionVisibilities.class, ACAQParameterCollectionVisibilitiesParameterEditorUI.class, "Parameter visibilities", "ACAQ5 parameter visibilities");
    }
}
