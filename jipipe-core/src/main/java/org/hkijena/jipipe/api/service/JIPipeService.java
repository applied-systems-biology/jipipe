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
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.api.service.events.JIPipeDatatypeRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipeNodeInfoRegisteredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipePluginDiscoveredEventEmitter;
import org.hkijena.jipipe.api.service.events.JIPipePluginRegisteredEventEmitter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JavaExtensionValidationReportContext;
import org.hkijena.jipipe.desktop.api.registries.JIPipeCustomMenuRegistry;
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
    private static boolean IS_RESTARTING = false;
    private final JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    private JIPipeServiceInitializationSettings initializationSettings = new JIPipeServiceInitializationSettings();

    private final Set<String> registeredExtensionIds = new HashSet<>();
    private final List<JIPipeDependency> registeredExtensions = new ArrayList<>();
    private final List<JIPipeDependency> failedExtensions = new ArrayList<>();

    private final JIPipeNodeRegistry nodeRegistry;
    private final JIPipeDatatypeRegistry datatypeRegistry;
    private final JIPipeImageJAdapterRegistry imageJDataAdapterRegistry;
    private final JIPipeCustomMenuRegistry customMenuRegistry;
    private final JIPipeParameterTypeRegistry parameterTypeRegistry;
    private final JIPipeApplicationSettingsRegistry applicationSettingsRegistry;
    private final JIPipeProjectSettingsRegistry projectSettingsRegistry;
    private final JIPipeExpressionRegistry tableOperationRegistry;
    private final JIPipeUtilityRegistry utilityRegistry;
    private final JIPipeExternalEnvironmentRegistry externalEnvironmentRegistry;
    private final JIPipePluginRegistry pluginRegistry;
    private final JIPipeGraphEditorToolRegistry graphEditorToolRegistry;
    private final JIPipeProjectTemplateRegistry projectTemplateRegistry;
    private final JIPipeArtifactsRegistry artifactsRegistry;
    private final JIPipeNodeTemplateRegistry nodeTemplateRegistry;
    private final JIPipeRecentProjectsRegistry recentProjectsRegistry;
    private final JIPipeMetadataRegistry metadataRegistry;

    private FilesCollection imageJPlugins = null;

    @Parameter
    private LogService logService;
    @Parameter
    private PluginService pluginService;

    private final JIPipeDatatypeRegisteredEventEmitter datatypeRegisteredEventEmitter = new JIPipeDatatypeRegisteredEventEmitter();
    private final JIPipePluginDiscoveredEventEmitter extensionDiscoveredEventEmitter = new JIPipePluginDiscoveredEventEmitter();
    private final JIPipePluginRegisteredEventEmitter extensionRegisteredEventEmitter = new JIPipePluginRegisteredEventEmitter();
    private final JIPipeNodeInfoRegisteredEventEmitter nodeInfoRegisteredEventEmitter = new JIPipeNodeInfoRegisteredEventEmitter();

    public JIPipeService() {
        recentProjectsRegistry = new JIPipeRecentProjectsRegistry(this);
        nodeRegistry = new JIPipeNodeRegistry(this);
        datatypeRegistry = new JIPipeDatatypeRegistry(this);
        imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry(this);
        customMenuRegistry = new JIPipeCustomMenuRegistry(this);
        parameterTypeRegistry = new JIPipeParameterTypeRegistry(this);
        applicationSettingsRegistry = new JIPipeApplicationSettingsRegistry(this);
        projectSettingsRegistry = new JIPipeProjectSettingsRegistry(this);
        tableOperationRegistry = new JIPipeExpressionRegistry(this);
        utilityRegistry = new JIPipeUtilityRegistry(this);
        externalEnvironmentRegistry = new JIPipeExternalEnvironmentRegistry(this);
        pluginRegistry = new JIPipePluginRegistry(this);
        projectTemplateRegistry = new JIPipeProjectTemplateRegistry(this);
        graphEditorToolRegistry = new JIPipeGraphEditorToolRegistry(this);
        metadataRegistry = new JIPipeMetadataRegistry(this);
        artifactsRegistry = new JIPipeArtifactsRegistry(this);
        nodeTemplateRegistry = new JIPipeNodeTemplateRegistry(this);
    }

    public JIPipeServiceMode getMode() {
        return mode;
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

    public JIPipeExternalEnvironmentRegistry getExternalEnvironmentRegistry() {
        return externalEnvironmentRegistry;
    }


    public JIPipeUtilityRegistry getUtilityRegistry() {
        return utilityRegistry;
    }


    public JIPipeArtifactsRegistry getArtifactsRegistry() {
        return artifactsRegistry;
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    public JIPipeParameterTypeRegistry getParameterTypeRegistry() {
        return parameterTypeRegistry;
    }

    public JIPipeApplicationSettingsRegistry getApplicationSettingsRegistry() {
        return applicationSettingsRegistry;
    }

    public JIPipeProjectSettingsRegistry getProjectSettingsRegistry() {
        return projectSettingsRegistry;
    }

    public JIPipeMetadataRegistry getMetadataRegistry() {
        return metadataRegistry;
    }

    public JIPipeExpressionRegistry getExpressionRegistry() {
        return tableOperationRegistry;
    }

    public JIPipeRecentProjectsRegistry getRecentProjectsRegistry() {
        return recentProjectsRegistry;
    }

    public JIPipePluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    public JIPipeNodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    public JIPipeDatatypeRegistry getDatatypeRegistry() {
        return datatypeRegistry;
    }

    public JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry() {
        return imageJDataAdapterRegistry;
    }

    public List<JIPipeDependency> getRegisteredExtensions() {
        return Collections.unmodifiableList(registeredExtensions);
    }

    public JIPipeCustomMenuRegistry getCustomMenuRegistry() {
        return customMenuRegistry;
    }

    public JIPipeNodeTemplateRegistry getNodeTemplateRegistry() {
        return nodeTemplateRegistry;
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

    public JIPipeExpressionRegistry getTableOperationRegistry() {
        return tableOperationRegistry;
    }


    public JIPipeGraphEditorToolRegistry getGraphEditorToolRegistry() {
        return graphEditorToolRegistry;
    }


    public JIPipeProjectTemplateRegistry getProjectTemplateRegistry() {
        return projectTemplateRegistry;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public List<JIPipeDependency> getFailedExtensions() {
        return Collections.unmodifiableList(failedExtensions);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report) {
        report.report(reportContext, nodeRegistry);
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
}
