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

package org.hkijena.jipipe;

import com.google.common.eventbus.EventBus;
import ij.IJ;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.events.ExtensionRegisteredEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.ui.registries.*;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A scijava service that discovers JIPipe plugins in the classpath
 */
@Plugin(type = JIPipeRegistry.class)
public class JIPipeDefaultRegistry extends AbstractService implements JIPipeRegistry {
    private static JIPipeDefaultRegistry instance;
    private EventBus eventBus = new EventBus();
    private Set<String> registeredExtensionIds = new HashSet<>();
    private List<JIPipeDependency> registeredExtensions = new ArrayList<>();
    private JIPipeAlgorithmRegistry algorithmRegistry = new JIPipeAlgorithmRegistry();
    private JIPipeDatatypeRegistry datatypeRegistry = new JIPipeDatatypeRegistry();
    private JIPipeUIDatatypeRegistry uiDatatypeRegistry = new JIPipeUIDatatypeRegistry();
    private JIPipeUIParameterTypeRegistry uiParametertypeRegistry = new JIPipeUIParameterTypeRegistry();
    private JIPipeImageJAdapterRegistry imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry();
    private JIPipeUIImageJDatatypeAdapterRegistry uiImageJDatatypeAdapterRegistry = new JIPipeUIImageJDatatypeAdapterRegistry();
    private JIPipeUIMenuServiceRegistry uiMenuServiceRegistry = new JIPipeUIMenuServiceRegistry();
    private JIPipeParameterTypeRegistry parameterTypeRegistry = new JIPipeParameterTypeRegistry();
    private JIPipeSettingsRegistry settingsRegistry = new JIPipeSettingsRegistry();
    private JIPipeTableRegistry tableRegistry = new JIPipeTableRegistry();
    private JIPipeUIAlgorithmRegistry jipipeuiAlgorithmRegistry = new JIPipeUIAlgorithmRegistry();

    @Parameter
    private PluginService pluginService;


    /**
     * Create a new registry instance
     */
    public JIPipeDefaultRegistry() {
    }

    /**
     * Clears all registries and reloads them
     */
    public void reload() {
        System.out.println("JIPipe: Reloading registry service");
        registeredExtensions = new ArrayList<>();
        registeredExtensionIds = new HashSet<>();
        datatypeRegistry = new JIPipeDatatypeRegistry();
        algorithmRegistry = new JIPipeAlgorithmRegistry();
        uiDatatypeRegistry = new JIPipeUIDatatypeRegistry();
        uiParametertypeRegistry = new JIPipeUIParameterTypeRegistry();
        imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry();
        uiImageJDatatypeAdapterRegistry = new JIPipeUIImageJDatatypeAdapterRegistry();
        uiMenuServiceRegistry = new JIPipeUIMenuServiceRegistry();
        parameterTypeRegistry = new JIPipeParameterTypeRegistry();
        settingsRegistry = new JIPipeSettingsRegistry();
        tableRegistry = new JIPipeTableRegistry();
        jipipeuiAlgorithmRegistry = new JIPipeUIAlgorithmRegistry();
        discover();
    }

    /**
     * Discovers extension services that provide new JIPipe modules
     */
    private void discover() {
        IJ.showStatus("Initializing JIPipe ...");
        List<PluginInfo<JIPipeJavaExtension>> pluginList = pluginService.getPluginsOfType(JIPipeJavaExtension.class).stream()
                .sorted(JIPipeDefaultRegistry::comparePlugins).collect(Collectors.toList());
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            System.out.println("JIPipe: Registering plugin " + info);
            try {
                JIPipeJavaExtension extension = info.createInstance();
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
                        "JIPipe plugin registry", "There is an error in the plugin's code that prevents it from being loaded.",
                        "Please contact the plugin author for further help.");
            }
        }

        for (JIPipeAlgorithmRegistrationTask task : algorithmRegistry.getScheduledRegistrationTasks()) {
            System.err.println("Could not register: " + task.toString());
        }

        // Check for errors
        for (JIPipeNodeInfo info : algorithmRegistry.getRegisteredAlgorithms().values()) {
            JIPipeGraphNode algorithm = info.newInstance();
            JIPipeParameterTree collection = new JIPipeParameterTree(algorithm);
            for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
                if (JIPipeParameterTypeRegistry.getInstance().getInfoByFieldClass(entry.getValue().getFieldClass()) == null) {
                    throw new UserFriendlyRuntimeException("Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                            + algorithm + " -> " + entry.getKey(),
                            "A plugin is invalid!",
                            "JIPipe plugin checker",
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
    public void register(JIPipeJsonExtension extension) {
        System.out.println("JIPipe: Registering Json Extension " + extension.getDependencyId());
        extension.setRegistry(this);
        extension.register();
        registeredExtensions.add(extension);
        registeredExtensionIds.add(extension.getDependencyId());
        eventBus.post(new ExtensionRegisteredEvent(this, extension));
    }

    @Override
    public JIPipeAlgorithmRegistry getAlgorithmRegistry() {
        return algorithmRegistry;
    }

    @Override
    public JIPipeDatatypeRegistry getDatatypeRegistry() {
        return datatypeRegistry;
    }

    @Override
    public JIPipeUIDatatypeRegistry getUIDatatypeRegistry() {
        return uiDatatypeRegistry;
    }

    @Override
    public JIPipeUIParameterTypeRegistry getUIParameterTypeRegistry() {
        return uiParametertypeRegistry;
    }

    @Override
    public JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry() {
        return imageJDataAdapterRegistry;
    }

    @Override
    public List<JIPipeDependency> getRegisteredExtensions() {
        return Collections.unmodifiableList(registeredExtensions);
    }

    @Override
    public JIPipeUIImageJDatatypeAdapterRegistry getUIImageJDatatypeAdapterRegistry() {
        return uiImageJDatatypeAdapterRegistry;
    }

    @Override
    public JIPipeUIMenuServiceRegistry getUIMenuServiceRegistry() {
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
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Algorithms").report(algorithmRegistry);
        for (JIPipeDependency extension : registeredExtensions) {
            report.forCategory("Extensions").forCategory(extension.getDependencyId()).report(extension);
        }
    }

    @Override
    public JIPipeDependency findExtensionById(String dependencyId) {
        return registeredExtensions.stream().filter(d -> Objects.equals(dependencyId, d.getDependencyId())).findFirst().orElse(null);
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    @Override
    public JIPipeParameterTypeRegistry getParameterTypeRegistry() {
        return parameterTypeRegistry;
    }

    @Override
    public JIPipeSettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    @Override
    public JIPipeTableRegistry getTableRegistry() {
        return tableRegistry;
    }

    @Override
    public JIPipeUIAlgorithmRegistry getUIAlgorithmRegistry() {
        return jipipeuiAlgorithmRegistry;
    }

    /**
     * @return Singleton instance
     */
    public static JIPipeDefaultRegistry getInstance() {
        return instance;
    }

    public static boolean isInstantiated() {
        return instance != null;
    }

    /**
     * Instantiates the plugin service. This is done within {@link JIPipeGUICommand}
     *
     * @param context the SciJava context
     */
    public static void instantiate(Context context) {
        if (instance == null) {
            try {
                PluginService pluginService = context.getService(PluginService.class);
                instance = (JIPipeDefaultRegistry) pluginService.getPlugin(JIPipeDefaultRegistry.class).createInstance();
                context.inject(instance);
                instance.setContext(context);
                instance.installEvents();
                instance.discover();
            } catch (InstantiableException e) {
                throw new UserFriendlyRuntimeException(e, "Could not create essential JIPipe data structures.",
                        "JIPipe plugin registry", "There seems to be an issue either with JIPipe or your ImageJ installation.",
                        "Try to install JIPipe into a new ImageJ distribution and one-by-one install additional plugins. " +
                                "Contact the JIPipe or plugin author if you cannot resolve the issue.");
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
