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

package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.*;
import org.hkijena.jipipe.api.environments.sources.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.service.components.JIPipeArtifactsServiceComponent;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Class that helps with keeping track of where environments are sourced from and resolve full environments.
 * Resolves a base environment and automatically targets a compatible environment within the chain [Node, Project, Application, Fallback].
 * Individual chain links can be left out. For artifact environments, the fallback is always the configured artifact query.
 */
public class JIPipeEnvironmentConfigurator<T extends JIPipeEnvironment> implements JIPipeValidatable {
    private final Class<T> environmentClass;
    private final JIPipeEnvironmentConfigurationCache configurationCache;
    private final JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo;
    private T baseEnvironment;
    private SourceType sourceType;
    private Object source;
    private JIPipeArtifactOperationContext artifactOperationContext;
    private List<JIPipeEnvironmentConfiguratorSource<T>> configuratorSources = new ArrayList<>();

    /**
     * Initializes a new configurator using the standard set of sources (node, project, applicatio, fallback)
     *
     * @param environmentClass   the target environment class
     * @param configurationCache the configuration cache where preconfigured environments are stored
     * @param graphNode          the graph node. Can be null.
     * @param project            the project. Can be null.
     */
    public JIPipeEnvironmentConfigurator(Class<T> environmentClass, JIPipeEnvironmentConfigurationCache configurationCache, JIPipeGraphNode graphNode, JIPipeProject project) {
        this.environmentClass = environmentClass;
        this.environmentInfo = JIPipe.getInstance().getEnvironments().getInfoByClass(environmentClass);
        if (graphNode != null) {
            configuratorSources.add(new JIPipeEnvironmentConfiguratorNodeSource<>(graphNode));
        }
        if (project != null) {
            configuratorSources.add(new JIPipeEnvironmentConfiguratorProjectSource<>(project));
        }
        configuratorSources.add(new JIPipeEnvironmentConfiguratorApplicationSource<>());
        configuratorSources.add(new JIPipeEnvironmentConfiguratorFallbackSource<>());
        this.configurationCache = configurationCache;
    }

    /**
     * Initializes a new configurator using a custom set of sources.
     * Please note that even the application and the fallback sources will not be added!
     *
     * @param environmentClass   the target environment class
     * @param configurationCache the configuration cache where preconfigured environments are stored
     */
    public JIPipeEnvironmentConfigurator(Class<T> environmentClass, JIPipeEnvironmentConfigurationCache configurationCache, JIPipeEnvironmentConfiguratorSource<T>... configuratorSources) {
        this.environmentClass = environmentClass;
        this.environmentInfo = JIPipe.getInstance().getEnvironments().getInfoByClass(environmentClass);
        this.configurationCache = configurationCache;
        this.configuratorSources.addAll(List.of(configuratorSources));
    }

    /**
     * Gets a fully resolved and configured environment.
     * Utilizes the cached value if possible.
     *
     * @param progressInfo the progress info
     * @return a cached and fully configured environment
     */
    public T get(JIPipeProgressInfo progressInfo) {
        resolveBaseEnvironment(progressInfo.resolve("Resolve environment " + environmentInfo.getId()));
        if (baseEnvironment == null) {
            return null;
        }
        JIPipeEnvironment environment = configurationCache.get(baseEnvironment);
        if (environment == null) {
            return configure(progressInfo);
        }
        return (T) environment;
    }

    public <V extends JIPipeEnvironmentConfiguratorSource<T>> V getFirstSourceOfType(Class<V> sourceClass) {
        for (JIPipeEnvironmentConfiguratorSource<T> source : configuratorSources) {
            if (source.getClass().equals(sourceClass)) {
                return (V) source;
            }
        }
        return null;
    }

    /**
     * UI-based action that guides users through the configuration if necessary.
     *
     * @param workbench the workbench
     * @param parent    the parent component
     * @param title     the dialog title
     * @param action    the action
     */
    public void showDialogAndGetLater(JIPipeDesktopWorkbench workbench, Component parent, String title, Consumer<T> action) {
        resolveBaseEnvironment(JIPipeProgressInfo.SILENT);
        if (baseEnvironment == null) {
            String errorMessage = "<html><p>Unable to find a suitable environment for '" + environmentInfo.getName() + "'.</p>";
            if (environmentInfo.getArchetype() == JIPipeEnvironmentArchetype.Managed) {
                errorMessage += "<ul>";

                JIPipeEnvironmentConfiguratorNodeSource<T> nodeSource = getFirstSourceOfType(JIPipeEnvironmentConfiguratorNodeSource.class);
                JIPipeEnvironmentConfiguratorProjectSource<T> projectSource = getFirstSourceOfType(JIPipeEnvironmentConfiguratorProjectSource.class);

                if (nodeSource != null && nodeSource.getSource() != null) {
                    errorMessage += "<li>Check if you have a wrongly configured environment override in the node '" + nodeSource.getGraphNode().getDisplayName() + "'</li>";
                }
                if (projectSource != null && projectSource.getProject() != null) {
                    errorMessage += "<li>Please check Project &gt; Project settings &gt; General &gt; Connected services</li>";
                }
                errorMessage += "<li>P>Please check Project &gt; Application settings &gt; General &gt; Connected services</li>";
                errorMessage += "</ul>";
            }
            errorMessage += "</html>";
            JOptionPane.showMessageDialog(parent, errorMessage, title, JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeEnvironment environment = configurationCache.get(baseEnvironment);
        if (environment == null) {

            // Ask the user if they are prepared for downloading the artifact package
            T configuredEnvironment = JIPipe.duplicateParameter(baseEnvironment);
            if (configuredEnvironment instanceof JIPipeArtifactEnvironment configuredArtifactEnvironment) {
                if (configuredArtifactEnvironment.isLoadFromArtifact()) {
                    JIPipeArtifact artifact = configureArtifactQuery(configuredArtifactEnvironment, JIPipeProgressInfo.SILENT);
                    if (artifact instanceof JIPipeRemoteArtifact) {
                        if (JOptionPane.showConfirmDialog(parent, "<html>JIPipe will need to download the artifact package <pre>" + artifact.getFullId() + "</pre> " +
                                "Depending on the package and your internet connection this will take a few minutes.<br/>Do you want to continue?</html>", title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                }
            }

            // Do the run configuration within a dialog
            JIPipeRunnable run = new DefaultJIPipeRunnable() {
                @Override
                public String getTaskLabel() {
                    return "Configure environment";
                }

                @Override
                public void run() {
                    configure(getProgressInfo());
                }

                @Override
                public void onFinished(FinishedEvent event) {
                    action.accept(get(getProgressInfo()));
                }

                @Override
                public void onInterrupted(InterruptedEvent event) {
                    JOptionPane.showMessageDialog(parent, title, "The configuration failed or was interrupted.", JOptionPane.ERROR_MESSAGE);
                }
            };

            JIPipeDesktopRunExecuteUI.runInDialog(workbench, parent, run);
        } else {
            action.accept((T) environment);
        }
    }

    /**
     * Fully configures the environment and stores the resulting fully configured environment into the cache
     * Also ensures that artifacts are downloaded.
     * Please note that this function will do a FULL RECONFIGURE. Use get() if you just want the environment.
     *
     * @param progressInfo the progress info
     * @return the fully configured environment
     */
    public T configure(JIPipeProgressInfo progressInfo) {
        resolveBaseEnvironment(progressInfo);
        if (baseEnvironment == null) {
            progressInfo.log("[ERROR] Configuration not possible without base environment!");
            throw new IllegalStateException("[ERROR] Configuration not possible without base environment!");
        }

        // We will create a copy where parameters are fully configured
        T configuredEnvironment = JIPipe.duplicateParameter(baseEnvironment);

        if (configuredEnvironment instanceof JIPipeArtifactEnvironment configuredArtifactEnvironment) {
            if (configuredArtifactEnvironment.isLoadFromArtifact()) {
                // Artifact environments require two steps: (1) final artifact resolution (2) artifact download
                JIPipeArtifact artifact = configureArtifactQuery(configuredArtifactEnvironment, progressInfo.resolve("Artifact configuration"));

                // Write the full ID into the environment, so the artifact system can later do the autoconfiguration
                configuredArtifactEnvironment.setArtifactQuery(new JIPipeArtifactQueryParameter(artifact.getFullId(JIPipeArtifact.ResolutionStatus.Full)));

                if (artifact instanceof JIPipeRemoteArtifact) {
                    downloadArtifact((JIPipeRemoteArtifact) artifact, progressInfo);
                    artifact = configureArtifactQuery(configuredArtifactEnvironment, progressInfo.resolve("Artifact configuration"));
                }
                if (!(artifact instanceof JIPipeLocalArtifact)) {
                    throw new IllegalStateException("Artifact download was unsuccessful: " + artifact.getFullId() + " not local artifact after download!");
                }

                // Apply the final configuration
                configuredArtifactEnvironment.applyConfigurationFromArtifactAndSetLastArtifact((JIPipeLocalArtifact) artifact,
                        progressInfo.resolve("Configure environment from artifact"));
            }
        }

        progressInfo.log("Success! Storing " + configuredEnvironment + " into cache");
        configurationCache.put(baseEnvironment, configuredEnvironment);

        return configuredEnvironment;
    }

    private void downloadArtifact(JIPipeRemoteArtifact remoteArtifact, JIPipeProgressInfo progressInfo) {
        JIPipeArtifactRepositoryApplyInstallUninstallRun run = new JIPipeArtifactRepositoryApplyInstallUninstallRun(
                List.of(remoteArtifact), Collections.emptyList());
        run.setExternalContext(artifactOperationContext); // Needed to prevent deadlock for nested runs
        run.setProgressInfo(progressInfo);
        run.run();
    }

    private JIPipeArtifact configureArtifactQuery(JIPipeArtifactEnvironment configuredArtifactEnvironment, JIPipeProgressInfo progressInfo) {
        String requestedArtifactId = configuredArtifactEnvironment.getArtifactQuery().getQuery();
        List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(requestedArtifactId);
        artifacts.removeIf(artifact -> !artifact.isCompatible());
        progressInfo.log("Found " + artifacts.size() + " compatible matching artifacts");
        JIPipeArtifact targetArtifact = null;

        if (artifacts.isEmpty()) {
            // Find alternative
            progressInfo.log("Unable to find compatible matching artifact! Finding alternative!");
            artifacts = JIPipe.getArtifacts().queryCachedArtifacts(requestedArtifactId);
            for (JIPipeArtifact artifact : artifacts) {
                JIPipeArtifact closestCompatibleArtifact = JIPipe.getArtifacts().findClosestCompatibleArtifact(artifact.getFullId());
                if (closestCompatibleArtifact != null) {
                    progressInfo.log("SUCCEEDED in finding closest compatible artifact to " + artifact.getFullId() + " as " + closestCompatibleArtifact.getFullId());
                    targetArtifact = closestCompatibleArtifact;
                    break;
                } else {
                    progressInfo.log("FAILED to find closest compatible artifact to " + artifact.getFullId());
                }
            }
        } else if (artifacts.size() > 1) {
            progressInfo.log("Warning: found " + artifacts.size() + " matching artifacts:");
            for (JIPipeArtifact artifact : artifacts) {
                progressInfo.log("- " + artifact.getFullId());
            }
            targetArtifact = JIPipeArtifactsServiceComponent.selectPreferredArtifactByClassifier(artifacts);
            progressInfo.log("Based on current preferences, selecting -> " + targetArtifact.getFullId());
        } else {
            targetArtifact = artifacts.getFirst();
        }

        if (targetArtifact == null) {
            throw new RuntimeException("Unable to find matching compatible artifact for " + requestedArtifactId + ". Unable to continue.");
        }

        progressInfo.log("Matched artifact: " + targetArtifact.getFullId());
        if (targetArtifact instanceof JIPipeRemoteArtifact) {
            progressInfo.log("[INFO] Artifact is not downloaded");
        }

        return targetArtifact;
    }

    /**
     * Ensures that the base (unconfigured) environment is selected and internally tracked
     */
    public void resolveBaseEnvironment(JIPipeProgressInfo progressInfo) {
        if (baseEnvironment != null) {
            // Already resolved
            return;
        }

        for (JIPipeEnvironmentConfiguratorSource<T> configuratorSource : configuratorSources) {
            progressInfo.log("Trying " + configuratorSource + " [" + configuratorSource.getSourceType() + "] ...");
            JIPipeOptionalParameter<T> resolved = configuratorSource.resolve(environmentClass, environmentInfo);
            if (resolved != null && resolved.isEnabled()) {
                sourceType = SourceType.Node;
                source = configuratorSource.getSource();
                baseEnvironment = (T) resolved.getContent();
                progressInfo.log("Success!");
                progressInfo.log("Base environment of type " + environmentInfo.getId() + " = " + baseEnvironment);
                return;
            }
        }

        progressInfo.log("Failed!");
    }

    /**
     * Gets the source type of the current environment.
     * Automatically resolves the base environment.
     *
     * @return the source type
     */
    public SourceType getSourceType() {
        resolveBaseEnvironment(JIPipeProgressInfo.SILENT);
        return sourceType;
    }

    /**
     * Gets the environment source object (null for application-wide or fallback, the project, the node)
     * Automatically resolves the base environment.
     *
     * @return the source object
     */
    public Object getSource() {
        resolveBaseEnvironment(JIPipeProgressInfo.SILENT);
        return source;
    }

    public JIPipeEnvironmentConfigurationCache getConfigurationCache() {
        return configurationCache;
    }

    public T getBaseEnvironment() {
        resolveBaseEnvironment(JIPipeProgressInfo.SILENT);
        return baseEnvironment;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        resolveBaseEnvironment(JIPipeProgressInfo.SILENT);
        if (!getBaseEnvironment().generateValidityReport(new UnspecifiedValidationReportContext(), reportSettings, progressInfo).isValid()) {
            JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(getBaseEnvironment().getClass());
            switch (getSourceType()) {
                case SourceType.Application -> {
                    new UnspecifiedValidationReportContext().error()
                            .title("Misconfigured connected service")
                            .explanation("An application-wide connected service of the type '" + info.getName() + "' is invalid. The project cannot to be run.")
                            .solution("Please go to Project > Application settings > General > Connected services and find the configuration for '" + info.getName() + "'. " +
                                    "Ensure that the service is correctly configured or disable the override (if available).")
                            .report(report);
                }
                case SourceType.Project -> {
                    var context = getSource() instanceof JIPipeProject ? reportContext.projectSettings((JIPipeProject) getSource()) : JIPipeValidationReportContext.UNSPECIFIED;
                    context.error()
                            .title("Misconfigured environment")
                            .explanation("A project connected service of the type '" + info.getName() + "' is invalid. The project cannot to be run.")
                            .solution("Please go to Project > Project settings > General > Connected services and find the configuration for '" + info.getName() + "'. Ensure that the service is correctly configured.")
                            .report(report);
                }
                case SourceType.Node -> {
                    var context = getSource() instanceof JIPipeGraphNode ? reportContext.node((JIPipeGraphNode) getSource()) : JIPipeValidationReportContext.UNSPECIFIED;
                    context.error()
                            .title("Misconfigured environment")
                            .explanation("A connected service of the type '" + info.getName() + "' that is configured using a node-specific override is invalid. The project cannot to be run.")
                            .solution("Please go to the affected node and find the connected service override for '" + info.getName() + "'. Ensure that the service is correctly configured.")
                            .report(report);
                }
                case SourceType.Custom -> {
                    JIPipeValidationReportContext.UNSPECIFIED.error()
                            .title("Misconfigured environment")
                            .explanation("A connected service of the type '" + info.getName() + "' that is configured using a node-specific override is invalid. The project cannot to be run.")
                            .solution("Ensure that the service is correctly configured.")
                            .report(report);
                }
            }
        }
    }

    public Class<T> getEnvironmentClass() {
        return environmentClass;
    }

    public JIPipeEnvironmentsServiceComponent.EnvironmentInfo getEnvironmentInfo() {
        return environmentInfo;
    }

    public JIPipeArtifactOperationContext getArtifactOperationContext() {
        return artifactOperationContext;
    }

    public void setArtifactOperationContext(JIPipeArtifactOperationContext artifactOperationContext) {
        this.artifactOperationContext = artifactOperationContext;
    }

    public List<JIPipeEnvironmentConfiguratorSource<T>> getConfiguratorSources() {
        return configuratorSources;
    }

    public void setConfiguratorSources(List<JIPipeEnvironmentConfiguratorSource<T>> configuratorSources) {
        this.configuratorSources = configuratorSources;
    }

    public enum SourceType {
        Node,
        Project,
        Application,
        Custom,
        Fallback
    }

}
