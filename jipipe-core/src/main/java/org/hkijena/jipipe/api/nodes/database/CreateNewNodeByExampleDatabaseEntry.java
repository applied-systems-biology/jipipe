package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewNodeByExampleDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeNodeExample example;
    private final List<String> tokens = new ArrayList<>();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();

    public CreateNewNodeByExampleDatabaseEntry(String id, JIPipeNodeExample example) {
        this.id = id;
        this.example = example;
        initializeSlots();
        initializeTokens();
    }

    private void initializeSlots() {
        JIPipeGraphNode node = example.getNodeTemplate().getGraph().getGraphNodes().iterator().next();
        for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
            inputSlots.put(inputSlot.getName(), inputSlot.getInfo());
        }
        for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
            outputSlots.put(outputSlot.getName(), outputSlot.getInfo());
        }
    }

    private void initializeTokens() {
        tokens.add(example.getNodeTemplate().getName());
        tokens.add(example.getNodeInfo().getName());
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            tokens.add(alias.getAlternativeName());
        }
        tokens.add(example.getNodeInfo().getCategory().getName() + "\n" + example.getNodeInfo().getMenuPath());
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath());
        }
        tokens.add(example.getNodeTemplate().getDescription().getBody());
        tokens.add(example.getNodeInfo().getDescription().getBody());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public JIPipeNodeDatabaseRole getRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }

    @Override
    public String getName() {
        return example.getNodeTemplate().getName();
    }

    @Override
    public HTMLText getDescription() {
        if(StringUtils.isNullOrEmpty(example.getNodeTemplate().getDescription().getBody())) {
            return example.getNodeInfo().getDescription();
        }
        else {
            return example.getNodeTemplate().getDescription();
        }
    }

    @Override
    public Icon getIcon() {
        return example.getNodeInfo().getIcon();
    }

    @Override
    public String getCategory() {
        return example.getNodeInfo().getCategory().getName() + "\n" + example.getNodeInfo().getMenuPath();
    }

    @Override
    public List<String> getTokens() {
        return tokens;
    }

    public JIPipeNodeExample getExample() {
        return example;
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getInputSlots() {
        return inputSlots;
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
        return outputSlots;
    }
}
