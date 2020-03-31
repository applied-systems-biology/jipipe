package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterHolder;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.Map;

/**
 * Structural node that represents a new trait
 */
@ACAQDocumentation(name = "Custom Annotation", description = "A custom annotation type")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmInputSlot(ACAQDiscriminatorNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQDiscriminatorNodeInheritanceData.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)
public class ACAQNewTraitNode extends ACAQTraitNode implements ACAQCustomParameterHolder {

    /**
     * Creates new instance
     * @param declaration The declaration
     */
    public ACAQNewTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the node
     * @param other The original
     */
    public ACAQNewTraitNode(ACAQNewTraitNode other) {
        super(other);
    }

    @Override
    public void setTraitDeclaration(ACAQTraitDeclaration traitDeclaration) {
        super.setTraitDeclaration(traitDeclaration);
        ACAQJsonTraitDeclaration jsonTraitDeclaration = (ACAQJsonTraitDeclaration) traitDeclaration;
        jsonTraitDeclaration.getEventBus().register(this);
        updateCustomName();
    }

    private void updateCustomName() {
        String name = getTraitDeclaration().getName();
        if (StringUtils.isNullOrEmpty(name)) {
            if (!StringUtils.isNullOrEmpty(getTraitDeclaration().getId())) {
                name = "<" + getTraitDeclaration().getId() + ">";
            } else {
                name = null;
            }
        }
        setCustomName(name);
    }

    @Override
    public Map<String, ACAQParameterAccess> getCustomParameters() {
        return ACAQParameterAccess.getParameters((ACAQParameterHolder) getTraitDeclaration());
    }

    /**
     * Triggered when the name, id or is-discriminator parameters are changed
     * @param event Generated event
     */
    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if ("name".equals(event.getKey()) || "id".equals(event.getKey())) {
            updateCustomName();
        } else if ("is-discriminator".equals(event.getKey())) {
            updateSlotTypes();
        }
        getEventBus().post(event);
    }
}
