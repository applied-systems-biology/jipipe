package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;

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

    @Override
    public void removeSlot(String name) {
        ACAQSlotDefinition slot = getSlots().get(name);
        super.removeSlot(name);

        if(slot.getSlotType() == ACAQDataSlot.SlotType.Input) {
            String outputName = "Output " + name;
            if(hasSlot(outputName)) {
                super.removeSlot(outputName);
            }
        }
        else if(slot.getSlotType() == ACAQDataSlot.SlotType.Output) {
            String inputName = name.substring("Output ".length());
            if(hasSlot(inputName)) {
                super.removeSlot(inputName);
            }
        }
    }
}
