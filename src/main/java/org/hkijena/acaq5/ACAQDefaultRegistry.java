/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import ij.IJ;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQSettingsRegistry;
import org.hkijena.acaq5.api.registries.ACAQTableRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIImageJDatatypeAdapterRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIMenuServiceRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private ACAQUIDatatypeRegistry uiDatatypeRegistry = new ACAQUIDatatypeRegistry();
    private ACAQUIParameterTypeRegistry uiParametertypeRegistry = new ACAQUIParameterTypeRegistry();
    private ACAQImageJAdapterRegistry imageJDataAdapterRegistry = new ACAQImageJAdapterRegistry();
    private ACAQUIImageJDatatypeAdapterRegistry uiImageJDatatypeAdapterRegistry = new ACAQUIImageJDatatypeAdapterRegistry();
    private ACAQUIMenuServiceRegistry uiMenuServiceRegistry = new ACAQUIMenuServiceRegistry();
    private ACAQParameterTypeRegistry parameterTypeRegistry = new ACAQParameterTypeRegistry();
    private ACAQSettingsRegistry settingsRegistry = new ACAQSettingsRegistry();
    private ACAQTableRegistry tableRegistry = new ACAQTableRegistry();
    private ACAQUIAlgorithmRegistry acaquiAlgorithmRegistry = new ACAQUIAlgorithmRegistry();

    @Parameter
    private PluginService pluginService;


    /**
     * Create a new registry instance
     */
    public ACAQDefaultRegistry() {
    }

    /**
     * Clears all registries and reloads them
     */
    public void reload() {
        System.out.println("ACAQ5: Reloading registry service");
        registeredExtensions = new ArrayList<>();
        registeredExtensionIds = new HashSet<>();
        datatypeRegistry = new ACAQDatatypeRegistry();
        algorithmRegistry = new ACAQAlgorithmRegistry();
        uiDatatypeRegistry = new ACAQUIDatatypeRegistry();
        uiParametertypeRegistry = new ACAQUIParameterTypeRegistry();
        imageJDataAdapterRegistry = new ACAQImageJAdapterRegistry();
        uiImageJDatatypeAdapterRegistry = new ACAQUIImageJDatatypeAdapterRegistry();
        uiMenuServiceRegistry = new ACAQUIMenuServiceRegistry();
        parameterTypeRegistry = new ACAQParameterTypeRegistry();
        settingsRegistry = new ACAQSettingsRegistry();
        tableRegistry = new ACAQTableRegistry();
        acaquiAlgorithmRegistry = new ACAQUIAlgorithmRegistry();
        discover();
    }

    /**
     * Discovers extension services that provide new ACAQ5 modules
     */
    private void discover() {
        IJ.showStatus("Initializing ACAQ5 ...");
        List<PluginInfo<ACAQJavaExtension>> pluginList = pluginService.getPluginsOfType(ACAQJavaExtension.class).stream()
                .sorted(ACAQDefaultRegistry::comparePlugins).collect(Collectors.toList());
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<ACAQJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            System.out.println("ACAQ5: Registering plugin " + info);
            try {
                ACAQJavaExtension extension = info.createInstance();
                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                extension.register();
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                eventBus.post(new ExtensionRegisteredEvent(this, extension));
            } catch (InstantiableException e) {
                throw new UserFriendlyRuntimeException(e, "A plugin could be be registered.",
                        "ACAQ plugin registry", "There is an error in the plugin's code that prevents it from being loaded.",
                        "Please contact the plugin author for further help.");
            }
        }

        for (ACAQAlgorithmRegistrationTask task : algorithmRegistry.getScheduledRegistrationTasks()) {
            System.err.println("Could not register: " + task.toString());
        }

        // Check for errors
        for (ACAQAlgorithmDeclaration declaration : algorithmRegistry.getRegisteredAlgorithms().values()) {
            ACAQGraphNode algorithm = declaration.newInstance();
            ACAQParameterTree collection = new ACAQParameterTree(algorithm);
            for (Map.Entry<String, ACAQParameterAccess> entry : collection.getParameters().entrySet()) {
                if (ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(entry.getValue().getFieldClass()) == null) {
                    throw new UserFriendlyRuntimeException("Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                            + algorithm + " -> " + entry.getKey(),
                            "A plugin is invalid!",
                            "ACAQ plugin checker",
                            "There is an error in the plugin's code that makes it use an unsupported parameter type.",
                            "Please contact the plugin author for further help.");
                }
            }
        }

        // Reload settings
        settingsRegistry.reload();

    }

    /**
     * Registers a JSON extension
     *
     * @param extension The extension
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
    public ACAQUIParameterTypeRegistry getUIParameterTypeRegistry() {
        return uiParametertypeRegistry;
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
    public ACAQUIImageJDatatypeAdapterRegistry getUIImageJDatatypeAdapterRegistry() {
        return uiImageJDatatypeAdapterRegistry;
    }

    @Override
    public ACAQUIMenuServiceRegistry getUIMenuServiceRegistry() {
        return uiMenuServiceRegistry;
    }

    private void installEvents() {
        algorithmRegistry.installEvents();
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Algorithms").report(algorithmRegistry);
        for (ACAQDependency extension : registeredExtensions) {
            report.forCategory("Extensions").forCategory(extension.getDependencyId()).report(extension);
        }
    }

    @Override
    public ACAQDependency findExtensionById(String dependencyId) {
        return registeredExtensions.stream().filter(d -> Objects.equals(dependencyId, d.getDependencyId())).findFirst().orElse(null);
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    @Override
    public ACAQParameterTypeRegistry getParameterTypeRegistry() {
        return parameterTypeRegistry;
    }

    @Override
    public ACAQSettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    @Override
    public ACAQTableRegistry getTableRegistry() {
        return tableRegistry;
    }

    @Override
    public ACAQUIAlgorithmRegistry getUIAlgorithmRegistry() {
        return acaquiAlgorithmRegistry;
    }

    /**
     * @return Singleton instance
     */
    public static ACAQDefaultRegistry getInstance() {
        return instance;
    }

    public static boolean isInstantiated() {
        return instance != null;
    }

    /**
     * Instantiates the plugin service. This is done within {@link ACAQGUICommand}
     *
     * @param context the SciJava context
     */
    public static void instantiate(Context context) {
        if (instance == null) {
            try {
                PluginService pluginService = context.getService(PluginService.class);
                instance = (ACAQDefaultRegistry) pluginService.getPlugin(ACAQDefaultRegistry.class).createInstance();
                context.inject(instance);
                instance.setContext(context);
                instance.installEvents();
                instance.discover();
            } catch (InstantiableException e) {
                throw new UserFriendlyRuntimeException(e, "Could not create essential ACAQ5 data structures.",
                        "ACAQ plugin registry", "There seems to be an issue either with ACAQ5 or your ImageJ installation.",
                        "Try to install ACAQ5 into a new ImageJ distribution and one-by-one install additional plugins. " +
                                "Contact the ACAQ5 or plugin author if you cannot resolve the issue.");
            }
        }
    }

    /**
     * Compares two plugins and sorts them by priority
     *
     * @param p0 Plugin
     * @param p1 Plugin
     * @return Comparator result
     */
    public static int comparePlugins(PluginInfo<?> p0, PluginInfo<?> p1) {
        return -Double.compare(p0.getPriority(), p1.getPriority());
    }
}
