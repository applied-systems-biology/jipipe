/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeInitializationReport;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.components.*;
import org.hkijena.jipipe.api.service.events.JIPipeDatatypeRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipeNodeInfoRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipePluginRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.init.JIPipeServiceDefaultInitializer;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JavaExtensionValidationReportContext;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Contains all JIPipe resources
 */
@Plugin(type = Service.class)
public class JIPipeService extends AbstractService implements JIPipeValidatable {

    private final JIPipeInitializationReport initializationReport = new JIPipeInitializationReport();
    private final JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final JIPipeNodesServiceComponent nodes;
    private final JIPipeDatatypesServiceComponent dataTypes;
    private final JIPipeImageJAdaptersServiceComponent imageJDataAdapters;
    private final JIPipeCustomMenuItemsServiceComponent customMenuItems;
    private final JIPipeParameterTypesServiceComponent parameterTypes;
    private final JIPipeApplicationSettingsServiceComponent applicationSettings;
    private final JIPipeProjectSettingsServiceComponent projectSettings;
    private final JIPipeExpressionFunctionsServiceComponent expressionFunctions;
    private final JIPipeUtilityClassesServiceComponent utilityClasses;
    private final JIPipeEnvironmentsServiceComponent environments;
    private final JIPipePluginsServiceComponent plugins;
    private final JIPipeGraphEditorToolsServiceComponent graphEditorTools;
    private final JIPipeProjectTemplatesServiceComponent projectTemplates;
    private final JIPipeArtifactsServiceComponent artifacts;
    private final JIPipeNodeTemplatesServiceComponent nodeTemplates;
    private final JIPipeRecentProjectsRegistry recentProjects;
    private final JIPipeMetadataTypesServiceComponent metadataTypes;
    private final JIPipeAccelerationServiceComponent acceleration;
    private final JIPipeCleanupServiceComponent cleanup;
    private final JIPipeProjectBackupServiceComponent projectBackup;
    private final JIPipeDatatypeRegisteredEventEmitter datatypeRegisteredEventEmitter = new JIPipeDatatypeRegisteredEventEmitter();
    private final JIPipePluginDiscoveredEventEmitter extensionDiscoveredEventEmitter = new JIPipePluginDiscoveredEventEmitter();
    private final JIPipePluginRegisteredEventEmitter extensionRegisteredEventEmitter = new JIPipePluginRegisteredEventEmitter();
    private final JIPipeNodeInfoRegisteredEventEmitter nodeInfoRegisteredEventEmitter = new JIPipeNodeInfoRegisteredEventEmitter();
    private JIPipeServiceState state = JIPipeServiceState.Uninitialized;
    private JIPipeServiceInitializationSettings initializationSettings = new JIPipeServiceInitializationSettings();
    private JIPipeServiceInitializer initializer = new JIPipeServiceDefaultInitializer(this);
    @Parameter
    private LogService logService;
    @Parameter
    private PluginService pluginService;
    private boolean autosaveSettings;
    private final JIPipeServiceComponent[] components;

    public JIPipeService() {
        recentProjects = new JIPipeRecentProjectsRegistry(this);
        nodes = new JIPipeNodesServiceComponent(this);
        dataTypes = new JIPipeDatatypesServiceComponent(this);
        imageJDataAdapters = new JIPipeImageJAdaptersServiceComponent(this);
        customMenuItems = new JIPipeCustomMenuItemsServiceComponent(this);
        parameterTypes = new JIPipeParameterTypesServiceComponent(this);
        applicationSettings = new JIPipeApplicationSettingsServiceComponent(this);
        projectSettings = new JIPipeProjectSettingsServiceComponent(this);
        expressionFunctions = new JIPipeExpressionFunctionsServiceComponent(this);
        utilityClasses = new JIPipeUtilityClassesServiceComponent(this);
        environments = new JIPipeEnvironmentsServiceComponent(this);
        plugins = new JIPipePluginsServiceComponent(this);
        projectTemplates = new JIPipeProjectTemplatesServiceComponent(this);
        graphEditorTools = new JIPipeGraphEditorToolsServiceComponent(this);
        metadataTypes = new JIPipeMetadataTypesServiceComponent(this);
        artifacts = new JIPipeArtifactsServiceComponent(this);
        nodeTemplates = new JIPipeNodeTemplatesServiceComponent(this);
        acceleration = new JIPipeAccelerationServiceComponent(this);
        cleanup = new JIPipeCleanupServiceComponent(this);
        projectBackup = new JIPipeProjectBackupServiceComponent(this);

        // Add into components list so we can later postprocess them
        this.components = new JIPipeServiceComponent[] {
                recentProjects, nodes, dataTypes, imageJDataAdapters, customMenuItems, parameterTypes, applicationSettings, projectSettings, expressionFunctions, utilityClasses,
                environments, plugins, projectTemplates, artifacts, nodeTemplates, acceleration, cleanup, projectBackup
        };
    }

    @Override
    public void dispose() {
        super.dispose();

        // Unload all plugins
        for (String activatedPluginId : getPlugins().getActivatedPlugins()) {
            logService.log(LogLevel.INFO, "Unloading plugin " + activatedPluginId);
            try {
                JIPipePlugin plugin = getPlugins().getKnownPluginById(activatedPluginId);
                plugin.dispose();
            } catch (Throwable e) {
                getLogService().log(LogLevel.ERROR, "Failed to unload plugin " + activatedPluginId, e);
            }
        }
    }

    public JIPipeServiceState getState() {
        ensureInitialized();
        return state;
    }

    public LogService getLogService() {
        return logService;
    }

    public JIPipeCleanupServiceComponent getCleanup() {
        return cleanup;
    }

    public JIPipeEnvironmentsServiceComponent getEnvironments() {
        ensureInitialized();
        return environments;
    }

    public JIPipeUtilityClassesServiceComponent getUtilityClasses() {
        ensureInitialized();
        return utilityClasses;
    }

    public JIPipeArtifactsServiceComponent getArtifacts() {
        ensureInitialized();
        return artifacts;
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    public JIPipeParameterTypesServiceComponent getParameterTypes() {
        ensureInitialized();
        return parameterTypes;
    }

    public JIPipeApplicationSettingsServiceComponent getApplicationSettings() {
        ensureInitialized();
        return applicationSettings;
    }

    public JIPipeProjectSettingsServiceComponent getProjectSettings() {
        ensureInitialized();
        return projectSettings;
    }

    public JIPipeMetadataTypesServiceComponent getMetadataTypes() {
        ensureInitialized();
        return metadataTypes;
    }

    public JIPipeRecentProjectsRegistry getRecentProjects() {
        ensureInitialized();
        return recentProjects;
    }

    public JIPipePluginsServiceComponent getPlugins() {
        ensureInitialized();
        return plugins;
    }

    public JIPipeNodesServiceComponent getNodes() {
        ensureInitialized();
        return nodes;
    }

    public JIPipeDatatypesServiceComponent getDataTypes() {
        ensureInitialized();
        return dataTypes;
    }

    public JIPipeImageJAdaptersServiceComponent getImageJDataAdapters() {
        ensureInitialized();
        return imageJDataAdapters;
    }

    public List<JIPipeDependency> getRegisteredExtensions() {
        ensureInitialized();
        return Collections.unmodifiableList(initializationReport.getRegisteredExtensions());
    }

    public JIPipeCustomMenuItemsServiceComponent getCustomMenuItems() {
        ensureInitialized();
        return customMenuItems;
    }

    public JIPipeNodeTemplatesServiceComponent getNodeTemplates() {
        ensureInitialized();
        return nodeTemplates;
    }

    public Set<String> getRegisteredExtensionIds() {
        ensureInitialized();
        return initializationReport.getRegisteredExtensionIds();
    }

    public JIPipeDatatypeRegisteredEventEmitter getDatatypeRegisteredEventEmitter() {
        return datatypeRegisteredEventEmitter;
    }

    public JIPipePluginDiscoveredEventEmitter getExtensionDiscoveredEventEmitter() {
        return extensionDiscoveredEventEmitter;
    }


    public JIPipePluginRegisteredEventEmitter getExtensionRegisteredEventEmitter() {
        return extensionRegisteredEventEmitter;
    }


    public JIPipeNodeInfoRegisteredEventEmitter getNodeInfoRegisteredEventEmitter() {
        return nodeInfoRegisteredEventEmitter;
    }

    public JIPipeExpressionFunctionsServiceComponent getExpressionFunctions() {
        ensureInitialized();
        return expressionFunctions;
    }

    public JIPipeGraphEditorToolsServiceComponent getGraphEditorTools() {
        ensureInitialized();
        return graphEditorTools;
    }

    public JIPipeProjectTemplatesServiceComponent getProjectTemplates() {
        ensureInitialized();
        return projectTemplates;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        report.report(reportContext, nodes, progressInfo);
        for (JIPipeDependency extension : initializationReport.getFailedExtensions()) {
            if (extension != null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new JavaExtensionValidationReportContext(extension),
                        "Error during loading the extension!",
                        "There was an error while loading the extension. Please refer to the message that you get on restarting JIPipe. Please refer to the message that you get on restarting JIPipe.",
                        null,
                        null));
            }
        }
        for (JIPipeDependency extension : initializationReport.getRegisteredExtensions()) {
            report.report(reportContext, extension, progressInfo);
        }
    }

    public JIPipeServiceInitializationSettings getInitializationSettings() {
        return initializationSettings;
    }

    public void setInitializationSettings(JIPipeServiceInitializationSettings initializationSettings) {
        if (state != JIPipeServiceState.Uninitialized) {
            throw new IllegalStateException("The JIPipe service has already been initialized.");
        }
        this.initializationSettings = new JIPipeServiceInitializationSettings(initializationSettings);
    }

    public boolean isAutosaveSettings() {
        return autosaveSettings;
    }

    public void setAutosaveSettings(boolean autosaveSettings) {
        this.autosaveSettings = autosaveSettings;
    }

    public void ensureInitialized() {
        if (state == JIPipeServiceState.Uninitialized) {
            state = JIPipeServiceState.Initializing;
            try {
                initializer.runInitialization();
                state = JIPipeServiceState.Initialized;
            } catch (Throwable e) {
                state = JIPipeServiceState.Error;
                e.printStackTrace();
            } finally {
                initializer.runPostprocessing();
            }
        }
    }

    public JIPipeInitializationReport getInitializationReport() {
        return initializationReport;
    }

    public JIPipeServiceInitializer getInitializer() {
        return initializer;
    }

    public void setInitializer(JIPipeServiceInitializer initializer) {
        if (state != JIPipeServiceState.Uninitialized) {
            throw new IllegalStateException("The JIPipe service has already been initialized.");
        }
        this.initializer = initializer;
    }

    public JIPipeAccelerationServiceComponent getAcceleration() {
        return acceleration;
    }

    public JIPipeProjectBackupServiceComponent getProjectBackup() {
        return projectBackup;
    }

    public JIPipeServiceComponent[] getComponents() {
        return components;
    }
}
