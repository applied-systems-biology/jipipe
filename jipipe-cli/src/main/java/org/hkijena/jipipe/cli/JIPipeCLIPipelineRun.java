package org.hkijena.jipipe.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.settings.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JIPipeCLIPipelineRun {
    public static void doRunPipeline(List<String> argsList) {
        Path projectFile = null;
        Path outputFolder = null;
        int numThreads = 1;
        boolean saveToDisk = true;
        boolean saveToDiskOnlyCompartments = false;

        boolean verbose = false;
        boolean fastInit = false;
        Path overrideProfileDir = null;

        Map<String, String> parameterOverrides = new HashMap<>();
        Map<String, Path> userDirectoryOverrides = new HashMap<>();

        // Parse flags
        for (int i = 0; i < argsList.size(); i++) {
            String arg = argsList.get(i);
            boolean success = false;
            if (arg.equals("--fast-init")) {
                fastInit = true;
                success = true;
            } else if (arg.equals("--verbose")) {
                verbose = true;
                success = true;
            }

            // Delete the flag
            if (success) {
                argsList.remove(i);
                --i;
            }
        }

        // Parse key value pairs
        for (int i = 0; i < argsList.size(); i += 2) {
            String arg = argsList.get(i);
            String value = argsList.get(i + 1);
            if (arg.equals("--project")) {
                projectFile = Paths.get(value);
            } else if (arg.equals("--output-folder")) {
                outputFolder = Paths.get(value);
            } else if (arg.equals("--num-threads")) {
                numThreads = Integer.parseInt(value);
            } else if (arg.equals("--overwrite-parameters")) {
                try {
                    JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(new File(value));
                    for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(node.fields())) {
                        parameterOverrides.put(entry.getKey(), JsonUtils.toJsonString(entry.getValue()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (arg.equals("--overwrite-user-directories")) {
                try {
                    JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(new File(value));
                    for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(node.fields())) {
                        userDirectoryOverrides.put(entry.getKey(), Paths.get(entry.getValue().textValue()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (arg.equals("--output-results")) {
                switch (value) {
                    case "all":
                        saveToDisk = true;
                        saveToDiskOnlyCompartments = false;
                        break;
                    case "none":
                        saveToDisk = false;
                        saveToDiskOnlyCompartments = false;
                        break;
                    case "only-compartment-outputs":
                        saveToDisk = true;
                        saveToDiskOnlyCompartments = true;
                        break;
                    default:
                        System.err.println("Unknown disk saving setting: " + value);
                        JIPipeCLIHelp.showHelp();
                        return;
                }
            } else if (arg.startsWith("--P")) {
                parameterOverrides.put(arg.substring(3), value);
            } else if (arg.startsWith("--U")) {
                userDirectoryOverrides.put(arg.substring(3), Paths.get(value));
            } else if (arg.equals("--profile-dir")) {
                overrideProfileDir = Paths.get(value);
            } else {
                System.err.println("Unknown argument: " + arg);
                JIPipeCLIHelp.showHelp();
                return;
            }
        }

        if (projectFile == null || !Files.exists(projectFile)) {
            System.err.println("Project file does not exist!");
            JIPipeCLIHelp.showHelp();
            return;
        }
        if (numThreads < 1) {
            System.err.println("Invalid number of threads!");
            JIPipeCLIHelp.showHelp();
            return;
        }
        if (overrideProfileDir != null) {
            System.out.println("Overriding base path for JIPipe profiles with " + overrideProfileDir);
            PathUtils.createDirectories(overrideProfileDir);
            JIPipe.OVERRIDE_USER_DIR_BASE = overrideProfileDir;
        }

        final ImageJ ij = new ImageJ();
        JIPipe jiPipe = JIPipe.createInstance(ij.context());
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        extensionSettings.setSilent(true);
        if (fastInit) {
            extensionSettings.setValidateNodeTypes(false);
        }
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        jiPipe.initialize(extensionSettings, issues, verbose);

        JIPipeValidationReport projectIssues = new JIPipeValidationReport();
        JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile, new UnspecifiedValidationReportContext(), projectIssues, notifications);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Overwrite parameters
        for (Map.Entry<String, String> entry : parameterOverrides.entrySet()) {
            String[] components = entry.getKey().split("/");
            String nodeId = components[0];
            String parameterId = String.join("/", Arrays.copyOfRange(components, 1, components.length));

            JIPipeParameterAccess access;

            if(StringUtils.isNullOrEmpty(nodeId)) {
                System.out.println("Setting parameter nodeId='" + nodeId + "' parameterId='" + parameterId + "' to " + entry.getValue());

                JIPipeGraphNode node = project.getGraph().findNode(nodeId);
                if (node == null) {
                    throw new RuntimeException("Could not find node nodeId='" + nodeId + "'. Check if the UUID or alias ID are provided in the pipeline.");
                }

                JIPipeParameterTree tree = new JIPipeParameterTree(node);
                access = tree.getParameters().getOrDefault(parameterId, null);

                if (access == null) {
                    throw new RuntimeException("Could not find parameter parameterId='" + parameterId + "' in nodeId='" + nodeId + "'. Check if the UUID or alias ID are provided in the pipeline.");
                }
            }
            else {
                System.out.println("Setting global parameter parameterId='" + parameterId + "' to " + entry.getValue());
                access = project.getMetadata().getGlobalParameters().get(parameterId);

                if (access == null) {
                    throw new RuntimeException("Could not find parameter parameterId='" + parameterId + "' in the set of global parameters.");
                }
            }

            Object value;
            try {
                value = JsonUtils.getObjectMapper().readerFor(access.getFieldClass()).readValue(entry.getValue());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Could not read parameter value '" + entry.getValue() + "' into data type " + access.getFieldClass() + "!");
            }

            access.set(value);
        }

        // Overwrite user directories
        for (Map.Entry<String, Path> entry : userDirectoryOverrides.entrySet()) {
            System.out.println("Setting user directory " + entry.getKey() + "=" + entry.getValue());
            project.getMetadata().getDirectories().setUserDirectory(entry.getKey(), entry.getValue());
        }

        project.reportValidity(new UnspecifiedValidationReportContext(), projectIssues);
        projectIssues.print();

        if (!notifications.isEmpty()) {
            System.err.println("The following notifications were generated:");
            for (JIPipeNotification notification : notifications.getNotifications()) {
                System.err.println("- " + notification.getHeading() + " [" + notification.getId() + "]");
                System.err.println("  " + notification.getDescription());
                if (!notification.getActions().isEmpty()) {
                    System.err.println("  -->> GUI actions detected. Please run the JIPipe GUI to execute them");
                }
            }
        }

        if (outputFolder == null) {
            System.out.println("No output folder provided.");
            outputFolder = project.newTemporaryDirectory("output");
            System.out.println("Output files will be written into: " + outputFolder);
        }

        JIPipeGraphRunConfiguration settings = new JIPipeGraphRunConfiguration();
        settings.setNumThreads(numThreads);
        settings.setOutputPath(outputFolder);
        settings.setStoreToDisk(saveToDisk);
        settings.setStoreToCache(false);
        if (saveToDisk && saveToDiskOnlyCompartments) {
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                if (!(graphNode instanceof JIPipeProjectCompartmentOutput)) {
                    settings.getDisableStoreToDiskNodes().add(graphNode.getUUIDInParentGraph());
                }
            }
        }

        JIPipeGraphRun run = new JIPipeGraphRun(project, settings);
        run.getProgressInfo().setLogToStdOut(true);
        run.run();

//        System.exit(0); // unreliable due to bug in scijava
        Runtime.getRuntime().halt(0);
    }
}
