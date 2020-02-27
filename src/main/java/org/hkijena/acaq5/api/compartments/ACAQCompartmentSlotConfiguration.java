package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;

public class ACAQCompartmentSlotConfiguration extends ACAQMutableSlotConfiguration {
    public ACAQCompartmentSlotConfiguration() {
    }

    @Override
    public void addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        super.addInputSlot(name, klass);
        if(!getSlots().containsKey("Output " + name)) {
            addOutputSlot("Output " + name, klass);
        }
    }

    @Override
    public void addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!name.startsWith("Output "))
            name = "Output " + name;
        super.addOutputSlot(name, klass);
        String inputName = name.substring("Output ".length());
        if(!getSlots().containsKey(inputName))
            addInputSlot(inputName, klass);
    }
}
