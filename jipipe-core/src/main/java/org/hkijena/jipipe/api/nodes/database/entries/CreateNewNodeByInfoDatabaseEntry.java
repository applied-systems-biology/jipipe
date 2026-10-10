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
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.DefaultJIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.WeightedTokens;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
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

public class CreateNewNodeByInfoDatabaseEntry extends DefaultJIPipeNodeDatabaseEntry {

    private final String id;
    private final JIPipeNodeInfo nodeInfo;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private final boolean canAddInputSlots;
    private final boolean canAddOutputSlots;
    private final List<String> locationInfos = new ArrayList<>();

    public CreateNewNodeByInfoDatabaseEntry(String id, JIPipeNodeInfo nodeInfo) {
        this.id = id;
        this.nodeInfo = nodeInfo;
        JIPipeGraphNode node = nodeInfo.newInstance();
        this.canAddInputSlots = node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canAddInputSlot();
        this.canAddOutputSlots = node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration &&
                ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canAddOutputSlot();
        initializeSlots();
        initializeTokens();
        initializeLocationInfos();
        initializeTaxonomyFacets(this, nodeInfo);
    }

    /**
     * Populates the taxonomy facets from the given node info: the
     * function ids (primary first, then secondary, in declaration order),
     * the framework, the spatial dimensions, the derived input profile,
     * and the merged tag set (node-level tags plus the tags of every
     * taxonomy function the node appears under).
     *
     * @param nodeInfo the node info to read the facets from
     */
    static void initializeTaxonomyFacets(DefaultJIPipeNodeDatabaseEntry entry, JIPipeNodeInfo nodeInfo) {
        Set<String> functionIds = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction primary = nodeInfo.getPrimaryFunction();
        if (primary != null) {
            functionIds.add(primary.getId());
        }
        for (JIPipeNodeTaxonomyFunction secondary : nodeInfo.getSecondaryFunctions()) {
            if (secondary != null) {
                functionIds.add(secondary.getId());
            }
        }
        entry.setFunctionIds(functionIds);
        entry.setFramework(nodeInfo.getFramework());
        entry.setSpatialDimensions(nodeInfo.getSpatialDimensions());
        entry.setInputProfile(nodeInfo.getInputProfile());
        entry.setTags(nodeInfo.getMergedTags());
    }

    /**
     * Populates {@link #locationInfos} from the node's taxonomy (Task 26a).
     * <p>
     * The primary function contributes the function chain root→leaf,
     * {@code \n}-joined, no node name — e.g. {@code "Data Sources\nImage data"}.
     * The 21b node-finder + entry-list renderers do {@code .replace("\n", " > ")},
     * and the search/embedding consume the raw form. Secondary functions each
     * contribute their own chain (they replace the removed alias location
     * entries). A node with no primary function contributes nothing — it
     * should not exist post-conversion; do NOT read the legacy category.
     */
    private void initializeLocationInfos() {
        JIPipeNodeTaxonomyFunction primary = nodeInfo.getPrimaryFunction();
        if (primary != null) {
            locationInfos.add(functionChainLocation(primary));
        }
        for (JIPipeNodeTaxonomyFunction secondary : nodeInfo.getSecondaryFunctions()) {
            if (secondary != null) {
                locationInfos.add(functionChainLocation(secondary));
            }
        }
    }

    /**
     * The {@code \n}-joined function chain (root→leaf) for the given leaf
     * function, e.g. {@code "Data Sources\nImage data"}. Guards against a
     * cyclic parent chain so it always terminates.
     *
     * @param leaf the leaf function to start the chain from
     * @return the root→leaf chain joined by {@code \n}; never null
     */
    static String functionChainLocation(JIPipeNodeTaxonomyFunction leaf) {
        List<String> names = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction current = leaf;
        while (current != null && visited.add(current.getId())) {
            names.add(current.getName());
            current = current.getParent();
        }
        // names is leaf→root; emit root→leaf.
        StringBuilder sb = new StringBuilder();
        for (int i = names.size() - 1; i >= 0; i--) {
            if (i < names.size() - 1) {
                sb.append("\n");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
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
        tokens.add(nodeInfo.getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
        // Task 18: feed the new taxonomy into the search tokens ADDITIVELY.
        // For unconverted nodes (no function, NoFramework, no tags) this
        // adds nothing, so the legacy token set is unchanged.
        addTaxonomyTokens(tokens, nodeInfo);
    }

    /**
     * Adds the taxonomy search tokens of the given node info:
     * <ul>
     * <li><b>Function path:</b> the primary function's name at
     *     {@link WeightedTokens#WEIGHT_FUNCTION_LEAF}, then its
     *     {@code getParent()} chain walked up, each ancestor at
     *     {@link WeightedTokens#WEIGHT_FUNCTION_LEAF} − 2 × depth, floored
     *     at {@link WeightedTokens#WEIGHT_FUNCTION_TOP}. Secondary
     *     functions (alternate locations the node is listed under) add
     *     their own name at the leaf weight only — their ancestor chains
     *     are deliberately NOT walked, because the shared ancestors
     *     (e.g. the top-level branch) would otherwise be added again on
     *     top of the primary path's contribution;</li>
     * <li><b>Framework:</b> the display name at
     *     {@link WeightedTokens#WEIGHT_FRAMEWORK} and the abbreviation at
     *     {@link WeightedTokens#WEIGHT_FRAMEWORK_ABBREV} — skipped for
     *     {@link NoFramework}, whose "—"/"—" values would be search noise
     *     on every unconverted node;</li>
     * <li><b>Tags:</b> each merged tag at {@link WeightedTokens#WEIGHT_TAG}.</li>
     * </ul>
     * The spatial dimensions and the input profile are filter-only facets
     * (see the spec) and are NOT added as tokens.
     * <p>
     * This is additive: the existing name/alias/menu/description tokens
     * must already have been added by the caller.
     *
     * @param tokens   the tokens to add to
     * @param nodeInfo the node info to read the taxonomy from
     */
    static void addTaxonomyTokens(WeightedTokens tokens, JIPipeNodeInfo nodeInfo) {
        // Function path: primary leaf + ancestors (gradient), secondary leaves only
        JIPipeNodeTaxonomyFunction primary = nodeInfo.getPrimaryFunction();
        if (primary != null) {
            addFunctionPathTokens(tokens, primary);
            for (JIPipeNodeTaxonomyFunction secondary : nodeInfo.getSecondaryFunctions()) {
                if (secondary != null && !secondary.getId().equals(primary.getId())) {
                    // Alternate location: leaf weight only, no ancestor walk
                    // (avoids double-adding ancestors shared with the primary path)
                    tokens.add(secondary.getName(), WeightedTokens.WEIGHT_FUNCTION_LEAF);
                }
            }
        }
        // Framework: skip the NoFramework sentinel ("—" would be search noise)
        JIPipeNodeTaxonomyFramework framework = nodeInfo.getFramework();
        if (framework != null && !NoFramework.ID.equals(framework.getId())) {
            tokens.add(framework.getName(), WeightedTokens.WEIGHT_FRAMEWORK);
            tokens.add(framework.getAbbreviation(), WeightedTokens.WEIGHT_FRAMEWORK_ABBREV);
        }
        // Tags: the merged set (node-level tags plus function-path tags)
        for (String tag : nodeInfo.getMergedTags()) {
            tokens.add(tag, WeightedTokens.WEIGHT_TAG);
        }
    }

    /**
     * Adds the name of the given function at the leaf weight, then walks
     * its {@code getParent()} chain up, each ancestor two weight levels
     * below the previous, floored at the top weight.
     */
    private static void addFunctionPathTokens(WeightedTokens tokens, JIPipeNodeTaxonomyFunction function) {
        JIPipeNodeTaxonomyFunction current = function;
        int weight = WeightedTokens.WEIGHT_FUNCTION_LEAF;
        while (current != null) {
            tokens.add(current.getName(), weight);
            weight = Math.max(WeightedTokens.WEIGHT_FUNCTION_TOP, weight - 2);
            current = current.getParent();
        }
    }

    @Override
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        TaxonomyView.BehaviorFlags flags = TaxonomyView.behaviorFlags(nodeInfo);
        return JIPipeNodeDatabasePipelineVisibility.fromFlags(flags.isVisibleInPipeline(), flags.isVisibleInCompartments());
    }

    @Override
    public String getName() {
        String result = nodeInfo.getName();
        if (nodeInfo.isUnstable()) {
            result += " (unstable!)";
        } else if (nodeInfo.isDeprecated()) {
            result += " (deprecated)";
        }
        return result;
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
    public Map<String, JIPipeDataSlotInfo> getInputSlots() {
        return inputSlots;
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public Color getFillColor() {
        return ThemeUtils.getNodeFillColor(nodeInfo);
    }

    @Override
    public Color getBorderColor() {
        return ThemeUtils.getNodeBorderColor(nodeInfo);
    }

    @Override
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        JIPipeGraphNode node = nodeInfo.newInstance();
        if (!node.uiBeforeAddToCanvas(canvasUI)) {
            return Collections.emptySet();
        }
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforeAddNode(node, canvasUI.getCompartmentUUID());
        }
        canvasUI.getGraph().insertNode(node, canvasUI.getCompartmentUUID());
        node.uiAfterAddToCanvas(canvasUI);
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
    public boolean isDeprecatedOrUnstable() {
        return nodeInfo.isDeprecated();
    }

    @Override
    public JIPipeNodeClassification getNodeClassification() {
        return nodeInfo.getNodeClassification();
    }

    @Override
    public String getOriginNodeId() {
        return nodeInfo.getId();
    }
}
