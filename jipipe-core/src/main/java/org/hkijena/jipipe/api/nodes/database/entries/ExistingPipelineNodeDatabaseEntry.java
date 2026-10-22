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

package org.hkijena.jipipe.api.nodes.database.entries;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.DefaultJIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.WeightedTokens;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ExistingPipelineNodeDatabaseEntry extends DefaultJIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeGraphNode graphNode;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();

    public ExistingPipelineNodeDatabaseEntry(String id, JIPipeGraphNode graphNode) {
        this.id = id;
        this.graphNode = graphNode;
        initializeSlots();
        initializeTokens();
        // Same node as the existing graph node: same taxonomy facets
        CreateNewNodeByInfoDatabaseEntry.initializeTaxonomyFacets(this, graphNode.getInfo());
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
        tokens.add(nodeInfo.getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
    }

    @Override
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        TaxonomyView.BehaviorFlags flags = TaxonomyView.behaviorFlags(graphNode.getInfo());
        return JIPipeNodeDatabasePipelineVisibility.fromFlags(flags.isVisibleInPipeline(), flags.isVisibleInCompartments());
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
        return ThemeUtils.getNodeFillColor(graphNode.getInfo());
    }

    @Override
    public Color getBorderColor() {
        return ThemeUtils.getNodeBorderColor(graphNode.getInfo());
    }

    @Override
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        return Collections.singleton(canvasUI.getNodeUIs().get(graphNode));
    }

    @Override
    public List<String> getLocationInfos() {
        UUID uuid = graphNode.getCompartmentUUIDInParentGraph();
        if (uuid == null) {
            return Collections.singletonList("Nodes");
        } else {
            return Collections.singletonList("Compartments\n" + graphNode.getCompartmentDisplayName());
        }
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
    public boolean isDeprecatedOrUnstable() {
        return false;
    }
}
