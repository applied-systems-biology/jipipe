package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Structural node that represents a new trait
 */
@ACAQDocumentation(name = "Custom Annotation", description = "A custom annotation type")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation)
@ACAQHidden
public class ACAQNewTraitNode extends ACAQTraitNode implements ACAQCustomParameterCollection {

    /**
     * Creates new instance
     *
     * @param declaration The declaration
     */
    public ACAQNewTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the node
     *
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
    public Map<String, ACAQParameterAccess> getParameters() {
        if (getTraitDeclaration() == null)
            return Collections.emptyMap();
        return ACAQTraversedParameterCollection.getParameters((ACAQParameterCollection) getTraitDeclaration());
    }

    /**
     * Triggered when the name, id or is-discriminator parameters are changed
     *
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
