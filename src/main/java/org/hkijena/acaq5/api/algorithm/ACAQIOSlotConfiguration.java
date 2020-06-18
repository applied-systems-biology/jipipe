package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.data.*;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * Slot configuration that always ensures 1:1 relation between input and output slots
 */
public class ACAQIOSlotConfiguration extends ACAQDefaultMutableSlotConfiguration {
    /**
     * Creates a new instance
     */
    public ACAQIOSlotConfiguration() {
    }

    @Override
    public ACAQSlotDefinition addSlot(String name, ACAQSlotDefinition definition, boolean user) {
        ACAQSlotDefinition newSlot = super.addSlot(name, definition, user);
        if(newSlot.isInput()) {
            ACAQSlotDefinition sisterSlot = new ACAQSlotDefinition(definition.getDataClass(), ACAQSlotType.Output, null);
            super.addSlot(name, sisterSlot, user);
        }
        else if(newSlot.isOutput()) {
            ACAQSlotDefinition sisterSlot = new ACAQSlotDefinition(definition.getDataClass(), ACAQSlotType.Input, null);
            super.addSlot(name, sisterSlot, user);
        }
        return newSlot;
    }

    @Override
    public void removeInputSlot(String name, boolean user) {
        super.removeInputSlot(name, user);
        super.removeOutputSlot(name, user);
    }

    @Override
    public void removeOutputSlot(String name, boolean user) {
        super.removeOutputSlot(name, user);
        super.removeInputSlot(name, user);
    }
}
