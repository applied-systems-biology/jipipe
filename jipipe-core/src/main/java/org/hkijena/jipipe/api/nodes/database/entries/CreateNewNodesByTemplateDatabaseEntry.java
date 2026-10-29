package org.hkijena.jipipe.api.nodes.database.entries;

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.DefaultJIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.WeightedTokens;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TemplatesFunction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CreateNewNodesByTemplateDatabaseEntry extends DefaultJIPipeNodeDatabaseEntry {

    private static final String BADGE_PATTERN = "Pattern";
    private static final String BADGE_CUSTOM = "Custom";

    private final JIPipeNodeTemplate template;
    private final JIPipeNodeDatabasePipelineVisibility visibility;
    private final WeightedTokens tokens = new WeightedTokens();
    private final String id;
    private final String descriptionPlain;
    private final TemplateScope scope;

    public CreateNewNodesByTemplateDatabaseEntry(JIPipeNodeTemplate template) {
        this(template, TemplateScope.Global);
    }

    public CreateNewNodesByTemplateDatabaseEntry(JIPipeNodeTemplate template, TemplateScope scope) {
        this.template = template;
        this.scope = scope;
        this.id = "node-template:" + template.getUUID();

        // Find visibility
        boolean foundPipelineOnly = false;
        for (JIPipeGraphNode graphNode : template.getGraph().getGraphNodes()) {
            if (!TaxonomyView.behaviorFlags(graphNode.getInfo()).isVisibleInCompartments()) {
                foundPipelineOnly = true;
            }
        }
        visibility = foundPipelineOnly ? JIPipeNodeDatabasePipelineVisibility.Pipeline : JIPipeNodeDatabasePipelineVisibility.Both;
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();

        initializeTokens();
    }

    /**
     * Re-keys every node of a template graph (a fresh {@code duplicate()} of
     * the template's stored graph) into the target canvas compartment, so a
     * subsequent {@link JIPipeGraph#mergeWith} inserts them into a compartment
     * the canvas actually renders (instead of the template's own phantom one).
     * <p>
     * Stored location metadata is re-keyed from the old compartment to the
     * new one: the canvas resolves stored locations under the node's CURRENT
     * compartment (the same lookup {@code pasteNodes} performs when it
     * re-anchors pasted nodes at the cursor), so a location left under the
     * template's own compartment would never resolve after the merge. The
     * stale entry is removed so it does not bloat saved projects.
     * <p>
     * Nodes whose source compartment already equals the target (the common
     * case for user-authored templates, which store the empty/default
     * compartment) are left untouched.
     *
     * @param graph             the duplicated template graph to fix up (mutated in place)
     * @param targetCompartment the canvas' compartment (null = the default/main compartment)
     */
    static void rekeyTemplateGraphToCompartment(JIPipeGraph graph, UUID targetCompartment) {
        String targetCompartmentKey = targetCompartment == null ? "_" : targetCompartment.toString();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            UUID sourceUuid = graph.getCompartmentUUIDOf(node);
            String sourceCompartmentKey = sourceUuid == null ? "_" : sourceUuid.toString();
            if (!sourceCompartmentKey.equals(targetCompartmentKey)) {
                Point location = node.getNodeUILocationWithin(sourceCompartmentKey);
                graph.setCompartment(node.getUUIDInParentGraph(), targetCompartment);
                if (location != null) {
                    node.setNodeUILocationWithin(targetCompartmentKey, location);
                    node.getNodeMetadata().clearEntriesWithPathPrefix(
                            JIPipePathMetadataStore.key("location", sourceCompartmentKey));
                }
            }
        }
    }

    private void initializeTokens() {
        tokens.add(template.getName(), WeightedTokens.WEIGHT_NAME);
        tokens.add(getLocationInfos().get(0), WeightedTokens.WEIGHT_NAME);
        tokens.add(scope.name(), WeightedTokens.WEIGHT_NAME);
        tokens.add(getBadge(), WeightedTokens.WEIGHT_NAME);
        tokens.add(template.getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
    }

    public TemplateScope getScope() {
        return scope;
    }

    /**
     * The display badge for this template entry: {@code Pattern} when the template
     * graph contains two or more nodes, otherwise {@code Custom}. Computed on demand
     * (never stored) from the live graph node count.
     */
    public String getBadge() {
        int nodeCount = template.getGraph() == null ? 0 : template.getGraph().getGraphNodes().size();
        return nodeCount >= 2 ? BADGE_PATTERN : BADGE_CUSTOM;
    }

    @Override
    public Set<String> getFunctionIds() {
        JIPipeTaxonomyFunctionRef ref = template.getTaxonomyFunction();
        if (ref != null && ref.isResolved()) {
            return Set.of(ref.getId());
        }
        // Unset or stale ref: fall back to the catch-all leaf so the template
        // always has a defined function-tree placement.
        return Set.of(TemplatesFunction.ID);
    }

    public JIPipeNodeTemplate getTemplate() {
        return template;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public WeightedTokens getTokens() {
        return tokens;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        return visibility;
    }

    @Override
    public String getName() {
        return template.getName();
    }

    @Override
    public HTMLText getDescription() {
        return template.getDescription();
    }

    @Override
    public Icon getIcon() {
        return template.getIconImage();
    }

    @Override
    public List<String> getLocationInfos() {
        // The function chain ("Data Sources > Image data") — the same
        // location form the node entries use — so a template reads as its
        // function, not as storage bookkeeping. The catch-all case (an
        // unset taxonomy function) falls back to the scope line so there is
        // always a location (the round-2 review: "Templates / System" was
        // not helpful in the 2-pane list).
        JIPipeTaxonomyFunctionRef ref = template.getTaxonomyFunction();
        if (ref != null && ref.isResolved() && ref.getFunction() != null
                && !TemplatesFunction.ID.equals(ref.getId())) {
            return Collections.singletonList(
                    CreateNewNodeByInfoDatabaseEntry.functionChainLocation(ref.getFunction()));
        }
        return Collections.singletonList("Templates / " + scope.name());
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getInputSlots() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
        return Collections.emptyMap();
    }

    @Override
    public Color getFillColor() {
        return ThemeUtils.getNodeFillColor(template.getColorHue());
    }

    @Override
    public Color getBorderColor() {
        return ThemeUtils.getNodeBorderColor(template.getColorHue());
    }

    @Override
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {

        JIPipeGraph graph = template.getGraph().duplicate();

        // A mined/extracted template graph carries its OWN compartment UUID
        // (e.g. the source project's pipeline UUID) in every node's
        // "jipipe:graph-compartment". mergeWith preserves that UUID, so the
        // inserted nodes would land in a phantom compartment that does not
        // exist on the canvas — isVisibleIn(canvasCompartment) is false for
        // all of them, the canvas creates no node UIs, and nothing happens
        // (no error, no log; the round-2 "double-click adds no nodes" bug).
        // The drag-and-drop path avoids this because the drop is serialized
        // and re-parsed through pasteNodes, which re-keys the compartment to
        // the target canvas before merging. Do the same here, on the
        // duplicated graph (see {@link #rekeyTemplateGraphToCompartment}).
        rekeyTemplateGraphToCompartment(graph, canvasUI.getCompartmentUUID());

        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforeAddNodes(graph.getGraphNodes(), canvasUI.getCompartmentUUID());
        }

        Map<UUID, JIPipeGraphNode> nodeMap = canvasUI.getGraph().mergeWith(graph);
        Set<JIPipeDesktopGraphNodeUI> nodeUISet = new HashSet<>();
        for (JIPipeGraphNode value : nodeMap.values()) {
            JIPipeDesktopGraphNodeUI ui = canvasUI.getNodeUIs().get(value);
            if (ui != null) {
                nodeUISet.add(ui);
            }
        }
        return nodeUISet;
    }

    @Override
    public boolean canAddInputSlots() {
        return false;
    }

    @Override
    public boolean canAddOutputSlots() {
        return false;
    }

    @Override
    public boolean isDeprecatedOrUnstable() {
        return false;
    }
}
