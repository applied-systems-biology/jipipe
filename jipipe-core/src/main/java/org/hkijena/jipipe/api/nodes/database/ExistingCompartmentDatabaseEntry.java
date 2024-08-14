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

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExistingCompartmentDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeProjectCompartment compartment;
    private final WeightedTokens tokens = new WeightedTokens();
    private final Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private final Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();

    public ExistingCompartmentDatabaseEntry(String id, JIPipeProjectCompartment compartment) {
        this.id = id;
        this.compartment = compartment;
        initializeSlots();
        initializeTokens();
    }

    private void initializeSlots() {
        inputSlots.put("Input", new JIPipeDataSlotInfo(JIPipeCompartmentOutputData.class,
                JIPipeSlotType.Input,
                "Input",
                null));
        outputSlots.put("Output", new JIPipeDataSlotInfo(JIPipeCompartmentOutputData.class,
                JIPipeSlotType.Output,
                "Output",
                null));
    }

    @Override
    public JIPipeNodeDatabasePipelineVisibility getVisibility() {
        return JIPipeNodeDatabasePipelineVisibility.Compartments;
    }

    @Override
    public String getName() {
        return compartment.getName();
    }

    @Override
    public HTMLText getDescription() {
        return compartment.getCustomDescription();
    }

    @Override
    public Icon getIcon() {
        return compartment.getInfo().getIcon();
    }

    private void initializeTokens() {
        tokens.add(compartment.getName(), WeightedTokens.WEIGHT_NAME);
        tokens.add(compartment.getCustomDescription().getBody(), WeightedTokens.WEIGHT_DESCRIPTION);
    }

    @Override
    public WeightedTokens getTokens() {
        return tokens;
    }

    @Override
    public String getId() {
        return id;
    }

    public JIPipeGraphNode getCompartment() {
        return compartment;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public List<String> getLocationInfos() {
        return Collections.singletonList("Compartments");
    }

    @Override
    public Set<String> getCategoryIds() {
        return Collections.emptySet();
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
        return Color.WHITE;
    }

    @Override
    public Color getBorderColor() {
        return Color.LIGHT_GRAY;
    }

    @Override
    public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
        return Collections.singleton(canvasUI.getNodeUIs().get(compartment));
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
