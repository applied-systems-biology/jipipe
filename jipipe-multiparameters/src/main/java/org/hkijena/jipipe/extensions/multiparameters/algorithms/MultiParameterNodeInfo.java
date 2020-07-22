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

package org.hkijena.jipipe.extensions.multiparameters.algorithms;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Info for {@link MultiParameterAlgorithm}
 */
public class MultiParameterNodeInfo implements JIPipeNodeInfo {

    private List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<JIPipeOutputSlot> outputSlots = new ArrayList<>();

    /**
     * Creates a new instance
     */
    public MultiParameterNodeInfo() {
        this.inputSlots.add(new DefaultJIPipeInputSlot(ParametersData.class, "Parameters", false));
    }

    @Override
    public String getId() {
        return "multiparameter-wrapper";
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return MultiParameterAlgorithm.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new MultiParameterAlgorithm(this);
    }

    @Override
    public JIPipeGraphNode clone(JIPipeGraphNode algorithm) {
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
    public JIPipeNodeTypeCategory getCategory() {
        return new MiscellaneousNodeTypeCategory();
    }

    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return true;
    }
}
