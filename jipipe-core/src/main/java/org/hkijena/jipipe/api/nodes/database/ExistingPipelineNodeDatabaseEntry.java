package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.*;

public class ExistingPipelineNodeDatabaseEntry implements JIPipeNodeDatabaseEntry{
    private final String id;
    private final JIPipeGraphNode graphNode;
    private final List<String> tokens = new ArrayList<>();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();

    public ExistingPipelineNodeDatabaseEntry(String id, JIPipeGraphNode graphNode) {
        this.id = id;
        this.graphNode = graphNode;
        initializeSlots();
        initializeTokens();
    }

    private void initializeSlots() {
        inputSlots.put("Input", new JIPipeDataSlotInfo(JIPipeCompartmentOutputData.class,
                JIPipeSlotType.Input,
                "Input",
                null));
        outputSlots.put("Output", new JIPipeDataSlotInfo(JIPipeCompartmentOutputData.class,
                JIPipeSlotType.Output,
                "Output",
                null));
    }

    private void initializeTokens() {
        JIPipeNodeInfo nodeInfo = graphNode.getInfo();
        tokens.add(graphNode.getName());
        tokens.add(graphNode.getCustomDescription().getBody());
        tokens.add(nodeInfo.getName());
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getAlternativeName());
        }
        tokens.add(nodeInfo.getCategory().getName() + "\n" + nodeInfo.getMenuPath());
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath());
        }
        tokens.add(nodeInfo.getDescription().getBody());
    }

    @Override
    public JIPipeNodeDatabaseRole getRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }

    @Override
    public String getName() {
        return graphNode.getName();
    }

    @Override
    public HTMLText getDescription() {
        if(!StringUtils.isNullOrEmpty(graphNode.getCustomDescription().getBody()))
            return graphNode.getCustomDescription();
        else
            return graphNode.getInfo().getDescription();
    }

    @Override
    public Icon getIcon() {
        return graphNode.getInfo().getIcon();
    }

    @Override
    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public String getId() {
        return id;
    }

    public JIPipeGraphNode getGraphNode() {
        return graphNode;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getInputSlots() {
        return inputSlots;
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public String getCategory() {
        UUID uuid = graphNode.getCompartmentUUIDInParentGraph();
        if(uuid == null) {
            return "Nodes";
        }
        else {
            return "Compartments\n" + graphNode.getCompartmentDisplayName();
        }
    }
}
