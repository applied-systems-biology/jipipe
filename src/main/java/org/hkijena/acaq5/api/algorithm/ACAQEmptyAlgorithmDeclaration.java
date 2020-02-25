package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;

import java.util.List;
import java.util.Set;

public class ACAQEmptyAlgorithmDeclaration implements ACAQAlgorithmDeclaration {
    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return null;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return null;
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return null;
    }

    @Override
    public Set<Class<? extends ACAQTrait>> getPreferredTraits() {
        return null;
    }

    @Override
    public Set<Class<? extends ACAQTrait>> getUnwantedTraits() {
        return null;
    }

    @Override
    public List<AddsTrait> getAddedTraits() {
        return null;
    }

    @Override
    public List<RemovesTrait> getRemovedTraits() {
        return null;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return null;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return null;
    }

    @Override
    public boolean matches(JsonNode node) {
        return false;
    }
}
