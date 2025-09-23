package org.hkijena.jipipe.api.service.init;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistrationTask;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializer;
import org.hkijena.jipipe.api.service.JIPipeServiceState;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEvent;
import org.hkijena.jipipe.api.service.events.JIPipePluginRegisteredEvent;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.scijava.service.AbstractService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JIPipeServiceNoImageJInitializer extends JIPipeServiceInitializer {

    private final List<Class<? extends JIPipeJavaPlugin>> plugins;

    public JIPipeServiceNoImageJInitializer(JIPipeService service, List<Class<? extends JIPipeJavaPlugin>> plugins) {
        super(service);
        this.plugins = plugins;
    }

    @Override
    public void run() {
        if(state != JIPipeServiceState.Uninitialized) {
            progressInfo.log("ERROR: JIPipe initialization has already been called");
            return;
        }
        state = JIPipeServiceState.Initializing;

        progressInfo.setProgress(0, 5);
        nodeRegistry.installEvents();
        pluginRegistry.initialize(); // Init extension registry
        pluginRegistry.load();
        progressInfo.setProgress(1);
        progressInfo.log("Pre-initialization phase ...");

        List<JIPipeJavaPlugin> pluginInstances = new ArrayList<>();
        for (Class<? extends JIPipeJavaPlugin> pluginClass : plugins) {
            try {
                JIPipeJavaPlugin extension = pluginClass.newInstance();
                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }

                pluginInstances.add(extension);
                extensionDiscoveredEventEmitter.emit(new JIPipePluginDiscoveredEvent(this, extension));
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        progressInfo.setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = progressInfo.resolveAndLog("Register features");

        for (JIPipeJavaPlugin extension : pluginInstances) {
            extension.register(this, getContext(), registerFeaturesProgress.resolve(extension.getDependencyId()));
            registeredExtensions.add(extension);
            registeredExtensionIds.add(extension.getDependencyId());
            extensionRegisteredEventEmitter.emit(new JIPipePluginRegisteredEvent(this, extension));
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

        // Create settings for default importers
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();

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

        state = JIPipeServiceState.Initialized;

        // Push progress into log
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry("JIPipe initialization",
                LocalDateTime.now(),
                progressInfo.getLog().toString(),
                new JIPipeNotificationInbox(), true));

        // Mark log as read
        JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead();
    }
}
