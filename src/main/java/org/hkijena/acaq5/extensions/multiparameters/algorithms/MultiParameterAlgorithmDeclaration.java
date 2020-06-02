package org.hkijena.acaq5.extensions.multiparameters.algorithms;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.algorithm.DefaultAlgorithmInputSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Declaration for {@link MultiParameterAlgorithm}
 */
public class MultiParameterAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

    private ACAQDataSlotTraitConfiguration slotConfiguration = new ACAQDataSlotTraitConfiguration();
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();

    /**
     * Creates a new instance
     */
    public MultiParameterAlgorithmDeclaration() {
        this.inputSlots.add(new DefaultAlgorithmInputSlot(ParametersData.class, "Parameters", false));
    }

    @Override
    public String getId() {
        return "multiparameter-wrapper";
    }

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return MultiParameterAlgorithm.class;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return new MultiParameterAlgorithm(this);
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return new MultiParameterAlgorithm((MultiParameterAlgorithm) algorithm);
    }

    @Override
    public String getName() {
        return "Apply parameters";
    }

    @Override
    public String getDescription() {
        return "Applies each input parameter to an algorithm";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.Miscellaneous;
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return Collections.emptySet();
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return Collections.emptySet();
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return slotConfiguration;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        return Collections.emptySet();
    }
}
