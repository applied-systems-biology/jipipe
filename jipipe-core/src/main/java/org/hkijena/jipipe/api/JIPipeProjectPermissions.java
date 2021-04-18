package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public class JIPipeProjectPermissions implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean preventAddingDeletingNodes = false;
    private boolean preventModifyingSlots = false;

    @JIPipeDocumentation(name = "Prevent adding/deleting nodes", description = "If enabled, users cannot add or delete nodes or compartments. Use this for teaching environments.")
    @JIPipeParameter("prevent-adding-deleting-nodes")
    @JsonGetter("prevent-adding-deleting-nodes")
    public boolean isPreventAddingDeletingNodes() {
        return preventAddingDeletingNodes;
    }

    @JIPipeParameter("prevent-adding-deleting-nodes")
    @JsonSetter("prevent-adding-deleting-nodes")
    public void setPreventAddingDeletingNodes(boolean preventAddingDeletingNodes) {
        this.preventAddingDeletingNodes = preventAddingDeletingNodes;
    }

    @JIPipeDocumentation(name = "Prevent modifying slots", description = "If enabled, users cannot modify slots (add, delete, edit). " +
            "They can still re-label them. Use this for teaching environments.")
    @JIPipeParameter("prevent-modifying-slots")
    @JsonGetter("prevent-modifying-slots")
    public boolean isPreventModifyingSlots() {
        return preventModifyingSlots;
    }

    @JIPipeParameter("prevent-modifying-slots")
    @JsonSetter("prevent-modifying-slots")
    public void setPreventModifyingSlots(boolean preventModifyingSlots) {
        this.preventModifyingSlots = preventModifyingSlots;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
