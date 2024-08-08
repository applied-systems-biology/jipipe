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
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ExistingPipelineNodeDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeGraphNode graphNode;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private final String descriptionPlain;
    private final Set<String> categoryIds = new HashSet<>();

    public ExistingPipelineNodeDatabaseEntry(String id, JIPipeGraphNode graphNode) {
        this.id = id;
        this.graphNode = graphNode;
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();
        initializeSlots();
        initializeTokens();
        initializeCategoryIds();
    }

    private void initializeCategoryIds() {
        categoryIds.add(graphNode.getCategory().getId());
        for (JIPipeNodeMenuLocation alias : graphNode.getInfo().getAliases()) {
            categoryIds.add(alias.getCategory().getId());
        }
    }

    private void initializeSlots() {
        for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
            inputSlots.put(inputSlot.getName(), inputSlot.getInfo());
        }
        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
            outputSlots.put(outputSlot.getName(), outputSlot.getInfo());
        }
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
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        return JIPipeNodeDatabasePipelineVisibility.fromCategory(graphNode.getCategory());
    }

    @Override
    public String getName() {
        return graphNode.getName();
    }

    @Override
    public HTMLText getDescription() {
        if (!StringUtils.isNullOrEmpty(graphNode.getCustomDescription().getBody()))
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
    public JIPipeDesktopGraphNodeUI addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        return canvasUI.getNodeUIs().get(graphNode);
    }

    @Override
    public String getLocationInfo() {
        UUID uuid = graphNode.getCompartmentUUIDInParentGraph();
        if (uuid == null) {
            return "Nodes";
        } else {
            return "Compartments\n" + graphNode.getCompartmentDisplayName();
        }
    }

    @Override
    public Set<String> getCategoryIds() {
        return categoryIds;
    }

    @Override
    public String getDescriptionPlain() {
        return descriptionPlain;
    }

    @Override
    public boolean canAddInputSlots() {
        return graphNode.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) graphNode.getSlotConfiguration()).canAddInputSlot();
    }

    @Override
    public boolean canAddOutputSlots() {
        return graphNode.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) graphNode.getSlotConfiguration()).canAddOutputSlot();
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }
}
