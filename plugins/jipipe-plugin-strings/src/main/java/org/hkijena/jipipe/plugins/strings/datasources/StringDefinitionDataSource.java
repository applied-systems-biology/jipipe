/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.strings.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.strings.StringData;

@SetJIPipeDocumentation(name = "Define string", description = "Creates a string data object from a parameter")
@AddJIPipeOutputSlot(value = StringData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(value), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Value", description = "The value that will be generated")
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
