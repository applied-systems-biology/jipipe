package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExistingCompartmentDatabaseEntry implements JIPipeNodeDatabaseEntry{
    private final String id;
    private final JIPipeProjectCompartment compartment;
    private final List<String> tokens = new ArrayList<>();
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
    public JIPipeNodeDatabaseRole getRole() {
        return JIPipeNodeDatabaseRole.CompartmentNode;
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
        tokens.add(compartment.getName());
        tokens.add(compartment.getCustomDescription().getBody());
    }

    @Override
    public List<String> getTokens() {
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
    public String getCategory() {
        return "Compartments";
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getInputSlots() {
        return inputSlots;
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
        return outputSlots;
    }
}
