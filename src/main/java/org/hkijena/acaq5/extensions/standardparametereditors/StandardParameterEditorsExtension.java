package org.hkijena.acaq5.extensions.standardparametereditors;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.api.parameters.PathCollection;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefCollection;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.standardalgorithms.api.macro.MacroCode;
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
    public void register(ACAQDefaultRegistry registryService) {
        // Register parameter editor UIs
        registryService.getUIParametertypeRegistry().registerParameterEditor(Path.class, FilePathParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(int.class, IntegerParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(double.class, DoubleParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(float.class, FloatParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(boolean.class, BooleanParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(Integer.class, IntegerParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(Double.class, DoubleParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(Float.class, FloatParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(Boolean.class, BooleanParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(String.class, StringParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(MacroCode.class, MacroParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(Enum.class, EnumParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(CollectionParameter.class, CollectionParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(PathCollection.class, PathCollectionParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(PathFilter.class, PathFilterParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(ACAQTraitDeclarationRef.class, ACAQTraitDeclarationRefParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(ACAQTraitDeclarationRefCollection.class, ACAQTraitDeclarationRefCollectionParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(ACAQTrait.class, ACAQTraitParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(ACAQParameterCollectionVisibilities.class, ACAQParameterCollectionVisibilitiesParameterEditorUI.class);
    }
}
