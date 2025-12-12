package org.hkijena.jipipe.api.service.init;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeInitializationReport;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializer;
import org.hkijena.jipipe.api.service.components.nodes.JIPipeNodeRegistrationTask;
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
    public void runInitialization() {
        JIPipeInitializationReport report = getService().getInitializationReport();

        getProgressInfo().setProgress(0, 5);
        getService().getNodes().installEvents();
        getService().getPlugins().initialize(); // Init extension registry
        getService().getPlugins().load();
        getProgressInfo().setProgress(1);
        getProgressInfo().log("Pre-initialization phase ...");

        List<JIPipeJavaPlugin> pluginInstances = new ArrayList<>();
        for (Class<? extends JIPipeJavaPlugin> pluginClass : plugins) {
            try {
                JIPipeJavaPlugin extension = pluginClass.newInstance();
                getContext().inject(extension);
                extension.setService(getService());
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }

                pluginInstances.add(extension);
                getService().getExtensionDiscoveredEventEmitter().emit(new JIPipePluginDiscoveredEvent(getService(), extension));
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        getProgressInfo().setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = getProgressInfo().resolveAndLog("Register features");

        for (JIPipeJavaPlugin extension : pluginInstances) {
            extension.register(getService(), getContext(), registerFeaturesProgress.resolve(extension.getDependencyId()));
            report.getRegisteredExtensions().add(extension);
            report.getRegisteredExtensionIds().add(extension.getDependencyId());
            getService().getExtensionRegisteredEventEmitter().emit(new JIPipePluginRegisteredEvent(getService(), extension));
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

        // Create settings for default importers
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();
        createDefaultEnvironmentSettings();

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
        postprocessingProgress.log("Post-processing service components ...");
        for(JIPipeServiceComponent component : getService ().getComponents()) {
            component.postprocess(getProgressInfo().resolve(component.getClass().getSimpleName()));
        }
        getProgressInfo().log("JIPipe loading finished");
    }

    @Override
    public void runPostprocessing() {
        // Push progress into log
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry("JIPipe initialization",
                LocalDateTime.now(),
                getProgressInfo().getLog().toString(),
                new JIPipeNotificationInbox(), true));

        // Mark log as read
        JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead();
    }
}
