package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

public class ExistingCompartmentDatabaseEntry implements JIPipeNodeDatabaseEntry{
    private final String id;
    private final JIPipeProjectCompartment compartment;

    public ExistingCompartmentDatabaseEntry(String id, JIPipeProjectCompartment compartment) {
        this.id = id;
        this.compartment = compartment;
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
}
