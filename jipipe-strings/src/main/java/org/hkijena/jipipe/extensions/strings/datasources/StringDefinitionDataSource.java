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

package org.hkijena.jipipe.extensions.strings.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.strings.StringData;

@JIPipeDocumentation(name = "Define string", description = "Creates a string data object from a parameter")
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class StringDefinitionDataSource extends JIPipeSimpleIteratingAlgorithm {

    private String value = "";

    public StringDefinitionDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public StringDefinitionDataSource(StringDefinitionDataSource other) {
        super(other);
        this.value = other.value;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(value), progressInfo);
    }

    @JIPipeDocumentation(name = "Value", description = "The value that will be generated")
    @StringParameterSettings(monospace = true, multiline = true)
    @JIPipeParameter("value")
    public String getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(String value) {
        this.value = value;
    }
}
