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

package org.hkijena.jipipe.api.grouping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEventListener;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.grouping.parameters.NodeGroupContents;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.run.JIPipeGraphRunPartitionInheritedBoolean;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.desktop.app.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A sub-graph algorithm that can be defined by a user
 */
@SetJIPipeDocumentation(name = "Group", description = "A node that contains a sub-pipeline. Double-click the node to open the contained workflow and add inputs/outputs to the 'Group input'/'Group output' nodes. " +
        "To expose parameters from within the group to the surrounding group node, go to the 'Parameters' tab and add references via 'Edit parameter references'.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class JIPipeNodeGroup extends JIPipeGraphWrapperAlgorithm implements JIPipeCustomParameterCollection, ParameterReferencesChangedEventListener {

    private NodeGroupContents contents;
    private GraphNodeParameterReferenceGroupCollection exportedParameters = new GraphNodeParameterReferenceGroupCollection();
    private boolean showLimitedParameters = false;

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public JIPipeNodeGroup(JIPipeNodeInfo info) {
        super(info, new JIPipeGraph());
        initializeContents();
        exportedParameters.getParameterReferencesChangedEventEmitter().subscribe(this);
        registerSubParameter(exportedParameters);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeNodeGroup(JIPipeNodeGroup other) {
        super(other);
        this.exportedParameters = new GraphNodeParameterReferenceGroupCollection(other.exportedParameters);
        exportedParameters.getParameterReferencesChangedEventEmitter().subscribe(this);
        this.showLimitedParameters = other.showLimitedParameters;
        registerSubParameter(exportedParameters);
        initializeContents();
    }

    /**
     * Initializes from an existing graph
     *
     * @param graph           algorithms to be added
     * @param autoCreateSlots automatically create input and output slots
     * @param clearLocations  if enabled, clear the locations. if false, fixLocations will be queried
     * @param fixLocations    if enabled, move group inputs/outputs to locations where they fit better
     */
    public JIPipeNodeGroup(JIPipeGraph graph, boolean autoCreateSlots, boolean clearLocations, boolean fixLocations) {
        super(JIPipe.getNodes().getInfoById("node-group"), new JIPipeGraph());

        // Remove all algorithms with no i/o
//        for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getAlgorithmNodes().values())) {
//            if (node.getInputSlots().isEmpty() && node.getOutputSlots().isEmpty()) {
//                graph.removeNode(node, false);
//            }
//        }

        // Clear locations
        if (clearLocations) {
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                node.clearLocations();
            }
        }

        // Replace all JIPipeCompartmentOutput by IOInterfaceAlgorithm
        for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (node instanceof JIPipeProjectCompartmentOutput) {
                IOInterfaceAlgorithm.replaceCompartmentOutput((JIPipeProjectCompartmentOutput) node);
            }
        }

        // Second copy
        setWrappedGraph(graph);
        initializeContents();

        if (autoCreateSlots) {
            this.autoCreateSlots();
        }

        registerSubParameter(exportedParameters);

        if (!clearLocations && fixLocations) {
            // Assign locations of input and output accordingly
            for (JIPipeGraphViewMode viewMode : JIPipeGraphViewMode.values()) {
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
                    Map<String, Point> locations = graphNode.getNodeUILocationPerViewModePerCompartment().getOrDefault("", null);
                    if (locations != null) {
                        Point point = locations.getOrDefault(viewMode.name(), null);
                        if (point != null) {
                            minX = Math.min(minX, point.x);
                            minY = Math.min(minY, point.y);
                            maxX = Math.max(maxX, point.x);
                            maxY = Math.max(maxY, point.y);
                        }
                    }
                }

                // Set group input/output
                if (minX != Integer.MAX_VALUE && minY != Integer.MAX_VALUE && maxX != Integer.MIN_VALUE && maxY != Integer.MIN_VALUE) {
                    getGroupInput().setNodeUILocationWithin("", new Point(minX, minY - 5), viewMode.name());
                    getGroupOutput().setNodeUILocationWithin("", new Point(maxX, maxY + 5), viewMode.name());
                }
            }
        }
    }

    /**
     * Automatically creates slots that connect all inputs
     *
     * @return Map from a slot in the wrapped graph to the exported group slot
     */
    public BiMap<JIPipeDataSlot, JIPipeDataSlot> autoCreateSlots() {
        setPreventUpdateSlots(true);
        JIPipeMutableSlotConfiguration inputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        JIPipeMutableSlotConfiguration outputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
        BiMap<JIPipeDataSlot, JIPipeDataSlot> exportedSlots = HashBiMap.create();
        BiMap<JIPipeDataSlot, String> exportedInputSlotNames = HashBiMap.create();
        BiMap<JIPipeDataSlot, String> exportedOutputSlotNames = HashBiMap.create();
        for (JIPipeDataSlot slot : getWrappedGraph().getUnconnectedSlots()) {
            if (!slot.getNode().getInfo().isRunnable())
                continue;
            if (slot.isInput()) {
                String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", exportedInputSlotNames::containsValue);
                inputSlotConfiguration.addSlot(uniqueName, slot.getInfo(), false);
                exportedInputSlotNames.put(slot, uniqueName);
            } else if (slot.isOutput()) {
                String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", exportedOutputSlotNames::containsValue);
                outputSlotConfiguration.addSlot(uniqueName, slot.getInfo(), false);
                exportedOutputSlotNames.put(slot, uniqueName);
            }
        }
        setPreventUpdateSlots(false);
        updateGroupSlots();

        // Store the assigned slots
        for (Map.Entry<JIPipeDataSlot, String> entry : exportedInputSlotNames.entrySet()) {
            exportedSlots.put(entry.getKey(), getInputSlot(entry.getValue()));
        }
        for (Map.Entry<JIPipeDataSlot, String> entry : exportedOutputSlotNames.entrySet()) {
            exportedSlots.put(entry.getKey(), getOutputSlot(entry.getValue()));
        }

        // Internal input slot -> Connect to group output
        for (Map.Entry<JIPipeDataSlot, String> entry : exportedInputSlotNames.entrySet()) {
            JIPipeDataSlot internalSlot = entry.getKey();
            String exportedName = entry.getValue();
            JIPipeDataSlot source = getGroupInput().getOutputSlot(exportedName);
            getWrappedGraph().connect(source, internalSlot);
        }
        // Internal output slot -> Connect to group input
        for (Map.Entry<JIPipeDataSlot, String> entry : exportedOutputSlotNames.entrySet()) {
            JIPipeDataSlot internalSlot = entry.getKey();
            String exportedName = entry.getValue();
            JIPipeDataSlot target = getGroupOutput().getInputSlot(exportedName);
            getWrappedGraph().connect(internalSlot, target);
        }

        return exportedSlots;
    }

    private void initializeContents() {
        contents = new NodeGroupContents();
        contents.setWrappedGraph(getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
        contents.setParent(this);
    }

    @SetJIPipeDocumentation(name = "Continue on failure", description = "If enabled, the pipeline will continue if a node within the group fails. For pass-through iteration, " +
            "JIPipe will continue with the pipeline (no data is output from the group). If you enabled iteration, all successful results will be stored and passed as output.")
    @JIPipeParameter(value = "continue-on-failure", important = true)
    @Override
    public JIPipeGraphRunPartitionInheritedBoolean isContinueOnFailure() {
        return super.isContinueOnFailure();
    }

    @JIPipeParameter("continue-on-failure")
    @Override
    public void setContinueOnFailure(JIPipeGraphRunPartitionInheritedBoolean continueOnFailure) {
        super.setContinueOnFailure(continueOnFailure);
    }

    @SetJIPipeDocumentation(name = "Save inputs of failed sub-pipeline", description = "If enabled, the inputs of saved sub-pipelines will be saved to allow diagnostics. " +
            "Increases memory consumption.")
    @JIPipeParameter("continue-on-failure-backup-mode")
    @Override
    public JIPipeGraphRunPartitionInheritedBoolean getContinueOnFailureExportFailedInputs() {
        return super.getContinueOnFailureExportFailedInputs();
    }

    @JIPipeParameter("continue-on-failure-backup-mode")
    @Override
    public void setContinueOnFailureExportFailedInputs(JIPipeGraphRunPartitionInheritedBoolean continueOnFailureExportFailedInputs) {
        super.setContinueOnFailureExportFailedInputs(continueOnFailureExportFailedInputs);
    }

    @SetJIPipeDocumentation(name = "Wrapped graph", description = "The graph that is wrapped inside this node")
    @JIPipeParameter("contents")
    public NodeGroupContents getContents() {
        return contents;
    }

    @JIPipeParameter("contents")
    public void setContents(NodeGroupContents contents) {
        this.contents = contents;
        contents.setParent(this);
        setWrappedGraph(contents.getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
    }

    @SetJIPipeDocumentation(name = "Exported parameters", description = "Allows you to export parameters from the group into the group node")
    @JIPipeParameter(value = "exported-parameters", functional = false)
    public GraphNodeParameterReferenceGroupCollection getExportedParameters() {
        return exportedParameters;
    }

    @JIPipeParameter("exported-parameters")
    public void setExportedParameters(GraphNodeParameterReferenceGroupCollection exportedParameters) {
        this.exportedParameters = exportedParameters;
        this.exportedParameters.setGraph(getWrappedGraph());
        exportedParameters.getParameterReferencesChangedEventEmitter().subscribe(this);
        registerSubParameter(exportedParameters);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Exported parameters", "exported-parameters"), exportedParameters);

        // Only check if the graph creates a valid group output
        report.report(reportContext, getGroupOutput());
    }

    @SetJIPipeDocumentation(name = "Show limited parameter set", description = "If enabled, only the exported parameters, name, and description are shown as parameters. " +
            "The iteration step generation will also be hidden. This can be useful for educational pipelines.")
    @JIPipeParameter(value = "show-limited-parameters", functional = false)
    public boolean isShowLimitedParameters() {
        return showLimitedParameters;
    }

    @JIPipeParameter("show-limited-parameters")
    public void setShowLimitedParameters(boolean showLimitedParameters) {
        this.showLimitedParameters = showLimitedParameters;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (showLimitedParameters) {
            if (access.getSource() == this) {
                String key = access.getKey();
                if ("show-limited-parameters".equals(key) || "jipipe:node:name".equals(key) || "jipipe:node:description".equals(key)) {
                    return true;
                } else {
                    return false;
                }
            }
            if (access.getSource() == getBatchGenerationSettings())
                return false;
        }
        return super.isParameterUIVisible(tree, access);
    }


    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
//        JIPipeParameterTree standardParameters = new JIPipeParameterTree(this,
//                JIPipeParameterTree.IGNORE_CUSTOM | JIPipeParameterTree.FORCE_REFLECTION);
//        HashMap<String, JIPipeParameterAccess> map = new HashMap<>(standardParameters.getParameters());
//        map.values().removeIf(access -> access.getSource() != this);
//        return map;
        return Collections.emptyMap();
    }

    @Override
    public boolean getIncludeReflectionParameters() {
        return true;
    }

    @Override
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        this.exportedParameters.setGraph(getWrappedGraph());
        Map<String, JIPipeParameterCollection> result = new HashMap<>();
//        result.put("jipipe:data-batch-generation", getBatchGenerationSettings());
        result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getWrappedGraph().getParameterTree(false, null), false));
        return result;
    }

    @Override
    public void onParameterReferencesChanged(ParameterReferencesChangedEvent event) {
        emitParameterStructureChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Input management", description = "Only used if the graph iteration mode is not set to 'Pass data through'. " +
            "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true)
    public JIPipeMergingAlgorithmIterationStepGenerationSettings getBatchGenerationSettings() {
        return super.getBatchGenerationSettings();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (ParameterUtils.isHiddenLocalParameterCollection(tree, subParameter, "jipipe:data-batch-generation", "jipipe:adaptive-parameters")) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @SetJIPipeDocumentation(name = "Graph iteration mode", description = "Determines how the wrapped graph is iterated:" +
            "<ul>" +
            "<li>The data can be passed through. This means that the wrapped graph receives all data as-is and will be executed once.</li>" +
            "<li>The wrapped graph can be executed per iteration step. Here you can choose between an iterative iteration step (one item per slot) " +
            "or a merging iteration step (multiple items per slot).</li>" +
            "</ul>")
    @JIPipeParameter(value = "iteration-mode", functional = false)
    public IterationMode getIterationMode() {
        return super.getIterationMode();
    }

    @JIPipeParameter("iteration-mode")
    public void setIterationMode(IterationMode iterationMode) {
        super.setIterationMode(iterationMode);
    }
}
