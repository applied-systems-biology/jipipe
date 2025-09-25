package org.hkijena.jipipe.api.service.init;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializer;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.api.service.components.nodes.JIPipeNodeRegistrationTask;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEvent;
import org.hkijena.jipipe.api.service.events.JIPipePluginRegisteredEvent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.JavaExtensionValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDefaultDataViewer;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactAccelerationPreference;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.CUDAUtils;
import org.hkijena.jipipe.utils.JIPipeUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.AbstractService;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.*;

public class JIPipeServiceDefaultInitializer extends JIPipeServiceInitializer {
    private final JIPipeInitializationReport issues = new JIPipeInitializationReport();

    public JIPipeServiceDefaultInitializer(JIPipeService service) {
        super(service);
    }

    @Override
    public void runInitialization() {
        JIPipeInitializationReport report = getService().getInitializationReport();
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();

        getProgressInfo().setProgress(0, 5);
        if (isVerbose()) {
            getProgressInfo().getStatusUpdatedEventEmitter().subscribeLambda((emitter, event) -> {
                getService().getLogService().info(event.getMessage());
            });
        }

        IJ.showStatus("Initializing JIPipe ...");
        getService().getNodes().installEvents();
        List<PluginInfo<JIPipeJavaPlugin>> pluginList = getService().getPluginService().getPluginsOfType(JIPipeJavaPlugin.class).stream()
                .sorted(JIPipe::comparePlugins).toList();
        getService().getPlugins().initialize(); // Init extension registry
        getService().getPlugins().load();
        getProgressInfo().setProgress(1);
        getProgressInfo().log("Legacy settings conversion ...");
        copyTemplatesFromPropertiesToLegacyProfile(getProgressInfo().resolve("Legacy conversion"));
        if (applyProfileUpgrades(getProgressInfo().resolve("Preparing profiles"))) {
            getProgressInfo().log("Upgrade was applied. Reloading extension settings.");
            extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        }

        getProgressInfo().log("Pre-initialization phase ...");

        // Creating instances of extensions
        Map<String, JIPipeJavaExtensionInitializationInfo> allJavaExtensionsByID = new HashMap<>();

        for (PluginInfo<JIPipeJavaPlugin> pluginInfo : pluginList) {
            try {
                getProgressInfo().log("Creating instance of " + pluginInfo + " ...");
                JIPipeJavaPlugin extension = pluginInfo.createInstance();

                // Validate ID
                if (!JIPipeUtils.isValidExtensionId(extension.getDependencyId())) {
                    System.err.println("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + ". REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");
                    getProgressInfo().log("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + ". REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");

                    continue;
                } else {
                    if (allJavaExtensionsByID.containsKey(extension.getDependencyId())) {
                        System.err.println("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder. REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");
                        getProgressInfo().log("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder. REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");

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
        getProgressInfo().log("Determining extensions to be loaded ...");
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
                    if (JIPipeUtils.isValidExtensionId(extension.getDependencyId())) {
                        if (!impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                            getProgressInfo().log("-> Core/User: " + extension.getDependencyId());
                            impliedLoadedJavaExtensions.add(extension.getDependencyId());
                            impliedLoadedJavaExtensionsChanged = true;
                        }
                    }
                    for (JIPipeDependency dependency : extension.getDependencies()) {
                        if (JIPipeUtils.isValidExtensionId(dependency.getDependencyId())) {
                            if (!impliedLoadedJavaExtensions.contains(dependency.getDependencyId())) {
                                impliedLoadedJavaExtensions.add(dependency.getDependencyId());
                                impliedLoadedJavaExtensionsChanged = true;
                                getProgressInfo().log("-> Required by " + extension.getDependencyId() + ": " + dependency.getDependencyId());
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
                getService().getPlugins().registerKnownPlugin(extension);

                // Check if the extension should be loaded
                if (!extension.isCorePlugin() && getService().getPlugins().getSettings().getDeactivatedPlugins().contains(extension.getDependencyId()) && !impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    getProgressInfo().log("Extension with ID " + extension.getDependencyId() + " will not be loaded (deactivated in extension manager)");
                    initializationInfo.setLoaded(false);
                    continue;
                }

                // Extension self-check
                JIPipeValidationReport preActivationIssues = new JIPipeValidationReport();
                issues.getPreActivationIssues().put(extension.getDependencyId(), preActivationIssues);
                if (!extension.canActivate(preActivationIssues, getProgressInfo().resolve("Pre-activation check").resolve(extension.getDependencyId()))) {
                    if (!extensionSettings.isIgnorePreActivationChecks()) {
                        new JavaExtensionValidationReportContext(extension).warning()
                                .title("Extension '" + extension.getMetadata().getName() + "' refuses to activate!")
                                .explanation("The extension's pre-activation check failed. It will not be activated. Please refer to the other items if available.")
                                .report(preActivationIssues);
                        getProgressInfo().log("Extension with ID " + extension.getDependencyId() + " will not be loaded (pre-activation check failed; extension refuses to activate)");
                        initializationInfo.setLoaded(false);
                        if (!StringUtils.isNullOrEmpty(extension.getDependencyId())) {
                            getProgressInfo().log("Extension with ID " + extension.getDependencyId() + " was removed from the list of activated extensions");
                            getService().getPlugins().getSettings().getDeactivatedPlugins().add(extension.getDependencyId());
                            preActivationScheduledSave = true;
                        }
                        continue;
                    } else {
                        getProgressInfo().log("Extension with ID " + extension.getDependencyId() + " indicated that its pre-activation checks failed. WILL BE LOADED anyway DUE TO APPLICATION SETTINGS!");
                    }
                }

                getContext().inject(extension);
                extension.setService(getService());
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                initializationInfo.setLoaded(true);
                getService().getExtensionDiscoveredEventEmitter().emit(new JIPipePluginDiscoveredEvent(getService(), extension));
            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(initializationInfo.getPluginInfo());
            }
        }

        // Save extension settings
        if (preActivationScheduledSave && getService().isAutosaveSettings()) {
            getService().getPlugins().save();
        }

        getProgressInfo().setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = getProgressInfo().resolveAndLog("Register features");
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
                extension.register(getService(), getContext(), getProgressInfo().resolve(extension.getDependencyId()));
                report.getRegisteredExtensions().add(extension);
                report.getRegisteredExtensionIds().add(extension.getDependencyId());
                getService().getExtensionRegisteredEventEmitter().emit(new JIPipePluginRegisteredEvent(getService(), extension));
            } catch (NoClassDefFoundError | Exception e) {
                getProgressInfo().log("[!] ERROR: Unable to instantiate extension " + info);
                e.printStackTrace();
                getProgressInfo().log(e.toString());
                issues.getErroneousPlugins().add(info);
                issues.getErrors().add(e);
                if (extension != null) {
                    report.getFailedExtensions().add(extension);
                }
            }
        }

        registerFeaturesProgress.log("Registering remaining " + getService().getNodes().getScheduledRegistrationTasks().size() + " features ...");
        for (JIPipeNodeRegistrationTask task : getService().getNodes().getScheduledRegistrationTasks()) {
            try {
                task.register();
            } catch (Throwable ex) {
                getService().getLogService().error("Could not register: " + task.toString() + " -> " + ex);
                registerFeaturesProgress.log("Could not register: " + task + " -> " + ex);
            }
        }

        // Check for errors

        getProgressInfo().setProgress(3);
        getProgressInfo().log("Validating node types ...");
        validateDataTypes(issues);
        if (extensionSettings.isValidateNodeTypes()) {
            validateNodeTypes(issues);
        }
        getProgressInfo().log("Validating parameter types ...");
        validateParameterTypes(issues);

        // Create dependency graph
        getProgressInfo().log("Creating dependency graph ...");
        getService().getPlugins().getDependencyGraph();

        // Create settings for default importers
        getProgressInfo().log("Creating dynamic settings ...");
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();
        createDefaultEnvironmentSettings();
        registerNodeExamplesFromFileSystem();
        registerProjectTemplatesFromFileSystem();

        // Reload settings
        getProgressInfo().setProgress(4);
        getProgressInfo().log("Loading settings ...");
        getService().getApplicationSettings().reload();

        // Required as the reload deletes the allowed values
        updateDefaultImporterSettings();
        updateDefaultCacheDisplaySettings();

        // Postprocessing
        getProgressInfo().setProgress(5);
        JIPipeProgressInfo postprocessingProgress = getProgressInfo().resolveAndLog("Postprocessing");
        for (JIPipeDependency extension : report.getRegisteredExtensions()) {
            if (!report.getFailedExtensions().contains(extension) && extension instanceof JIPipeJavaPlugin) {
                ((JIPipeJavaPlugin) extension).postprocess(postprocessingProgress.resolveAndLog(extension.getDependencyId()));
            }
        }
        postprocessingProgress.log("Converting display operations to import operations ...");
        getService().getDataTypes().convertDisplayOperationsToImportOperations();
        postprocessingProgress.log("Registering examples ...");
        getService().getNodes().executeScheduledRegisterExamples();
        postprocessingProgress.log("Registering extension-provided templates ...");
        getService().getNodes().executeScheduledRegisterTemplates();

        // Check recent projects and backups
        getProgressInfo().setProgress(6);
        getProgressInfo().log("Checking recent projects ...");
        getService().getRecentProjects().reload();
        getService().getRecentProjects().cleanup();
        getService().getRecentProjects().migrateFromLegacy();

        // Check artifacts
        getProgressInfo().setProgress(7);
        getService().getArtifacts().updateCachedArtifacts(getProgressInfo().resolve("Updating artifacts"));

        // Check acceleration
        if (JIPipeArtifactApplicationSettings.getInstance().isAutoConfigureAccelerationOnNextStartup()) {
            getProgressInfo().log("Determining acceleration profile ...");
            try {

                if (CUDAUtils.hasCudaSupport()) {
                    getProgressInfo().log("Determining acceleration profile ... CUDA support detected");
                    JIPipeArtifactApplicationSettings.getInstance().setAccelerationPreference(JIPipeArtifactAccelerationPreference.CUDA);

                    try {
                        JIPipeArtifactApplicationSettings.getInstance().setAccelerationPreferenceVersions(new Vector2iParameter(
                                CUDAUtils.getMinimumCudaVersion(),
                                0  // Broken due to Nvidia-SMI hanging on Linux -> have to use 0
                        ));
                        getProgressInfo().log("Determined CUDA version limits as " + JIPipeArtifactApplicationSettings.getInstance().getAccelerationPreferenceVersions());
                    } catch (Exception e) {
                        getProgressInfo().log(e);
                    }
                }

                JIPipeArtifactApplicationSettings.getInstance().setAutoConfigureAccelerationOnNextStartup(false);
                getService().getApplicationSettings().save();
            } catch (Exception e) {
                getProgressInfo().log(e);
            }
        }

        // Load templates
        getProgressInfo().log("Loading node templates ...");
        getService().getNodeTemplates().reloadGlobalTemplates(getProgressInfo().resolve("Node templates"));

        getProgressInfo().setProgress(8);
        getProgressInfo().log("JIPipe loading finished");
    }

    @Override
    public void runPostprocessing() {
        // Check if we have viewers for everything
        for (Class<? extends JIPipeData> dataClass : getService().getDataTypes().getRegisteredDataTypes().values()) {
            Class<? extends JIPipeDesktopDataViewer> defaultDataViewer = getService().getDataTypes().getDefaultDataViewer(dataClass);
            if (defaultDataViewer == JIPipeDesktopDefaultDataViewer.class) {
                getProgressInfo().log("Info: Data type " + getService().getDataTypes().getIdOf(dataClass) + " does not have a default data viewer");
            }
        }

        // Check for new extensions
        getService().getPlugins().findNewPlugins();
        for (String newExtension : getService().getPlugins().getNewPlugins()) {
            getProgressInfo().log("New extension found: " + newExtension);
        }

        // Error log
        if (!getService().getInitializationReport().getErrors().isEmpty()) {
            for (Throwable error : getService().getInitializationReport().getErrors()) {
                getProgressInfo().log("\n-------------------------------------------------\n");
                getProgressInfo().log("-- ERROR: " + error);
                getProgressInfo().log("-- MESSAGE: " + error.getMessage());
                getProgressInfo().log("-- STACKTRACE: " + ExceptionUtils.getStackTrace(error));
                getProgressInfo().log("\n-------------------------------------------------\n");
            }
            getProgressInfo().log("\n-------------------------------------------------\n");
            getProgressInfo().log("Found " + StringUtils.formatPluralS(getService().getInitializationReport().getErrors().size(), "error") + "!");
        }


        // Push progress into log
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry("JIPipe initialization",
                LocalDateTime.now(),
                getProgressInfo().getLog().toString(),
                new JIPipeNotificationInbox(), true));

        // Mark log as read
        JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead();
    }

    private void validateParameterTypes(JIPipeInitializationReport issues) {
        for (Map.Entry<String, JIPipeParameterTypeInfo> entry : getService().getParameterTypes().getRegisteredParameters().entrySet()) {
            try {
                entry.getValue().newInstance();
            } catch (Throwable t) {
                getService().getLogService().warn("Parameter type '" + entry.getKey() + "' cannot be initialized.");
                issues.getErroneousParameterTypes().add(entry.getValue());
                t.printStackTrace();
            }
            try {
                Object o = entry.getValue().newInstance();
                entry.getValue().duplicate(o);
            } catch (Throwable t) {
                getService().getLogService().warn("Parameter type '" + entry.getKey() + "' cannot be duplicated.");
                issues.getErroneousParameterTypes().add(entry.getValue());
                t.printStackTrace();
            }
        }
    }


    private void validateDataTypes(JIPipeInitializationReport issues) {
        for (Class<? extends JIPipeData> dataType : getService().getDataTypes().getRegisteredDataTypes().values()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(dataType);
            if (info.getStorageDocumentation() == null) {
                getService().getLogService().warn("Data type '" + dataType + "' has no storage documentation.");
                issues.getErroneousDataTypes().add(dataType);
            }
            if (dataType.isInterface() || Modifier.isAbstract(dataType.getModifiers()))
                continue;
            // Check if we can find a method "import"
            try {
                Method method = dataType.getDeclaredMethod("importData", JIPipeReadDataStorage.class, JIPipeProgressInfo.class);
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("Import method is not static!");
                }
                if (!JIPipeData.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalArgumentException("Import method does not return JIPipeData!");
                }
            } catch (NoClassDefFoundError | Exception e) {
                // Unregister node
                getService().getLogService().warn("Data type '" + dataType + "' cannot be instantiated.");
                getService().getLogService().warn("Ensure that a method static JIPipeData importData(Path, JIPipeProgressInfo) is present!");
                issues.getErroneousDataTypes().add(dataType);
                e.printStackTrace();
            }
        }
    }

    private void validateNodeTypes(JIPipeInitializationReport issues) {
        for (JIPipeNodeInfo info : ImmutableList.copyOf(getService().getNodes().getRegisteredNodeInfos().values())) {
            try {
                // Test instantiation
                JIPipeGraphNode algorithm = info.newInstance();

                // Test environments
                for (Class<? extends JIPipeEnvironment> environmentClass : algorithm.getInfo().getEnvironments()) {
                    JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = getService().getEnvironments().getInfoByClass(environmentClass);
                    if (environmentInfo.getArchetype() != JIPipeEnvironmentArchetype.Managed) {
                        getProgressInfo().log("[!] ERROR: Node is associated to unmanaged environment " + environmentInfo.getId());
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new UnspecifiedValidationReportContext(),
                                "A plugin is invalid!",
                                "Node is associated to unmanaged environment " + environmentInfo.getId(),
                                "There is an error in the plugin's code that registers an unsupported feature.",
                                "Please contact the plugin author for further help."));
                    }
                }

                // Test parameters
                JIPipeParameterTree collection = new JIPipeParameterTree(algorithm);
                for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
                    if (JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()) == null) {
                        getProgressInfo().log("[!] ERROR: Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                                + algorithm + " -> " + entry.getKey());
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new UnspecifiedValidationReportContext(),
                                "A plugin is invalid!",
                                "Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                                        + algorithm + " -> " + entry.getKey(),
                                "There is an error in the plugin's code that makes it use an unsupported parameter type.",
                                "Please contact the plugin author for further help."));
                    }
                }

                // Test duplication
                try {
                    algorithm.duplicate();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new JIPipeValidationRuntimeException(e1,
                            "A plugin is invalid!",
                            "There is an error in the plugin's code that prevents the copying of a node.",
                            "Please contact the plugin author for further help.");
                }

                // Test serialization
                try {
                    JsonUtils.toJsonString(algorithm);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new JIPipeValidationRuntimeException(e1,
                            "A plugin is invalid!",
                            "There is an error in the plugin's code that prevents the saving of a node.",
                            "Please contact the plugin author for further help.");
                }

                // Test cache state generation
                try {
                    if (!algorithm.functionallyEquals(algorithm)) {
                        throw new RuntimeException("Node " + algorithm.getInfo().getId() + " is not functionally equal to itself!");
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new JIPipeValidationRuntimeException(e1,
                            "A plugin is invalid!",
                            "There is an error in the plugin's code that prevents the cache state generation of a node.",
                            "Please contact the plugin author for further help.");
                }

                getService().getLogService().debug("OK: Algorithm '" + info.getId() + "'");
            } catch (NoClassDefFoundError | Exception e) {
                e.printStackTrace();
                // Unregister node
                getService().getLogService().warn("Unregistering node with id '" + info.getId() + "' as it cannot be instantiated, duplicated, serialized, or cached.");
                getService().getNodes().unregister(info.getId());
                issues.getErroneousNodes().add(info);
            }
        }
    }
}
