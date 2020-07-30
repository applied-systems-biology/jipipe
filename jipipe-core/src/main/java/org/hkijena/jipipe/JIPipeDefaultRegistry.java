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
import net.imagej.ui.swing.updater.SwingAuthenticator;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.Progress;
import net.imagej.updater.util.UpdaterUtil;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.events.ExtensionRegisteredEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.ui.ijupdater.IJProgressAdapter;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.ui.registries.*;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

import java.net.Authenticator;
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
    private JIPipeNodeRegistry nodeRegistry = new JIPipeNodeRegistry();
    private JIPipeDatatypeRegistry datatypeRegistry = new JIPipeDatatypeRegistry();
    private JIPipeUIDatatypeRegistry uiDatatypeRegistry = new JIPipeUIDatatypeRegistry();
    private JIPipeUIParameterTypeRegistry uiParametertypeRegistry = new JIPipeUIParameterTypeRegistry();
    private JIPipeImageJAdapterRegistry imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry();
    private JIPipeUIImageJDatatypeAdapterRegistry uiImageJDatatypeAdapterRegistry = new JIPipeUIImageJDatatypeAdapterRegistry();
    private JIPipeUIMenuServiceRegistry uiMenuServiceRegistry = new JIPipeUIMenuServiceRegistry();
    private JIPipeParameterTypeRegistry parameterTypeRegistry = new JIPipeParameterTypeRegistry();
    private JIPipeSettingsRegistry settingsRegistry = new JIPipeSettingsRegistry();
    private JIPipeTableRegistry tableRegistry = new JIPipeTableRegistry();
    private JIPipeUINodeRegistry jipipeuiNodeRegistry = new JIPipeUINodeRegistry();
    private FilesCollection imageJPlugins = null;

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
        nodeRegistry = new JIPipeNodeRegistry();
        uiDatatypeRegistry = new JIPipeUIDatatypeRegistry();
        uiParametertypeRegistry = new JIPipeUIParameterTypeRegistry();
        imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry();
        uiImageJDatatypeAdapterRegistry = new JIPipeUIImageJDatatypeAdapterRegistry();
        uiMenuServiceRegistry = new JIPipeUIMenuServiceRegistry();
        parameterTypeRegistry = new JIPipeParameterTypeRegistry();
        settingsRegistry = new JIPipeSettingsRegistry();
        tableRegistry = new JIPipeTableRegistry();
        jipipeuiNodeRegistry = new JIPipeUINodeRegistry();
        discover(ExtensionSettings.getInstanceFromRaw(), new JIPipeRegistryIssues());
    }

    /**
     * Discovers extension services that provide new JIPipe modules
     *
     * @param extensionSettings extension settings
     * @param issues            if no windows should be opened
     */
    private void discover(ExtensionSettings extensionSettings, JIPipeRegistryIssues issues) {
        IJ.showStatus("Initializing JIPipe ...");
        List<PluginInfo<JIPipeJavaExtension>> pluginList = pluginService.getPluginsOfType(JIPipeJavaExtension.class).stream()
                .sorted(JIPipeDefaultRegistry::comparePlugins).collect(Collectors.toList());
        List<JIPipeDependency> javaExtensions = new ArrayList<>();
        System.out.println("[1/3] Pre-initialization phase ...");
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            try {
                JIPipeJavaExtension extension = info.createInstance();
                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                javaExtensions.add(extension);
            } catch (NoClassDefFoundError | InstantiableException e) {
                issues.getErroneousPlugins().add(info);
            }
        }

        System.out.println("[2/3] Registration-phase ...");
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            System.out.println("JIPipe: Registering plugin " + info);
            try {
                JIPipeJavaExtension extension = (JIPipeJavaExtension) javaExtensions.get(i);
                extension.register();
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                eventBus.post(new ExtensionRegisteredEvent(this, extension));
            } catch (NoClassDefFoundError | Exception e) {
                issues.getErroneousPlugins().add(info);
            }
        }

        for (JIPipeNodeRegistrationTask task : nodeRegistry.getScheduledRegistrationTasks()) {
            System.err.println("Could not register: " + task.toString());
        }

        // Check for errors
        System.out.println("[3/3] Error-checking-phase ...");
        if (extensionSettings.isValidateNodeTypes()) {
            for (JIPipeNodeInfo info : nodeRegistry.getRegisteredNodeInfos().values()) {
                try {
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
                    System.out.println("OK: Algorithm '" + info.getId() + "'");
                } catch (NoClassDefFoundError | Exception e) {
                    issues.getErroneousNodes().add(info);
                    e.printStackTrace();
                }
            }
        }

        // Check for update sites

        if (extensionSettings.isValidateImageJDependencies())
            checkUpdateSites(issues, javaExtensions, new IJProgressAdapter());

        // Reload settings
        System.out.println("Loading settings ...");
        settingsRegistry.reload();
        System.out.println("JIPipe loading finished");
    }

    /**
     * Checks the update sites of all extensions and stores the results in the issues
     * @param issues the results
     * @param extensions list of known extensions
     * @param progressAdapter the adapter that takes the progress
     */
    public void checkUpdateSites(JIPipeRegistryIssues issues, List<JIPipeDependency> extensions, Progress progressAdapter) {
        Set<JIPipeImageJUpdateSiteDependency> dependencies = new HashSet<>();
        Set<JIPipeImageJUpdateSiteDependency> missingSites = new HashSet<>();
        for (JIPipeDependency extension : extensions) {
            dependencies.addAll(extension.getImageJUpdateSiteDependencies());
            missingSites.addAll(extension.getImageJUpdateSiteDependencies());
        }
        if (!dependencies.isEmpty()) {
            System.out.println("Following ImageJ update site dependencies were requested: ");
            for (JIPipeImageJUpdateSiteDependency dependency : dependencies) {
                System.out.println("  - " + dependency.getName() + " @ " + dependency.getUrl());
            }
            try {
                UpdaterUtil.useSystemProxies();
                Authenticator.setDefault(new SwingAuthenticator());

                imageJPlugins = new FilesCollection(JIPipeImageJPluginManager.getImageJRoot().toFile());
                AvailableSites.initializeAndAddSites(imageJPlugins);
                imageJPlugins.downloadIndexAndChecksum(progressAdapter);
            } catch (Exception e) {
                System.err.println("Unable to check update sites!");
                e.printStackTrace();
                missingSites.clear();
                System.out.println("No ImageJ update site check is applied.");
            }
            if (imageJPlugins != null) {
                System.out.println("Following ImageJ update sites are currently active: ");
                for (UpdateSite updateSite : imageJPlugins.getUpdateSites(true)) {
                    if (updateSite.isActive()) {
                        System.out.println("  - " + updateSite.getName() + " @ " + updateSite.getURL());
                        missingSites.removeIf(site -> Objects.equals(site.getName(), updateSite.getName()));
                    }
                }
            } else {
                System.err.println("No update sites available! Skipping.");
                missingSites.clear();
            }
        }

        if (!missingSites.isEmpty()) {
            System.out.println("Following ImageJ update site dependencies are missing: ");
            for (JIPipeImageJUpdateSiteDependency dependency : missingSites) {
                System.out.println("  - " + dependency.getName() + " @ " + dependency.getUrl());
            }
        }

        issues.setMissingImageJSites(missingSites);
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
    public JIPipeNodeRegistry getNodeRegistry() {
        return nodeRegistry;
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
        nodeRegistry.installEvents();
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
        report.forCategory("Algorithms").report(nodeRegistry);
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
    public JIPipeUINodeRegistry getUIAlgorithmRegistry() {
        return jipipeuiNodeRegistry;
    }

    public FilesCollection getImageJPlugins() {
        return imageJPlugins;
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
     * @param context           the SciJava context
     * @param extensionSettings extension settings
     * @param issues            registration issues
     */
    public static void instantiate(Context context, ExtensionSettings extensionSettings, JIPipeRegistryIssues issues) {
        if (instance == null) {
            try {
                PluginService pluginService = context.getService(PluginService.class);
                instance = (JIPipeDefaultRegistry) pluginService.getPlugin(JIPipeDefaultRegistry.class).createInstance();
                context.inject(instance);
                instance.setContext(context);
                instance.installEvents();
                instance.discover(extensionSettings, issues);
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
