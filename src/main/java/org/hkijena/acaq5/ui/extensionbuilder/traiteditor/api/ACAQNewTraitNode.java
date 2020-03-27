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

import java.util.Map;

@ACAQDocumentation(name = "Custom Annotation", description = "A custom annotation type")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Annotation)
public class ACAQNewTraitNode extends ACAQTraitNode implements ACAQCustomParameterHolder {

    public ACAQNewTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQNewTraitNode(ACAQNewTraitNode other) {
        super(other);
    }

    @Override
    public void setTraitDeclaration(ACAQTraitDeclaration traitDeclaration) {
        super.setTraitDeclaration(traitDeclaration);
        ACAQJsonTraitDeclaration jsonTraitDeclaration = (ACAQJsonTraitDeclaration) traitDeclaration;
        jsonTraitDeclaration.getEventBus().register(this);
    }

    @Override
    public Map<String, ACAQParameterAccess> getCustomParameters() {
        return ACAQParameterAccess.getParameters((ACAQParameterHolder) getTraitDeclaration());
    }

    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if ("name".equals(event.getKey())) {
            setCustomName(getTraitDeclaration().getName());
        }
        getEventBus().post(event);
    }
}
