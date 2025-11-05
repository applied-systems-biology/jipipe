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

package org.hkijena.jipipe.plugins.r.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.plugins.r.parameters.RScriptParameter;
import org.hkijena.jipipe.plugins.strings.RScriptData;

@SetJIPipeDocumentation(name = "Define R script", description = "Defines an R script")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Annotations", create = true, optional = true)
@AddJIPipeOutputSlot(value = RScriptData.class, name = "Script", create = true)
public class DefineRScriptAlgorithm extends JIPipeSimpleIteratingAlgorithm implements JIPipeScriptAlgorithm {

    private RScriptParameter script = new RScriptParameter();

    public DefineRScriptAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DefineRScriptAlgorithm(DefineRScriptAlgorithm other) {
        super(other);
        this.script = new RScriptParameter(other.script);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), new RScriptData(script.getCode()), progressInfo);
    }

    @Override
    protected boolean isAllowEmptyIterationStep() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Script", description = "The R script")
    @JIPipeParameter("script")
    public RScriptParameter getScript() {
        return script;
    }

    @JIPipeParameter("script")
    public void setScript(RScriptParameter script) {
        this.script = script;
    }

    @Override
    public JIPipeParameterAccess getScriptParameterAccess() {
        return getParameterAccess("script");
    }
}
