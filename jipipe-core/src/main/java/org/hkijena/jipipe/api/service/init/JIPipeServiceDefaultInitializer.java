package org.hkijena.jipipe.api.service.init;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.service.components.nodes.JIPipeNodeRegistrationTask;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializer;
import org.hkijena.jipipe.api.service.JIPipeServiceState;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEvent;
import org.hkijena.jipipe.api.service.events.JIPipePluginRegisteredEvent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.JavaExtensionValidationReportContext;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDefaultDataViewer;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactAccelerationPreference;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.CUDAUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.AbstractService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeServiceDefaultInitializer extends JIPipeServiceInitializer {
    private final JIPipeExtensionApplicationSettings extensionSettings;
    private final JIPipeRegistryIssues issues = new JIPipeRegistryIssues();

    public JIPipeServiceDefaultInitializer(JIPipeService service) {
        super(service);
        this.extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
    }

    @Override
    public void run() {
        if(state != JIPipeServiceState.Uninitialized) {
            progressInfo.log("ERROR: JIPipe initialization has already been called");
            return;
        }
        state = JIPipeServiceState.Initializing;

        progressInfo.setProgress(0, 5);
        if (verbose) {
            progressInfo.getStatusUpdatedEventEmitter().subscribeLambda((emitter, event) -> {
                logService.info(event.getMessage());
            });
        }

        IJ.showStatus("Initializing JIPipe ...");
        nodeRegistry.installEvents();
        List<PluginInfo<JIPipeJavaPlugin>> pluginList = pluginService.getPluginsOfType(JIPipeJavaPlugin.class).stream()
                .sorted(JIPipe::comparePlugins).collect(Collectors.toList());
        pluginRegistry.initialize(); // Init extension registry
        pluginRegistry.load();
        progressInfo.setProgress(1);
        progressInfo.log("Legacy settings conversion ...");
        copyTemplatesFromPropertiesToLegacyProfile(progressInfo.resolve("Legacy conversion"));
        if (applyProfileUpgrades(progressInfo.resolve("Preparing profiles"))) {
            progressInfo.log("Upgrade was applied. Reloading extension settings.");
            extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        }

        progressInfo.log("Pre-initialization phase ...");

        // Creating instances of extensions
        Map<String, JIPipeJavaExtensionInitializationInfo> allJavaExtensionsByID = new HashMap<>();

        for (PluginInfo<JIPipeJavaPlugin> pluginInfo : pluginList) {
            try {
                progressInfo.log("Creating instance of " + pluginInfo + " ...");
                JIPipeJavaPlugin extension = pluginInfo.createInstance();

                // Validate ID
                if (!isValidExtensionId(extension.getDependencyId())) {
                    System.err.println("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + ". REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");
                    progressInfo.log("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + ". REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");

                    continue;
                } else {
                    if (allJavaExtensionsByID.containsKey(extension.getDependencyId())) {
                        System.err.println("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder. REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");
                        progressInfo.log("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder. REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");

                        continue;
                    }
                }

                JIPipeJavaExtensionInitializationInfo initializationInfo = new JIPipeJavaExtensionInitializationInfo();
                initializationInfo.setPluginInfo(pluginInfo);
                initializationInfo.setInstance(extension);
                allJavaExtensionsByID.put(extension.getDependencyId(), initializationInfo);

            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(pluginInfo);
            }
        }

        // First loading check
        progressInfo.log("Determining extensions to be loaded ...");
        Set<String> impliedLoadedJavaExtensions = new HashSet<>();
        boolean impliedLoadedJavaExtensionsChanged;
        do {
            impliedLoadedJavaExtensionsChanged = false;
            for (JIPipeJavaExtensionInitializationInfo initializationInfo : allJavaExtensionsByID.values()) {
                JIPipeJavaPlugin extension = initializationInfo.getInstance();
                if (extension == null) {
                    continue;
                }
                if (extension.isCorePlugin() || impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    if (isValidExtensionId(extension.getDependencyId())) {
                        if (!impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                            progressInfo.log("-> Core/User: " + extension.getDependencyId());
                            impliedLoadedJavaExtensions.add(extension.getDependencyId());
                            impliedLoadedJavaExtensionsChanged = true;
                        }
                    }
                    for (JIPipeDependency dependency : extension.getDependencies()) {
                        if (isValidExtensionId(dependency.getDependencyId())) {
                            if (!impliedLoadedJavaExtensions.contains(dependency.getDependencyId())) {
                                impliedLoadedJavaExtensions.add(dependency.getDependencyId());
                                impliedLoadedJavaExtensionsChanged = true;
                                progressInfo.log("-> Required by " + extension.getDependencyId() + ": " + dependency.getDependencyId());
                            }
                        }
                    }
                }
            }
        }
        while (impliedLoadedJavaExtensionsChanged);

        boolean preActivationScheduledSave = false;

        ImmutableList<JIPipeJavaExtensionInitializationInfo> allJavaExtensionsList = ImmutableList.copyOf(allJavaExtensionsByID.values());
        for (int i = 0; i < allJavaExtensionsList.size(); i++) {
            JIPipeJavaExtensionInitializationInfo initializationInfo = allJavaExtensionsList.get(i);
            IJ.showProgress(i + 1, allJavaExtensionsList.size());
            JIPipeJavaPlugin extension = initializationInfo.getInstance();

            if (extension == null) {
                continue;
            }
            try {
                pluginRegistry.registerKnownPlugin(extension);

                // Check if the extension should be loaded
                if (!extension.isCorePlugin() && pluginRegistry.getSettings().getDeactivatedPlugins().contains(extension.getDependencyId()) && !impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    progressInfo.log("Extension with ID " + extension.getDependencyId() + " will not be loaded (deactivated in extension manager)");
                    initializationInfo.setLoaded(false);
                    continue;
                }

                // Extension self-check
                JIPipeValidationReport preActivationIssues = new JIPipeValidationReport();
                issues.getPreActivationIssues().put(extension.getDependencyId(), preActivationIssues);
                if (!extension.canActivate(preActivationIssues, progressInfo.resolve("Pre-activation check").resolve(extension.getDependencyId()))) {
                    if (!extensionSettings.isIgnorePreActivationChecks()) {
                        new JavaExtensionValidationReportContext(extension).warning()
                                .title("Extension '" + extension.getMetadata().getName() + "' refuses to activate!")
                                .explanation("The extension's pre-activation check failed. It will not be activated. Please refer to the other items if available.")
                                .report(preActivationIssues);
                        progressInfo.log("Extension with ID " + extension.getDependencyId() + " will not be loaded (pre-activation check failed; extension refuses to activate)");
                        initializationInfo.setLoaded(false);
                        if (!StringUtils.isNullOrEmpty(extension.getDependencyId())) {
                            progressInfo.log("Extension with ID " + extension.getDependencyId() + " was removed from the list of activated extensions");
                            pluginRegistry.getSettings().getDeactivatedPlugins().add(extension.getDependencyId());
                            preActivationScheduledSave = true;
                        }
                        continue;
                    } else {
                        progressInfo.log("Extension with ID " + extension.getDependencyId() + " indicated that its pre-activation checks failed. WILL BE LOADED anyway DUE TO APPLICATION SETTINGS!");
                    }
                }

                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                initializationInfo.setLoaded(true);
                extensionDiscoveredEventEmitter.emit(new JIPipePluginDiscoveredEvent(this, extension));
            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(initializationInfo.getPluginInfo());
            }
        }

        // Save extension settings
        if (preActivationScheduledSave && !NO_SETTINGS_AUTOSAVE) {
            pluginRegistry.save();
        }

        progressInfo.setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = progressInfo.resolveAndLog("Register features");
        for (int i = 0; i < allJavaExtensionsList.size(); i++) {
            JIPipeJavaExtensionInitializationInfo initializationInfo = allJavaExtensionsList.get(i);

            if (!initializationInfo.isLoaded()) {
                registerFeaturesProgress.log("Skipping (deactivated in extension manager or is refusing to activate)");
                continue;
            }

            PluginInfo<JIPipeJavaPlugin> info = initializationInfo.getPluginInfo();

            IJ.showProgress(i + 1, allJavaExtensionsList.size());
            registerFeaturesProgress.log("Registering plugin " + info);
            JIPipeJavaPlugin extension = null;
            try {
                extension = initializationInfo.getInstance();
                registerFeaturesProgress.log("ID=" + extension.getDependencyId());
                extension.register(this, getContext(), progressInfo.resolve(extension.getDependencyId()));
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                extensionRegisteredEventEmitter.emit(new JIPipePluginRegisteredEvent(this, extension));
            } catch (NoClassDefFoundError | Exception e) {
                progressInfo.log("[!] ERROR: Unable to instantiate extension " + info);
                e.printStackTrace();
                progressInfo.log(e.toString());
                issues.getErroneousPlugins().add(info);
                if (extension != null)
                    failedExtensions.add(extension);
            }
        }

        registerFeaturesProgress.log("Registering remaining " + nodeRegistry.getScheduledRegistrationTasks().size() + " features ...");
        for (JIPipeNodeRegistrationTask task : nodeRegistry.getScheduledRegistrationTasks()) {
            try {
                task.register();
            } catch (Throwable ex) {
                logService.error("Could not register: " + task.toString() + " -> " + ex);
                registerFeaturesProgress.log("Could not register: " + task + " -> " + ex);
            }
        }

        // Check for errors

        progressInfo.setProgress(3);
        progressInfo.log("Validating node types ...");
        validateDataTypes(issues);
        if (extensionSettings.isValidateNodeTypes()) {
            validateNodeTypes(issues);
        }
        progressInfo.log("Validating parameter types ...");
        validateParameterTypes(issues);

        // Create dependency graph
        progressInfo.log("Creating dependency graph ...");
        pluginRegistry.getDependencyGraph();

        // Create settings for default importers
        progressInfo.log("Creating dynamic settings ...");
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();
        registerNodeExamplesFromFileSystem();
        registerProjectTemplatesFromFileSystem();

        // Reload settings
        progressInfo.setProgress(4);
        progressInfo.log("Loading settings ...");
        applicationSettingsRegistry.reload();

        // Required as the reload deletes the allowed values
        updateDefaultImporterSettings();
        updateDefaultCacheDisplaySettings();

        // Postprocessing
        progressInfo.setProgress(5);
        JIPipeProgressInfo postprocessingProgress = progressInfo.resolveAndLog("Postprocessing");
        for (JIPipeDependency extension : registeredExtensions) {
            if (!failedExtensions.contains(extension) && extension instanceof JIPipeJavaPlugin) {
                ((JIPipeJavaPlugin) extension).postprocess(postprocessingProgress.resolveAndLog(extension.getDependencyId()));
            }
        }
        postprocessingProgress.log("Converting display operations to import operations ...");
        datatypeRegistry.convertDisplayOperationsToImportOperations();
        postprocessingProgress.log("Registering examples ...");
        nodeRegistry.executeScheduledRegisterExamples();
        postprocessingProgress.log("Registering extension-provided templates ...");
        nodeRegistry.executeScheduledRegisterTemplates();

        // Check recent projects and backups
        progressInfo.setProgress(6);
        progressInfo.log("Checking recent projects ...");
        recentProjectsRegistry.reload();
        recentProjectsRegistry.cleanup();
        recentProjectsRegistry.migrateFromLegacy();

        // Check artifacts
        progressInfo.setProgress(7);
        artifactsRegistry.updateCachedArtifacts(progressInfo.resolve("Updating artifacts"));

        // Check acceleration
        if (JIPipeArtifactApplicationSettings.getInstance().isAutoConfigureAccelerationOnNextStartup()) {
            progressInfo.log("Determining acceleration profile ...");
            try {

                if (CUDAUtils.hasCudaSupport()) {
                    progressInfo.log("Determining acceleration profile ... CUDA support detected");
                    JIPipeArtifactApplicationSettings.getInstance().setAccelerationPreference(JIPipeArtifactAccelerationPreference.CUDA);

                    try {
                        JIPipeArtifactApplicationSettings.getInstance().setAccelerationPreferenceVersions(new Vector2iParameter(
                                CUDAUtils.getMinimumCudaVersion(),
                                0  // Broken due to Nvidia-SMI hanging on Linux -> have to use 0
                        ));
                        progressInfo.log("Determined CUDA version limits as " + JIPipeArtifactApplicationSettings.getInstance().getAccelerationPreferenceVersions());
                    } catch (Exception e) {
                        progressInfo.log(e);
                    }
                }

                JIPipeArtifactApplicationSettings.getInstance().setAutoConfigureAccelerationOnNextStartup(false);
                applicationSettingsRegistry.save();
            } catch (Exception e) {
                progressInfo.log(e);
            }
        }

        // Load templates
        progressInfo.log("Loading node templates ...");
        nodeTemplateRegistry.reloadGlobalTemplates(progressInfo.resolve("Node templates"));

        progressInfo.setProgress(8);
        progressInfo.log("JIPipe loading finished");
        state = JIPipeServiceState.Initialized;

        // Check if we have viewers for everything
        for (Class<? extends JIPipeData> dataClass : datatypeRegistry.getRegisteredDataTypes().values()) {
            Class<? extends JIPipeDesktopDataViewer> defaultDataViewer = datatypeRegistry.getDefaultDataViewer(dataClass);
            if (defaultDataViewer == JIPipeDesktopDefaultDataViewer.class) {
                progressInfo.log("Info: Data type " + datatypeRegistry.getIdOf(dataClass) + " does not have a default data viewer");
            }
        }

        // Check for new extensions
        pluginRegistry.findNewPlugins();
        for (String newExtension : pluginRegistry.getNewPlugins()) {
            progressInfo.log("New extension found: " + newExtension);
        }

        // Push progress into log
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry("JIPipe initialization",
                LocalDateTime.now(),
                progressInfo.getLog().toString(),
                new JIPipeNotificationInbox(), true));

        // Mark log as read
        JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead();
    }
}
