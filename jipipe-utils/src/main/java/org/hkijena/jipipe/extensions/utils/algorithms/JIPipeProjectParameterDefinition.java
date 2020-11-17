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

package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.multiparameters.datasources.ParametersDataTableDefinition;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;

@JIPipeDocumentation(name = "Define JIPipe project parameters", description = "Defines parameters that will be put into JIPipe projects. The parameter key has two modes: " +
        "If it matches with a pipeline parameter (that can be set up via a pipeline's settings), this parameter is changed. It can also match with an absolute path to a node's parameter with following format: " +
        "[node-id]/[node parameter key]")
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class JIPipeProjectParameterDefinition extends ParametersDataTableDefinition {
    public JIPipeProjectParameterDefinition(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeProjectParameterDefinition(ParametersDataTableDefinition other) {
        super(other);
    }
}
