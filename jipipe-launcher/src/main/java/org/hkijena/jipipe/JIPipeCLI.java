package org.hkijena.jipipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import net.imagej.ImageJ;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
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
import java.util.Objects;

public class JIPipeCLI {
    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        List<String> argsList = Arrays.asList(args);
        int firstArgIndex = 0;

        if (argsList.contains("org.hkijena.jipipe.JIPipeCLI")) {
            // Started with ImageJ
            while (firstArgIndex < args.length && args[firstArgIndex].startsWith("-")) {
                firstArgIndex += 2;
            }
        }

        if (firstArgIndex >= args.length) {
            showHelp();
            return;
        }
        if (args[firstArgIndex].contains("help")) {
            showHelp();
            return;
        }
        if (Objects.equals(args[firstArgIndex], "run")) {
            Path projectFile = null;
            Path outputFolder = null;
            int numThreads = 1;
            Map<String, String> parameterOverrides = new HashMap<>();
            for (int i = firstArgIndex + 1; i < args.length; i += 2) {
                String arg = args[i];
                String value = args[i + 1];
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
                } else if (arg.startsWith("--P")) {
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
            JIPipeProject project;
            try {
                project = JIPipeProject.loadProject(projectFile, projectIssues);
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

            JIPipeRunSettings settings = new JIPipeRunSettings();
            settings.setNumThreads(numThreads);
            settings.setOutputPath(outputFolder);
            settings.setSaveToDisk(true);
            settings.setStoreToCache(false);

            JIPipeRun run = new JIPipeRun(project, settings);
            run.getProgressInfo().setLogToStdOut(true);
            run.run();
        } else {
            showHelp();
        }
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
        System.out.println("To run this tool, execute following command:");
        System.out.println("<ImageJ executable> --debug --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeCLI");
    }
}
