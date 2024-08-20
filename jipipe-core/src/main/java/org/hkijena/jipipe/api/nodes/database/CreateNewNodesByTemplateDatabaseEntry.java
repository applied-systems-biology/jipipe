package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class CreateNewNodesByTemplateDatabaseEntry implements JIPipeNodeDatabaseEntry {

    private final JIPipeNodeTemplate template;
    private final JIPipeNodeDatabasePipelineVisibility visibility;
    private final WeightedTokens tokens = new WeightedTokens();
    private final String id;
    private final String descriptionPlain;
    private final String locationInfo;

    public CreateNewNodesByTemplateDatabaseEntry(JIPipeNodeTemplate template) {
        this.template = template;
        this.id = "node-template:" + UUID.randomUUID();

        // Find visibility
        boolean foundPipelineOnly = false;
        for (JIPipeGraphNode graphNode : template.getGraph().getGraphNodes()) {
            if (!graphNode.getInfo().getCategory().isVisibleInCompartments()) {
                foundPipelineOnly = true;
            }
        }
        visibility = foundPipelineOnly ? JIPipeNodeDatabasePipelineVisibility.Pipeline : JIPipeNodeDatabasePipelineVisibility.Both;
        this.descriptionPlain = Jsoup.parse(getDescription().getHtml()).text();

        // Build location info
        if (template.getMenuPath().isEmpty()) {
            this.locationInfo = "Templates";
        } else {
            this.locationInfo = "Templates\n" + String.join("\n", template.getMenuPath());
        }

        initializeTokens();
    }

    private void initializeTokens() {
        tokens.add(template.getName(), WeightedTokens.WEIGHT_NAME);
        tokens.add(locationInfo, WeightedTokens.WEIGHT_NAME);
        tokens.add(template.getDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
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
        return Collections.singletonList(locationInfo);
    }

    @Override
    public Set<String> getCategoryIds() {
        return Collections.emptySet();
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
        return template.getFillColor();
    }

    @Override
    public Color getBorderColor() {
        return template.getBorderColor();
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
    public boolean isDeprecated() {
        return false;
    }
}
