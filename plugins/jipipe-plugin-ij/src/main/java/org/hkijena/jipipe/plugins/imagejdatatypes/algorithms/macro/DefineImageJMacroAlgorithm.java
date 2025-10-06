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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.macro;

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
import org.hkijena.jipipe.plugins.parameters.library.scripts.ImageJMacroParameter;
import org.hkijena.jipipe.plugins.parameters.library.scripts.PythonScriptParameter;
import org.hkijena.jipipe.plugins.strings.ImageJMacroData;
import org.hkijena.jipipe.plugins.strings.PythonScriptData;

@SetJIPipeDocumentation(name = "ImageJ macro", description = "Defines an ImageJ macro")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Annotations", create = true, optional = true)
@AddJIPipeOutputSlot(value = ImageJMacroData.class, name = "Script", create = true)
public class DefineImageJMacroAlgorithm extends JIPipeSimpleIteratingAlgorithm implements JIPipeScriptAlgorithm {

    private ImageJMacroParameter script = new ImageJMacroParameter();

    public DefineImageJMacroAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DefineImageJMacroAlgorithm(DefineImageJMacroAlgorithm other) {
        super(other);
        this.script = new ImageJMacroParameter(other.script);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), new ImageJMacroData(script.getCode()), progressInfo);
    }

    @Override
    protected boolean isAllowEmptyIterationStep() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Macro", description = "The ImageJ macro")
    @JIPipeParameter("script")
    public ImageJMacroParameter getScript() {
        return script;
    }

    @JIPipeParameter("script")
    public void setScript(ImageJMacroParameter script) {
        this.script = script;
    }

    @Override
    public JIPipeParameterAccess getScriptParameterAccess() {
        return getParameterAccess("script");
    }
}
