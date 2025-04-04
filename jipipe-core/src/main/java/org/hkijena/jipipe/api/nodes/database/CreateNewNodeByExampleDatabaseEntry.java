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
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
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
    private final JIPipeGraphNode exampleNode;
    private final Set<String> categoryIds = new HashSet<>();
    private final List<String> locationInfos = new ArrayList<>();

    public CreateNewNodeByExampleDatabaseEntry(String id, JIPipeNodeExample example) {
        this.id = id;
        this.example = example;
        this.exampleNode = example.getNodeTemplate().getGraph().getGraphNodes().iterator().next();
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();
        initializeSlots();
        initializeTokens();
        initializeCategoryIds();
        initializeLocationInfos();
    }

    private void initializeLocationInfos() {
        locationInfos.add((example.getNodeInfo().getCategory().getName() + "\n" + example.getNodeInfo().getMenuPath()).trim());
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            locationInfos.add((alias.getCategory().getName() + "\n" + alias.getMenuPath()).trim());
        }
    }

    private void initializeCategoryIds() {
        categoryIds.add(exampleNode.getInfo().getCategory().getId());
        for (JIPipeNodeMenuLocation alias : exampleNode.getInfo().getAliases()) {
            categoryIds.add(alias.getCategory().getId());
        }
    }

    private void initializeSlots() {
        for (JIPipeInputDataSlot inputSlot : exampleNode.getInputSlots()) {
            inputSlots.put(inputSlot.getName(), inputSlot.getInfo());
        }
        for (JIPipeOutputDataSlot outputSlot : exampleNode.getOutputSlots()) {
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
        tokens.add(example.getNodeTemplate().getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
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
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        return JIPipeNodeDatabasePipelineVisibility.fromCategory(example.getNodeInfo().getCategory());
    }

    @Override
    public String getName() {
        return example.getNodeInfo().getName() + ": " + example.getNodeTemplate().getName();
    }

    @Override
    public HTMLText getDescription() {
        if (StringUtils.isNullOrEmpty(example.getNodeTemplate().getDescription().getBody())) {
            return example.getNodeInfo().getDescription();
        } else {
            return example.getNodeTemplate().getDescription();
        }
    }

    @Override
    public Icon getIcon() {
        return example.getNodeInfo().getIcon();
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
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        JIPipeGraphNode copy = example.getNodeTemplate().getGraph().getGraphNodes().iterator().next().duplicate();
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforeAddNode(copy, canvasUI.getCompartmentUUID());
        }
        copy.setCustomName(copy.getInfo().getName() + ": " + example.getNodeTemplate().getName());
        canvasUI.getGraph().insertNode(copy, canvasUI.getCompartmentUUID());
        return Collections.singleton(canvasUI.getNodeUIs().get(copy));
    }

    @Override
    public boolean canAddInputSlots() {
        return exampleNode.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) exampleNode.getSlotConfiguration()).canAddInputSlot();
    }

    @Override
    public boolean canAddOutputSlots() {
        return exampleNode.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) exampleNode.getSlotConfiguration()).canAddOutputSlot();
    }

    @Override
    public boolean isDeprecated() {
        return exampleNode.getInfo().isDeprecated();
    }
}
