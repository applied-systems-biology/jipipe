package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewNodeByInfoDatabaseEntry implements JIPipeNodeDatabaseEntry {

    private final String id;
    private final JIPipeNodeInfo nodeInfo;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private final String descriptionPlain;

    public CreateNewNodeByInfoDatabaseEntry(String id, JIPipeNodeInfo nodeInfo) {
        this.id = id;
        this.nodeInfo = nodeInfo;
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();
        initializeSlots();
        initializeTokens();
    }

    private void initializeSlots() {
        for (JIPipeInputSlot inputSlot : nodeInfo.getInputSlots()) {
            if(!StringUtils.isNullOrEmpty(inputSlot.slotName())) {
                inputSlots.put(inputSlot.slotName(), new JIPipeDataSlotInfo(inputSlot.value(),
                        JIPipeSlotType.Input,
                        inputSlot.slotName(),
                        inputSlot.description()));
            }
        }
        for (JIPipeOutputSlot outputSlot : nodeInfo.getOutputSlots()) {
            if(!StringUtils.isNullOrEmpty(outputSlot.slotName())) {
                outputSlots.put(outputSlot.slotName(), new JIPipeDataSlotInfo(outputSlot.value(),
                        JIPipeSlotType.Output,
                        outputSlot.slotName(),
                        outputSlot.description()));
            }
        }
    }

    private void initializeTokens() {
        tokens.add(nodeInfo.getName(), WeightedTokens.WEIGHT_NAME);
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getAlternativeName(), WeightedTokens.WEIGHT_NAME);
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
        return nodeInfo.getName();
    }

    @Override
    public HTMLText getDescription() {
        return nodeInfo.getDescription();
    }

    @Override
    public Icon getIcon() {
        return nodeInfo.getIcon();
    }

    @Override
    public WeightedTokens getTokens() {
        return tokens;
    }

    @Override
    public boolean exists() {
        return false;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    @Override
    public String getId() {
        return id;
    }
    @Override
    public String getCategory() {
        return nodeInfo.getCategory().getName() + "\n" +nodeInfo.getMenuPath();
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
        return nodeInfo.getCategory().getFillColor();
    }

    @Override
    public Color getBorderColor() {
        return nodeInfo.getCategory().getBorderColor();
    }

    @Override
    public void addToGraph(JIPipeGraphCanvasUI canvasUI) {
        canvasUI.getGraph().insertNode(nodeInfo.newInstance(), canvasUI.getCompartment());
    }

    @Override
    public String getDescriptionPlain() {
        return descriptionPlain;
    }
}
