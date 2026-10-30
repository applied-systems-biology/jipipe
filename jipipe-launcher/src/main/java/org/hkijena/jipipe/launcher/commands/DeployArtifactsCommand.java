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

package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryInstallArtifactRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializationSettings;
import org.hkijena.jipipe.api.service.JIPipeServiceMode;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command for downloading and deploying artifacts
 */
public class DeployArtifactsCommand {
    public static void doInstallArtifacts(List<String> argsList) {
        Target target = Target.none;
        Set<String> customQueries = new HashSet<>();
        for (int i = 0; i < argsList.size(); i += 2) {
            String arg = argsList.get(i);
            String value = argsList.get(i + 1);

            if ("--target".equals(arg)) {
                target = Target.valueOf(value);
            } else if ("--query".equals(arg)) {
                customQueries.add(value);
            }
        }

        if (customQueries.isEmpty()) {
            customQueries.add("*");
        }

        // Initialize JIPipe
        System.out.println("Initializing JIPipe ...");
        JIPipeServiceInitializationSettings initializationSettings = new JIPipeServiceInitializationSettings();
        initializationSettings.setMode(JIPipeServiceMode.Headless);
        initializationSettings.setVerbose(false);

        final ImageJ ij = new ImageJ();
        JIPipeService service = JIPipe.createInstance(ij.context(), initializationSettings);
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        extensionSettings.setSilent(true);
        extensionSettings.setValidateNodeTypes(false);
        service.ensureInitialized();

        // Query the repository
        System.out.println("Discovering artifacts ...");
        List<JIPipeArtifact> cachedArtifacts = service.getArtifacts().queryCachedArtifacts(customQueries.toArray(new String[0]));
        System.out.println("Found " + cachedArtifacts.size() + " artifacts matching the queries");

        // Filter artifacts
        cachedArtifacts.removeIf(DeployArtifactsCommand::isInstalledOrIncompatibleArtifact);
        System.out.println(cachedArtifacts.size() + " artifacts are compatible and not installed yet");

        // Secondary filter (read-only target)
        if(target == Target.readonly) {
            cachedArtifacts.removeIf(artifact -> {
                boolean result = isAssociatedToReadOnlyTarget(artifact, service);
                if(result) {
                    System.out.println("Removing " + artifact.getFullId() + " - marked as not compatible with read-only deployment!");
                }
                return result;
            });
            System.out.println(cachedArtifacts.size() + " artifacts are compatible and not installed yet and can be deployed read-only");
        }

        for (JIPipeArtifact artifact : cachedArtifacts) {
            System.out.println("-> " + artifact.getFullId());
        }

        // Install artifacts
        JIPipeProgressInfo installProgress = JIPipeProgressInfo.STDOUT;
        for (int i = 0; i < cachedArtifacts.size(); i++) {
            JIPipeArtifact artifact = cachedArtifacts.get(i);
            JIPipeProgressInfo installArtifactProgress = installProgress.resolveAndLog(artifact.getFullId(), i, cachedArtifacts.size());
            JIPipeArtifactRepositoryInstallArtifactRun run = new JIPipeArtifactRepositoryInstallArtifactRun((JIPipeRemoteArtifact) artifact);
            run.setProgressInfo(installArtifactProgress);
            run.run();
        }

        // Deploy artifacts
        if(target != Target.none) {
            JIPipeProgressInfo deployProgress = JIPipeProgressInfo.STDOUT;
            System.out.println("Executing artifact deployment scripts ...");
            Set<String> configuredArtifactIds = new HashSet<>();
            for (Map.Entry<String, JIPipeEnvironmentsServiceComponent.EnvironmentInfo> entry : service.getEnvironments().getInfosById().entrySet()) {
                JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = entry.getValue();
                if(!environmentInfo.hasArtifactQuery()) {
                    continue;
                }
                if(JIPipeArtifactEnvironment.class.isAssignableFrom(environmentInfo.getEnvironmentClass())) {
                    JIPipeProgressInfo deployEnvironmentProgress = deployProgress.resolve(entry.getKey());
                    JIPipeArtifactEnvironment environment = createEnvironmentInstance(service, environmentInfo);
                    if(!environment.isAllowReadOnlyDeployment() && target ==Target.readonly) {
                        deployEnvironmentProgress.log("Skipping artifact environment ID=" + entry.getKey() + " -> not compatible with read-only deployment");
                        continue;
                    }
                    deployEnvironmentProgress.log("Deploying artifacts for environment ID=" + entry.getKey() + " ...");
                    List<JIPipeArtifact> cachedArtifactsForEnvironment = service.getArtifacts().queryCachedArtifacts(environmentInfo.getArtifactQuery());
                    for (JIPipeArtifact artifact : cachedArtifactsForEnvironment) {
                        if(artifact instanceof JIPipeLocalArtifact localArtifact) {
                            JIPipeProgressInfo deployEnvironmentArtifactProgress = deployEnvironmentProgress.resolve(localArtifact.getFullId());
                            if(configuredArtifactIds.contains(localArtifact.getFullId())) {
                                deployEnvironmentArtifactProgress.log("Skipping deployment of " + localArtifact.getFullId() + " (already applied)");
                                continue;
                            }
                            deployEnvironmentArtifactProgress.log("Running deployment scripts for " + localArtifact.getFullId());
                            configuredArtifactIds.add(localArtifact.getFullId());

                            JIPipeArtifactEnvironment tmpEnvironment = createEnvironmentInstance(service, environmentInfo);
                            tmpEnvironment.setLoadFromArtifact(true);
                            tmpEnvironment.setArtifactQuery(new JIPipeArtifactQueryParameter(localArtifact.getFullId()));
                            tmpEnvironment.applyConfigurationFromArtifact(localArtifact, deployEnvironmentArtifactProgress);
                        }
                    }

                }
            }
        }
        else {
            System.out.println("Artifact deployment is disabled.");
        }

        System.out.println("Success!");
    }

    private static JIPipeArtifactEnvironment createEnvironmentInstance(JIPipeService service, JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo) {
        return (JIPipeArtifactEnvironment) service.getParameterTypes().getInfoByFieldClass(environmentInfo.getEnvironmentClass()).newInstance();
    }

    private static boolean isAssociatedToReadOnlyTarget(JIPipeArtifact artifact, JIPipeService service) {
        for (Map.Entry<String, JIPipeEnvironmentsServiceComponent.EnvironmentInfo> entry : service.getEnvironments().getInfosById().entrySet()) {
            JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = entry.getValue();
            if (!environmentInfo.hasArtifactQuery()) {
                continue;
            }
            if(artifact.matchesQuery(environmentInfo.getArtifactQuery())) {
                JIPipeArtifactEnvironment environmentInstance = createEnvironmentInstance(service, environmentInfo);
                if(!environmentInstance.isAllowReadOnlyDeployment()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isInstalledOrIncompatibleArtifact(JIPipeArtifact artifact) {
        if(artifact instanceof JIPipeLocalArtifact) {
            return true;
        }
        if(!artifact.isCompatible()) {
            return true;
        }
        return false;
    }

    private enum Target {
        all,
        none,
        readonly
    }
}
