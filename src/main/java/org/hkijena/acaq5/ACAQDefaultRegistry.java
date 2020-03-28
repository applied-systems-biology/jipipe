package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.api.registries.*;
import org.hkijena.acaq5.ui.registries.*;
import org.scijava.InstantiableException;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A scijava service that discovers ACAQ5 plugins in the classpath
 */
@Plugin(type = ACAQRegistry.class)
public class ACAQDefaultRegistry extends AbstractService implements ACAQRegistry {
    private static ACAQDefaultRegistry instance;
    private EventBus eventBus = new EventBus();
    private Set<String> registeredExtensionIds = new HashSet<>();
    private List<ACAQDependency> registeredExtensions = new ArrayList<>();
    private ACAQAlgorithmRegistry algorithmRegistry = new ACAQAlgorithmRegistry();
    private ACAQDatatypeRegistry datatypeRegistry = new ACAQDatatypeRegistry();
    private ACAQTraitRegistry traitRegistry = new ACAQTraitRegistry();
    private ACAQUIDatatypeRegistry uiDatatypeRegistry = new ACAQUIDatatypeRegistry();
    private ACAQUIParametertypeRegistry uiParametertypeRegistry = new ACAQUIParametertypeRegistry();
    private ACAQUITraitRegistry acaquiTraitRegistry = new ACAQUITraitRegistry();
    private ACAQPlotBuilderRegistry plotBuilderRegistry = new ACAQPlotBuilderRegistry();
    private ACAQTableAnalyzerUIOperationRegistry tableAnalyzerUIOperationRegistry = new ACAQTableAnalyzerUIOperationRegistry();
    private ACAQImageJAdapterRegistry imageJDataAdapterRegistry = new ACAQImageJAdapterRegistry();
    private PluginService pluginService;


    public ACAQDefaultRegistry() {
    }

    /**
     * Clears all registries and reloads them
     */
    public void reload() {
        System.out.println("ACAQ5: Reloading registry service");
        registeredExtensions = new ArrayList<>();
        registeredExtensionIds = new HashSet<>();
        traitRegistry = new ACAQTraitRegistry();
        datatypeRegistry = new ACAQDatatypeRegistry();
        algorithmRegistry = new ACAQAlgorithmRegistry();
        uiDatatypeRegistry = new ACAQUIDatatypeRegistry();
        uiParametertypeRegistry = new ACAQUIParametertypeRegistry();
        acaquiTraitRegistry = new ACAQUITraitRegistry();
        plotBuilderRegistry = new ACAQPlotBuilderRegistry();
        tableAnalyzerUIOperationRegistry = new ACAQTableAnalyzerUIOperationRegistry();
        imageJDataAdapterRegistry = new ACAQImageJAdapterRegistry();
        discover(pluginService);
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
                ACAQJavaExtension extension = info.createInstance();
                extension.setRegistry(this);
                extension.register();
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                eventBus.post(new ExtensionRegisteredEvent(this, extension));
            } catch (InstantiableException e) {
                throw new RuntimeException(e);
            }
        }

        for (ACAQAlgorithmRegistrationTask task : algorithmRegistry.getScheduledRegistrationTasks()) {
            System.err.println("Could not register: " + task.toString());
        }

    }

    /**
     * Registers a JSON extension
     *
     * @param extension
     */
    public void register(ACAQJsonExtension extension) {
        System.out.println("ACAQ5: Registering Json Extension " + extension.getDependencyId());
        extension.setRegistry(this);
        extension.register();
        registeredExtensions.add(extension);
        registeredExtensionIds.add(extension.getDependencyId());
        eventBus.post(new ExtensionRegisteredEvent(this, extension));
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

    @Override
    public ACAQImageJAdapterRegistry getImageJDataAdapterRegistry() {
        return imageJDataAdapterRegistry;
    }

    @Override
    public List<ACAQDependency> getRegisteredExtensions() {
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Algorithms").report(algorithmRegistry);
        report.forCategory("Annotations").report(traitRegistry);
        for (ACAQDependency extension : registeredExtensions) {
            report.forCategory("Extensions").forCategory(extension.getDependencyId()).report(extension);
        }
    }

    public ACAQDependency findExtensionById(String dependencyId) {
        return registeredExtensions.stream().filter(d -> Objects.equals(dependencyId, d.getDependencyId())).findFirst().orElse(null);
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
                instance.pluginService = pluginService;
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
