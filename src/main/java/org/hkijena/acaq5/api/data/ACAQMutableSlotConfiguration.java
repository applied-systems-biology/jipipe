/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.data;

import java.util.Set;

/**
 * A slot configuration that can be changed.
 */
public interface ACAQMutableSlotConfiguration extends ACAQSlotConfiguration {
    Set<Class<? extends ACAQData>> getAllowedInputSlotTypes();

    Set<Class<? extends ACAQData>> getAllowedOutputSlotTypes();

    boolean isAllowInheritedOutputSlots();

    boolean allowsInputSlots();

    boolean allowsOutputSlots();

    boolean isInputSlotsSealed();

    boolean isOutputSlotsSealed();

    void removeInputSlot(String name, boolean user);

    void removeOutputSlot(String name, boolean user);

    boolean canAddInputSlot();

    boolean canModifyInputSlots();

    boolean canModifyOutputSlots();

    boolean canAddOutputSlot();

    boolean canCreateCompatibleInputSlot(Class<? extends ACAQData> acceptedDataType);

    ACAQSlotDefinition addSlot(String name, ACAQSlotDefinition definition, boolean user);

    void moveDown(String name, ACAQSlotType slotType);

    void moveUp(String name, ACAQSlotType slotType);
}
