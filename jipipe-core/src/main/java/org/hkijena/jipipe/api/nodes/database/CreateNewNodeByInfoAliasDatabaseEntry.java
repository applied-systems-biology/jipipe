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

package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CreateNewNodeByInfoAliasDatabaseEntry implements JIPipeNodeDatabaseEntry {

    private final String id;
    private final JIPipeNodeInfo nodeInfo;
    private final JIPipeNodeMenuLocation alias;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private final boolean canAddInputSlots;
    private final boolean canAddOutputSlots;
    private final Set<String> categoryIds = new HashSet<>();
    private final List<String> locationInfos = new ArrayList<>();

    public CreateNewNodeByInfoAliasDatabaseEntry(String id, JIPipeNodeInfo nodeInfo, JIPipeNodeMenuLocation alias) {
        this.id = id;
        this.nodeInfo = nodeInfo;
        this.alias = alias;
        JIPipeGraphNode node = nodeInfo.newInstance();
        this.canAddInputSlots = node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canAddInputSlot();
        this.canAddOutputSlots = node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canAddOutputSlot();
        initializeSlots();
        initializeTokens();
        initializeCategoryIds();
        initializeLocationInfos();
    }

    private void initializeLocationInfos() {
        locationInfos.add((alias.getCategory().getName() + "\n" + alias.getMenuPath()).trim());
    }

    private void initializeCategoryIds() {
        categoryIds.add(alias.getCategory().getId());
    }

    private void initializeSlots() {
        for (AddJIPipeInputSlot inputSlot : nodeInfo.getInputSlots()) {
            if (!StringUtils.isNullOrEmpty(inputSlot.name())) {
                inputSlots.put(inputSlot.name(), new JIPipeDataSlotInfo(inputSlot.value(),
                        JIPipeSlotType.Input,
                        inputSlot.name(),
                        inputSlot.description()));
            }
        }
        for (AddJIPipeOutputSlot outputSlot : nodeInfo.getOutputSlots()) {
            if (!StringUtils.isNullOrEmpty(outputSlot.name())) {
                outputSlots.put(outputSlot.name(), new JIPipeDataSlotInfo(outputSlot.value(),
                        JIPipeSlotType.Output,
                        outputSlot.name(),
                        outputSlot.description()));
            }
        }
    }

    private void initializeTokens() {
        tokens.add(nodeInfo.getName(), WeightedTokens.WEIGHT_NAME);
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getAlternativeName(), WeightedTokens.WEIGHT_NAME);
        }
        tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath(), WeightedTokens.WEIGHT_MENU);
        tokens.add(nodeInfo.getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
    }

    @Override
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        return JIPipeNodeDatabasePipelineVisibility.fromCategory(nodeInfo.getCategory());
    }

    @Override
    public String getName() {
        return StringUtils.orElse(alias.getAlternativeName(), nodeInfo.getName());
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
    public List<String> getLocationInfos() {
        return locationInfos;
    }

    @Override
    public Set<String> getCategoryIds() {
        return categoryIds;
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
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        JIPipeGraphNode node = nodeInfo.newInstance();
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforeAddNode(node, canvasUI.getCompartmentUUID());
        }
        canvasUI.getGraph().insertNode(node, canvasUI.getCompartmentUUID());
        return Collections.singleton(canvasUI.getNodeUIs().get(node));
    }

    @Override
    public boolean canAddInputSlots() {
        return canAddInputSlots;
    }

    @Override
    public boolean canAddOutputSlots() {
        return canAddOutputSlots;
    }

    @Override
    public boolean isDeprecated() {
        return nodeInfo.isDeprecated();
    }
}
