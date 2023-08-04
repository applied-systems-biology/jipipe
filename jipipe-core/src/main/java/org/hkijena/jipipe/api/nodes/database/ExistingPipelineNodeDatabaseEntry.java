package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ExistingPipelineNodeDatabaseEntry implements JIPipeNodeDatabaseEntry{
    private final String id;
    private final JIPipeGraphNode graphNode;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private final String descriptionPlain;

    public ExistingPipelineNodeDatabaseEntry(String id, JIPipeGraphNode graphNode) {
        this.id = id;
        this.graphNode = graphNode;
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();
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
        tokens.add(graphNode.getName(), WeightedTokens.WEIGHT_NAME);
        tokens.add(graphNode.getCustomDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
        tokens.add(nodeInfo.getName(), WeightedTokens.WEIGHT_MENU);
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getAlternativeName(), WeightedTokens.WEIGHT_MENU);
        }
        tokens.add(nodeInfo.getCategory().getName() + "\n" + nodeInfo.getMenuPath(), WeightedTokens.WEIGHT_MENU);
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath(), WeightedTokens.WEIGHT_MENU);
        }
        tokens.add(nodeInfo.getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
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
    public WeightedTokens getTokens() {
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
    public Color getFillColor() {
        return graphNode.getInfo().getCategory().getFillColor();
    }

    @Override
    public Color getBorderColor() {
        return graphNode.getInfo().getCategory().getBorderColor();
    }

    @Override
    public void addToGraph(JIPipeGraphCanvasUI canvasUI) {

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

    @Override
    public String getDescriptionPlain() {
        return descriptionPlain;
    }
}
