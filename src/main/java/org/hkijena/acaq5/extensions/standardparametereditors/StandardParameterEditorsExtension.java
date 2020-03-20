package org.hkijena.acaq5.extensions.standardparametereditors;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollectionVisibilities;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.api.parameters.PathCollection;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefCollection;
import org.hkijena.acaq5.extensions.standardalgorithms.api.macro.MacroCode;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.*;
import org.hkijena.acaq5.utils.PathFilter;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class StandardParameterEditorsExtension extends AbstractService implements ACAQExtensionService {

    @Override
    public String getName() {
        return "ACAQ5 standard parameter editor user interfaces";
    }

    @Override
    public String getDescription() {
        return "Provides user interfaces that allow editing of parameters";
    }

    @Override
    public List<String> getAuthors() {
        return Arrays.asList("Zoltán Cseresnyés", "Ruman Gerst");
    }

    @Override
    public String getURL() {
        return "https://applied-systems-biology.github.io/acaq5/";
    }

    @Override
    public String getLicense() {
        return "BSD-2";
    }

    @Override
    public URL getIconURL() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public void register(ACAQRegistryService registryService) {
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
