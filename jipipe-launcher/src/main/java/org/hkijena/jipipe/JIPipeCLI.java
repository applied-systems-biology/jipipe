package org.hkijena.jipipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import net.imagej.ImageJ;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JIPipeCLI {
    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        int firstArgIndex = 0;

        if (argsList.contains("help")) {
            showHelp();
            return;
        }
        if (argsList.contains("run")) {
            int runIndex = argsList.lastIndexOf("run");
            while (runIndex > 0) {
                argsList.remove(0);
                --runIndex;
            }
        } else {
            showHelp();
            return;
        }

        // remove run
        argsList.remove(0);

        Path projectFile = null;
        Path outputFolder = null;
        int numThreads = 1;
        boolean saveToDisk = true;
        boolean saveToDiskOnlyCompartments = false;
        Map<String, String> parameterOverrides = new HashMap<>();
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
            }
            else if(arg.equals("--save-to-disk")) {
                if(value.equals("on")) {
                    saveToDisk = true;
                    saveToDiskOnlyCompartments = false;
                }
                else if(value.equals("off")) {
                    saveToDisk = false;
                    saveToDiskOnlyCompartments = false;
                }
                else if(value.equals("only-compartment-outputs")) {
                    saveToDisk = true;
                    saveToDiskOnlyCompartments = true;
                }
                else {
                    System.err.println("Unknown disk saving setting: " + value);
                    showHelp();
                    return;
                }
            }
            else if (arg.startsWith("--P")) {
                parameterOverrides.put(arg.substring(3), value);
            } else {
                System.err.println("Unknown argument: " + arg);
                showHelp();
                return;
            }
        }

        if (projectFile == null || !Files.exists(projectFile)) {
            System.err.println("Project file does not exist!");
            showHelp();
            return;
        }
        if (outputFolder == null) {
            System.err.println("Please provide an output folder!");
            showHelp();
            return;
        }
        if (numThreads < 1) {
            System.err.println("Invalid number of threads!");
            showHelp();
            return;
        }
        final ImageJ ij = new ImageJ();
        JIPipe jiPipe = JIPipe.createInstance(ij.context());
        ExtensionSettings extensionSettings = ExtensionSettings.getInstanceFromRaw();
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        jiPipe.initialize(extensionSettings, issues);

        JIPipeIssueReport projectIssues = new JIPipeIssueReport();
        JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile, projectIssues, notifications);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Overwrite parameters
        for (Map.Entry<String, String> entry : parameterOverrides.entrySet()) {
            String[] components = entry.getKey().split("/");
            String nodeId = components[0];
            String parameterId = String.join("/", Arrays.copyOfRange(components, 1, components.length));
            System.out.println("Setting parameter nodeId='" + nodeId + "' parameterId='" + parameterId + "' to " + entry.getValue());

            JIPipeGraphNode node = project.getGraph().findNode(nodeId);
            if (node == null) {
                throw new RuntimeException("Could not find node nodeId='" + nodeId + "'. Check if the UUID or alias ID are provided in the pipeline.");
            }

            JIPipeParameterTree tree = new JIPipeParameterTree(node);
            JIPipeParameterAccess access = tree.getParameters().getOrDefault(parameterId, null);

            if (access == null) {
                throw new RuntimeException("Could not find parameter parameterId='" + parameterId + "' in nodeId='" + nodeId + "'. Check if the UUID or alias ID are provided in the pipeline.");
            }

            Object value;
            try {
                value = JsonUtils.getObjectMapper().readerFor(access.getFieldClass()).readValue(entry.getValue());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Could not read parameter value '" + entry.getValue() + "' into data type " + access.getFieldClass() + "!");
            }

            access.set(value);
        }

        project.reportValidity(projectIssues);
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

        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setNumThreads(numThreads);
        settings.setOutputPath(outputFolder);
        settings.setSaveToDisk(saveToDisk);
        settings.setStoreToCache(false);
        if(saveToDisk && saveToDiskOnlyCompartments) {
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                if(!(graphNode instanceof JIPipeCompartmentOutput)) {
                    settings.getDisableSaveToDiskNodes().add(graphNode.getUUIDInParentGraph());
                }
            }
        }

        JIPipeProjectRun run = new JIPipeProjectRun(project, settings);
        run.getProgressInfo().setLogToStdOut(true);
        run.run();

        System.exit(0);
    }

    private static void showHelp() {
        System.out.println("\n" +
                "                                         \n" +
                "     ,--.,--.,------. ,--.               \n" +
                "     |  ||  ||  .--. '`--' ,---.  ,---.  \n" +
                ",--. |  ||  ||  '--' |,--.| .-. || .-. : \n" +
                "|  '-'  /|  ||  | --' |  || '-' '\\   --. \n" +
                " `-----' `--'`--'     `--'|  |-'  `----' \n" +
                "                          `--'           \n\n");
        System.out.println("Part of JIPipe https://www.jipipe.org/");
        System.out.println("Developed by Applied Systems Biology, HKI Jena, Germany");
        System.out.println();
        System.out.println("run <options>");
        System.out.println("Runs a project file and writes outputs to the specified directory.");
        System.out.println("--project <Project file>");
        System.out.println("--output-folder <Output folder>");
        System.out.println("--num-threads <N=1,2,...>");
        System.out.println("--overwrite-parameters <JSON file>");
        System.out.println("--P<Node ID>/<Parameter ID> <Parameter Value (JSON)>");
        System.out.println("--save-results-to-disk <on/off/only-compartment-outputs> (default: on)");
        System.out.println("To run this tool, execute following command:");
        System.out.println("<ImageJ executable> --debug --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeCLI");
    }
}
