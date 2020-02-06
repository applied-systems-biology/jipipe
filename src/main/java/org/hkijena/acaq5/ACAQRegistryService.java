package org.hkijena.acaq5;

import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.scijava.InstantiableException;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

/**
 * A scijava service that discovers ACAQ5 plugins in the classpath
 */
@Plugin(type=ACAQService.class)
public class ACAQRegistryService extends AbstractService implements ACAQService {
    private static ACAQRegistryService instance;

    public static ACAQRegistryService getInstance() {
        return instance;
    }

    private ACAQAlgorithmRegistry algorithmRegistry = new ACAQAlgorithmRegistry();
    private ACAQDatatypeRegistry datatypeRegistry = new ACAQDatatypeRegistry();
    private ACAQUIDatatypeRegistry uiDatatypeRegistry = new ACAQUIDatatypeRegistry();

    /**
     * Instantiates the plugin service. This is done within {@link ACAQCommand}
     * @param pluginService
     */
    public static void instantiate(PluginService pluginService) {
        try {
            instance = (ACAQRegistryService) pluginService.getPlugin(ACAQRegistryService.class).createInstance();
            instance.discover(pluginService);
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Discovers extension services that provide new ACAQ5 modules
     * @param pluginService
     */
    private void discover(PluginService pluginService) {
        for(PluginInfo<ACAQExtensionService> info : pluginService.getPluginsOfType(ACAQExtensionService.class)) {
            System.out.println("ACAQ5: Registering plugin " + info);
            try {
                ACAQExtensionService service = (ACAQExtensionService)info.createInstance();
                service.register(this);
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
}
