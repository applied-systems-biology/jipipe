package org.hkijena.jipipe.api.nodes.database.entries;

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.DefaultJIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.WeightedTokens;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TemplatesFunction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.ThemeUtils;
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

        // Give the inserted nodes an arrangement derived from the template's
        // own structure (longest-path layering, top-to-bottom), anchored at
        // the current editor cursor. Without this, template nodes carry no
        // location (the mined/extracted graphs store none) and the canvas
        // drops every one of them onto the cursor position (the round-2
        // review bug: an inserted pattern arrived as a bunch at one spot).
        // Locations are keyed under "" (no compartment) exactly like
        // JIPipeGraph.extract stores them, so the canvas's
        // moveToStoredGridLocation picks them up after the merge.
        arrangeTemplateGraph(graph, canvasUI);

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

    /**
     * Computes grid locations for the template's nodes from the template's
     * own wiring (a lightweight local layout — NOT a full-canvas Sugiyama,
     * which would re-arrange the surrounding pipeline and can over-expand
     * larger patterns): each node's column is its longest distance from a
     * source node (longest-path layering, left-to-right), nodes of the same
     * layer stack top-to-bottom. The whole arrangement is translated so its
     * top-left sits at the editor cursor (converted to grid units).
     */
    private static void arrangeTemplateGraph(JIPipeGraph graph, JIPipeDesktopGraphCanvasUI canvasUI) {
        Map<JIPipeGraphNode, Integer> layer = new HashMap<>();
        for (JIPipeDataSlot slot : graph.traverseSlots()) {
            // traverseSlots yields inputs before their consumers, so a
            // node's inputs are all layered before the node is visited.
            JIPipeGraphNode node = slot.getNode();
            if (layer.containsKey(node)) {
                continue;
            }
            int l = 0;
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : graph.getInputIncomingSourceSlots(inputSlot)) {
                    Integer sourceLayer = layer.get(sourceSlot.getNode());
                    if (sourceLayer != null) {
                        l = Math.max(l, sourceLayer + 1);
                    }
                }
            }
            layer.put(node, l);
        }
        // A node with no slots at all never appears in traverseSlots —
        // assign it layer 0 so it still gets a location.
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            layer.putIfAbsent(node, 0);
        }
        // Stack the nodes of each layer top-to-bottom (traverse order is
        // stable and input-first)
        Map<Integer, List<JIPipeGraphNode>> columns = new TreeMap<>();
        for (Map.Entry<JIPipeGraphNode, Integer> e : layer.entrySet()) {
            columns.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        // Anchor at the editor cursor in grid units (rounding like the canvas)
        Point cursor = canvasUI.getGraphEditorCursor();
        int baseX = (int) Math.round((cursor.x / canvasUI.getZoom()) / JIPipeDesktopGraphCanvasGrid.GRID_WIDTH);
        int baseY = (int) Math.round((cursor.y / canvasUI.getZoom()) / JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT);
        // Reserve ~2 grid rows per node so stacked nodes do not overlap
        for (Map.Entry<Integer, List<JIPipeGraphNode>> column : columns.entrySet()) {
            List<JIPipeGraphNode> nodes = column.getValue();
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).setNodeUILocationWithin("",
                    new Point(baseX + column.getKey(), baseY + i * 2));
            }
        }
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
