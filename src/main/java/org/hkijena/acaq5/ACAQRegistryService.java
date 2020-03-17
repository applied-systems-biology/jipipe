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
@Plugin(type = ACAQService.class)
public class ACAQRegistryService extends AbstractService implements ACAQService {
    private static ACAQRegistryService instance;
    private List<ACAQExtensionService> registeredExtensions = new ArrayList<>();
    private ACAQAlgorithmRegistry algorithmRegistry = new ACAQAlgorithmRegistry();
    private ACAQDatatypeRegistry datatypeRegistry = new ACAQDatatypeRegistry();
    private ACAQTraitRegistry traitRegistry = new ACAQTraitRegistry();
    private ACAQUIDatatypeRegistry uiDatatypeRegistry = new ACAQUIDatatypeRegistry();
    private ACAQUIParametertypeRegistry uiParametertypeRegistry = new ACAQUIParametertypeRegistry();
    private ACAQUITraitRegistry acaquiTraitRegistry = new ACAQUITraitRegistry();
    private ACAQPlotBuilderRegistry plotBuilderRegistry = new ACAQPlotBuilderRegistry();
    private ACAQTableAnalyzerUIOperationRegistry tableAnalyzerUIOperationRegistry = new ACAQTableAnalyzerUIOperationRegistry();

    /**
     * Discovers extension services that provide new ACAQ5 modules
     *
     * @param pluginService
     */
    private void discover(PluginService pluginService) {
        for (PluginInfo<ACAQExtensionService> info : pluginService.getPluginsOfType(ACAQExtensionService.class).stream()
                .sorted(ACAQRegistryService::comparePlugins).collect(Collectors.toList())) {
            System.out.println("ACAQ5: Registering plugin " + info);
            try {
                ACAQExtensionService service = info.createInstance();
                service.register(this);
                registeredExtensions.add(service);
            } catch (InstantiableException e) {
                throw new RuntimeException(e);
            }
        }
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

    public List<ACAQExtensionService> getRegisteredExtensions() {
        return Collections.unmodifiableList(registeredExtensions);
    }

    @Override
    public ACAQPlotBuilderRegistry getPlotBuilderRegistry() {
        return plotBuilderRegistry;
    }

    public ACAQTableAnalyzerUIOperationRegistry getTableAnalyzerUIOperationRegistry() {
        return tableAnalyzerUIOperationRegistry;
    }

    public static ACAQRegistryService getInstance() {
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
                instance = (ACAQRegistryService) pluginService.getPlugin(ACAQRegistryService.class).createInstance();
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
