package org.hkijena.acaq5;

import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.ui.registries.*;
import org.scijava.InstantiableException;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A scijava service that discovers ACAQ5 plugins in the classpath
 */
@Plugin(type = ACAQRegistry.class)
public class ACAQDefaultRegistry extends AbstractService implements ACAQRegistry {
    private static ACAQDefaultRegistry instance;
    private List<ACAQJavaExtension> registeredExtensions;
    private ACAQAlgorithmRegistry algorithmRegistry;
    private ACAQDatatypeRegistry datatypeRegistry;
    private ACAQTraitRegistry traitRegistry;
    private ACAQUIDatatypeRegistry uiDatatypeRegistry;
    private ACAQUIParametertypeRegistry uiParametertypeRegistry;
    private ACAQUITraitRegistry acaquiTraitRegistry;
    private ACAQPlotBuilderRegistry plotBuilderRegistry;
    private ACAQTableAnalyzerUIOperationRegistry tableAnalyzerUIOperationRegistry;

    public ACAQDefaultRegistry() {
        registeredExtensions = new ArrayList<>();
        traitRegistry = new ACAQTraitRegistry();
        datatypeRegistry = new ACAQDatatypeRegistry();
        algorithmRegistry = new ACAQAlgorithmRegistry();
        uiDatatypeRegistry = new ACAQUIDatatypeRegistry();
        uiParametertypeRegistry = new ACAQUIParametertypeRegistry();
        acaquiTraitRegistry = new ACAQUITraitRegistry();
        plotBuilderRegistry = new ACAQPlotBuilderRegistry();
        tableAnalyzerUIOperationRegistry = new ACAQTableAnalyzerUIOperationRegistry();
    }

    /**
     * Discovers extension services that provide new ACAQ5 modules
     *
     * @param pluginService
     */
    private void discover(PluginService pluginService) {
        for (PluginInfo<ACAQJavaExtension> info : pluginService.getPluginsOfType(ACAQJavaExtension.class).stream()
                .sorted(ACAQDefaultRegistry::comparePlugins).collect(Collectors.toList())) {
            System.out.println("ACAQ5: Registering plugin " + info);
            try {
                ACAQJavaExtension service = info.createInstance();
                service.register(this);
                registeredExtensions.add(service);
            } catch (InstantiableException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Registers a JSON extension
     * @param extension
     */
    public void register(ACAQJsonExtension extension) {

    }

    @Override
    public ACAQAlgorithmRegistry getAlgorithmRegistry() {
        return algorithmRegistry;
    }

    @Override
    public ACAQDatatypeRegistry getDatatypeRegistry() {
        return datatypeRegistry;
    }

    @Override
    public ACAQUIDatatypeRegistry getUIDatatypeRegistry() {
        return uiDatatypeRegistry;
    }

    @Override
    public ACAQUIParametertypeRegistry getUIParametertypeRegistry() {
        return uiParametertypeRegistry;
    }

    @Override
    public ACAQTraitRegistry getTraitRegistry() {
        return traitRegistry;
    }

    @Override
    public ACAQUITraitRegistry getUITraitRegistry() {
        return acaquiTraitRegistry;
    }

    public List<ACAQJavaExtension> getRegisteredExtensions() {
        return Collections.unmodifiableList(registeredExtensions);
    }

    @Override
    public ACAQPlotBuilderRegistry getPlotBuilderRegistry() {
        return plotBuilderRegistry;
    }

    public ACAQTableAnalyzerUIOperationRegistry getTableAnalyzerUIOperationRegistry() {
        return tableAnalyzerUIOperationRegistry;
    }

    private void installEvents() {
        algorithmRegistry.installEvents();
    }

    public static ACAQDefaultRegistry getInstance() {
        return instance;
    }

    /**
     * Instantiates the plugin service. This is done within {@link ACAQGUICommand}
     *
     * @param pluginService
     */
    public static void instantiate(PluginService pluginService) {
        if (instance == null) {
            try {
                instance = (ACAQDefaultRegistry) pluginService.getPlugin(ACAQDefaultRegistry.class).createInstance();
                instance.installEvents();
                instance.discover(pluginService);
            } catch (InstantiableException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int comparePlugins(PluginInfo<?> p0, PluginInfo<?> p1) {
        return -Double.compare(p0.getPriority(), p1.getPriority());
    }
}
