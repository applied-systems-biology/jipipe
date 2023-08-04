package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CreateNewNodeByExampleDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeNodeExample example;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private final String descriptionPlain;

    public CreateNewNodeByExampleDatabaseEntry(String id, JIPipeNodeExample example) {
        this.id = id;
        this.example = example;
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();
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
        tokens.add(example.getNodeTemplate().getName(), WeightedTokens.WEIGHT_NAME);
        tokens.add(example.getNodeInfo().getName(), WeightedTokens.WEIGHT_NAME);
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            tokens.add(alias.getAlternativeName(), WeightedTokens.WEIGHT_NAME);
        }
        tokens.add(example.getNodeInfo().getCategory().getName() + "\n" + example.getNodeInfo().getMenuPath(), WeightedTokens.WEIGHT_MENU);
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath(), WeightedTokens.WEIGHT_MENU);
        }
        tokens.add(example.getNodeTemplate().getDescription().getBody(),WeightedTokens.WEIGHT_DESCRIPTION);
        tokens.add(example.getNodeInfo().getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
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
        return example.getNodeInfo().getName() + ": " + example.getNodeTemplate().getName();
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
    public WeightedTokens getTokens() {
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

    @Override
    public Color getFillColor() {
        return example.getNodeInfo().getCategory().getFillColor();
    }

    @Override
    public Color getBorderColor() {
        return example.getNodeInfo().getCategory().getBorderColor();
    }

    @Override
    public void addToGraph(JIPipeGraphCanvasUI canvasUI) {
        JIPipeGraph graph = new JIPipeGraph(example.getNodeTemplate().getGraph());
        for (UUID uuid : graph.getGraphNodeUUIDs()) {
            graph.setCompartment(uuid, canvasUI.getCompartment());
        }

        canvasUI.getGraph().mergeWith(graph);
    }

    @Override
    public String getDescriptionPlain() {
        return descriptionPlain;
    }
}
