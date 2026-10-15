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
