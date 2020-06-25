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

package org.hkijena.acaq5.extensions.multiparameters.algorithms;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Declaration for {@link MultiParameterAlgorithm}
 */
public class MultiParameterAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

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
    public Class<? extends ACAQGraphNode> getAlgorithmClass() {
        return MultiParameterAlgorithm.class;
    }

    @Override
    public ACAQGraphNode newInstance() {
        return new MultiParameterAlgorithm(this);
    }

    @Override
    public ACAQGraphNode clone(ACAQGraphNode algorithm) {
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

    @Override
    public boolean isHidden() {
        return false;
    }
}
