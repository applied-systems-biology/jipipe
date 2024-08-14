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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateNewCompartmentNodeDatabaseEntry implements JIPipeNodeDatabaseEntry {

    private final WeightedTokens tokens = new WeightedTokens();

    public CreateNewCompartmentNodeDatabaseEntry() {
        initializeTokens();
    }

    private void initializeTokens() {
        tokens.add("Graph compartment", WeightedTokens.WEIGHT_NAME);
    }

    @Override
    public String getId() {
        return "create-node-custom:jipipe:graph-compartment";
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
        return JIPipeNodeDatabasePipelineVisibility.Compartments;
    }

    @Override
    public String getName() {
        return "Graph compartment";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Creates a new compartment");
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/graph-compartment.png");
    }

    @Override
    public List<String> getLocationInfos() {
        return Collections.singletonList("Compartment");
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
        return Color.WHITE;
    }

    @Override
    public Color getBorderColor() {
        return Color.GRAY;
    }

    @Override
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI = (JIPipeDesktopCompartmentsGraphEditorUI) canvasUI.getGraphEditorUI();
        graphEditorUI.addCompartment();
        return null;
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
