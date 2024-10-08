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

package org.hkijena.jipipe.plugins.utils.algorithms.meta;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.plugins.utils.datatypes.JIPipeOutputData;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Runs a JIPipe project
 */
@SetJIPipeDocumentation(name = "Run JIPipe project", description = "Runs an existing JIPipe project with given parameter set. Use the 'Define JIPipe project parameters' node to create the necessary parameters. " +
        "Parameters can either point to a project parameter (preferred way) or to a parameter within a specific node (prefix with [node id]/). To find out the parameter ids, take a look at the 'Node ID' documentation on selecting a node.")
@AddJIPipeInputSlot(value = ParametersData.class, name = "Project parameters", create = true)
@AddJIPipeOutputSlot(value = JIPipeOutputData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Meta run")
public class RunJIPipeProjectAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Path projectFile = Paths.get("");
    private OptionalIntegerParameter threads = new OptionalIntegerParameter();
    private boolean ignoreValidation = false;

    public RunJIPipeProjectAlgorithm(JIPipeNodeInfo info) {
        super(info);
        threads.setContent(JIPipeRuntimeApplicationSettings.getInstance().getDefaultRunThreads());
    }

    public RunJIPipeProjectAlgorithm(RunJIPipeProjectAlgorithm other) {
        super(other);
        this.projectFile = other.projectFile;
        this.threads = new OptionalIntegerParameter(other.threads);
        this.ignoreValidation = other.ignoreValidation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeValidationReport report = new JIPipeValidationReport();
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile, new UnspecifiedValidationReportContext(), report, new JIPipeNotificationInbox());
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(
                    e,
                    "Could not load project!",
                    "The node tried to load a JIPipe project from " + projectFile + ", but could not load it.",
                    "Please check if the file exists and is a valid JIPipe project.");
        }
        if (!ignoreValidation) {
            if (!report.isValid()) {
                report.print();
                throw new JIPipeValidationRuntimeException(report);
            }
        }

        // Set parameters
        ParametersData parameters = iterationStep.getInputData(getFirstInputSlot(), ParametersData.class, progressInfo);
        {
            JIPipeProjectInfoParameters pipelineParameters = project.getPipelineParameters();
            JIPipeParameterTree infoParameterTree = new JIPipeParameterTree(pipelineParameters);
            for (Map.Entry<String, Object> entry : parameters.getParameterData().entrySet()) {
                JIPipeParameterAccess access = infoParameterTree.getParameters().getOrDefault(entry.getKey(), null);
                if (access != null) {
                    access.set(entry.getValue());
                } else if (entry.getKey().contains("/")) {
                    String nodeId = entry.getKey().substring(0, entry.getKey().indexOf("/"));
                    String key = entry.getKey().substring(entry.getKey().indexOf("/") + 1);
                    JIPipeGraphNode node = project.getGraph().findNode(nodeId);
                    if (node == null)
                        continue;
                    JIPipeParameterTree nodeTree = new JIPipeParameterTree(node);
                    JIPipeParameterAccess nodeAccess = nodeTree.getParameters().getOrDefault(key, null);
                    if (nodeAccess == null)
                        continue;
                    nodeAccess.set(entry.getValue());
                }
            }
        }

        // Main validation
        if (!ignoreValidation) {
            project.reportValidity(new UnspecifiedValidationReportContext(), report);
            if (!report.isValid()) {
                report.print();
                throw new JIPipeValidationRuntimeException(report);
            }
        }

        // Generate output-folder (must be pre-generated)
        Path rowStoragePath = getFirstOutputSlot().getRowStoragePath(getFirstOutputSlot().getRowCount());

        // Generate the run
        JIPipeGraphRunConfiguration settings = new JIPipeGraphRunConfiguration();
        settings.setOutputPath(rowStoragePath);
        settings.setStoreToCache(false);
        settings.setLoadFromCache(false);
        settings.setNumThreads(threads.isEnabled() ? threads.getContent() : JIPipeRuntimeApplicationSettings.getInstance().getDefaultRunThreads());
        JIPipeGraphRun run = new JIPipeGraphRun(project, settings);
        run.setProgressInfo(progressInfo.resolve("Project " + projectFile.getFileName().toString()));

        run.run();

        // Add output data
        iterationStep.addOutputData(getFirstOutputSlot(), new JIPipeOutputData(rowStoragePath), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @SetJIPipeDocumentation(name = "Project file", description = "The project file")
    @PathParameterSettings(pathMode = PathType.FilesOnly, ioMode = PathIOMode.Open, extensions = {"jip"})
    @JIPipeParameter("project-file")
    public Path getProjectFile() {
        return projectFile;
    }

    @JIPipeParameter("project-file")
    public void setProjectFile(Path projectFile) {
        this.projectFile = projectFile;
    }

    @SetJIPipeDocumentation(name = "Override number of threads", description = "Overrides the number of threads to use. Otherwise the number of threads are taken from the JIPipe settings.")
    @JIPipeParameter("num-threads")
    public OptionalIntegerParameter getThreads() {
        return threads;
    }

    @JIPipeParameter("num-threads")
    public void setThreads(OptionalIntegerParameter threads) {
        this.threads = threads;
    }

    @SetJIPipeDocumentation(name = "Ignore project validation", description = "If disabled, the project is not validated. Otherwise errors are thrown to the current run.")
    @JIPipeParameter("ignore-validation")
    public boolean isIgnoreValidation() {
        return ignoreValidation;
    }

    @JIPipeParameter("ignore-validation")
    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }
}
