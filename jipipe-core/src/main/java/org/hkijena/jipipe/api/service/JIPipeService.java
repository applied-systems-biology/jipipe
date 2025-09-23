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

import net.imagej.updater.FilesCollection;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.events.JIPipeDatatypeRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipeNodeInfoRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipePluginRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.components.*;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JavaExtensionValidationReportContext;
import org.hkijena.jipipe.api.service.components.JIPipeCustomMenuItemsServiceComponent;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import java.util.*;

/**
 * Contains all JIPipe resources
 */
@Plugin(type = Service.class)
public class JIPipeService extends AbstractService implements JIPipeValidatable {

    private JIPipeServiceState state =  JIPipeServiceState.Uninitialized;
    private final JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    private JIPipeServiceInitializationSettings initializationSettings = new JIPipeServiceInitializationSettings();

    private final Set<String> registeredExtensionIds = new HashSet<>();
    private final List<JIPipeDependency> registeredExtensions = new ArrayList<>();
    private final List<JIPipeDependency> failedExtensions = new ArrayList<>();

    private final JIPipeNodesServiceComponent nodes;
    private final JIPipeDatatypesServiceComponent dataTypes;
    private final JIPipeImageJAdaptersServiceComponent imageJDataAdapters;
    private final JIPipeCustomMenuItemsServiceComponent customMenuItems;
    private final JIPipeParameterTypesServiceComponent parameterTypes;
    private final JIPipeApplicationSettingsServiceComponent applicationSettings;
    private final JIPipeProjectSettingsServiceComponent projectSettings;
    private final JIPipeExpressionFunctionsServiceComponent expressionFunctions;
    private final JIPipeUtilitiesServiceComponent utilities;
    private final JIPipeEnvironmentsServiceComponent environments;
    private final JIPipePluginsServiceComponent plugins;
    private final JIPipeGraphEditorToolsServiceComponent graphEditorTools;
    private final JIPipeProjectTemplatesServiceComponent projectTemplates;
    private final JIPipeArtifactsServiceComponent artifacts;
    private final JIPipeNodeTemplatesServiceComponent nodeTemplates;
    private final JIPipeRecentProjectsRegistry recentProjects;
    private final JIPipeMetadataTypesServiceComponent metadata;

    private FilesCollection imageJPlugins = null;

    @Parameter
    private LogService logService;
    @Parameter
    private PluginService pluginService;

    private final JIPipeDatatypeRegisteredEventEmitter datatypeRegisteredEventEmitter = new JIPipeDatatypeRegisteredEventEmitter();
    private final JIPipePluginDiscoveredEventEmitter extensionDiscoveredEventEmitter = new JIPipePluginDiscoveredEventEmitter();
    private final JIPipePluginRegisteredEventEmitter extensionRegisteredEventEmitter = new JIPipePluginRegisteredEventEmitter();
    private final JIPipeNodeInfoRegisteredEventEmitter nodeInfoRegisteredEventEmitter = new JIPipeNodeInfoRegisteredEventEmitter();
    private boolean autosaveSettings;

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
        utilities = new JIPipeUtilitiesServiceComponent(this);
        environments = new JIPipeEnvironmentsServiceComponent(this);
        plugins = new JIPipePluginsServiceComponent(this);
        projectTemplates = new JIPipeProjectTemplatesServiceComponent(this);
        graphEditorTools = new JIPipeGraphEditorToolsServiceComponent(this);
        metadata = new JIPipeMetadataTypesServiceComponent(this);
        artifacts = new JIPipeArtifactsServiceComponent(this);
        nodeTemplates = new JIPipeNodeTemplatesServiceComponent(this);
    }

    public JIPipeServiceState getState() {
        return state;
    }

    public FilesCollection getImageJPlugins() {
        return imageJPlugins;
    }

    public LogService getLogService() {
        return logService;
    }

    public JIPipeEnvironmentsServiceComponent getEnvironments() {
        return environments;
    }

    public JIPipeUtilitiesServiceComponent getUtilities() {
        return utilities;
    }

    public JIPipeArtifactsServiceComponent getArtifacts() {
        return artifacts;
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    public JIPipeParameterTypesServiceComponent getParameterTypes() {
        return parameterTypes;
    }

    public JIPipeApplicationSettingsServiceComponent getApplicationSettings() {
        return applicationSettings;
    }

    public JIPipeProjectSettingsServiceComponent getProjectSettings() {
        return projectSettings;
    }

    public JIPipeMetadataTypesServiceComponent getMetadata() {
        return metadata;
    }

    public JIPipeExpressionFunctionsServiceComponent getExpressionRegistry() {
        return expressionFunctions;
    }

    public JIPipeRecentProjectsRegistry getRecentProjects() {
        return recentProjects;
    }

    public JIPipePluginsServiceComponent getPlugins() {
        return plugins;
    }

    public JIPipeNodesServiceComponent getNodes() {
        return nodes;
    }

    public JIPipeDatatypesServiceComponent getDataTypes() {
        return dataTypes;
    }

    public JIPipeImageJAdaptersServiceComponent getImageJDataAdapters() {
        return imageJDataAdapters;
    }

    public List<JIPipeDependency> getRegisteredExtensions() {
        return Collections.unmodifiableList(registeredExtensions);
    }

    public JIPipeCustomMenuItemsServiceComponent getCustomMenuItems() {
        return customMenuItems;
    }

    public JIPipeNodeTemplatesServiceComponent getNodeTemplates() {
        return nodeTemplates;
    }

    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
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
        return expressionFunctions;
    }


    public JIPipeGraphEditorToolsServiceComponent getGraphEditorTools() {
        return graphEditorTools;
    }


    public JIPipeProjectTemplatesServiceComponent getProjectTemplates() {
        return projectTemplates;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public List<JIPipeDependency> getFailedExtensions() {
        return Collections.unmodifiableList(failedExtensions);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report) {
        report.report(reportContext, nodes);
        for (JIPipeDependency extension : failedExtensions) {
            if (extension != null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new JavaExtensionValidationReportContext(extension),
                        "Error during loading the extension!",
                        "There was an error while loading the extension. Please refer to the message that you get on restarting JIPipe. Please refer to the message that you get on restarting JIPipe.",
                        null,
                        null));
            }
        }
        for (JIPipeDependency extension : registeredExtensions) {
            report.report(reportContext, extension);
        }
    }

    public JIPipeDependency findExtensionById(String dependencyId) {
        return registeredExtensions.stream().filter(d -> Objects.equals(dependencyId, d.getDependencyId())).findFirst().orElse(null);
    }

    public JIPipeServiceInitializationSettings getInitializationSettings() {
        return initializationSettings;
    }

    public void setInitializationSettings(JIPipeServiceInitializationSettings initializationSettings) {
        if(state != JIPipeServiceState.Uninitialized) {
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
}
