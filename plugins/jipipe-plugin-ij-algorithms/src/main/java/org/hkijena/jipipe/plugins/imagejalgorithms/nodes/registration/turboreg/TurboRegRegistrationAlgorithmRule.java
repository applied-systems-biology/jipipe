package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.AlignedImage5DSliceIndexExpressionParameterVariablesInfo;

public class TurboRegRegistrationAlgorithmRule extends AbstractJIPipeParameterCollection {
    private JIPipeExpressionParameter condition = new JIPipeExpressionParameter("true");
    private JIPipeExpressionParameter referenceCIndex = new JIPipeExpressionParameter("c");
    private JIPipeExpressionParameter referenceZIndex = new JIPipeExpressionParameter("z");
    private JIPipeExpressionParameter referenceTIndex = new JIPipeExpressionParameter("t");
    private TurboRegRegistrationAlgorithmRuleType ruleType = TurboRegRegistrationAlgorithmRuleType.CalculateTransformation;

    public TurboRegRegistrationAlgorithmRule() {
    }

    public TurboRegRegistrationAlgorithmRule(TurboRegRegistrationAlgorithmRule other) {
        this.condition = other.condition;
        this.referenceCIndex = new JIPipeExpressionParameter(other.referenceCIndex);
        this.referenceZIndex = new JIPipeExpressionParameter(other.referenceZIndex);
        this.referenceTIndex = new JIPipeExpressionParameter(other.referenceTIndex);
        this.ruleType = other.ruleType;
    }

    @SetJIPipeDocumentation(name = "Match if ...", description = "Determines whether this condition matches")
    @JIPipeParameter(value = "condition", uiOrder = -99)
    public JIPipeExpressionParameter getCondition() {
        return condition;
    }

    @JIPipeParameter("condition")
    public void setCondition(JIPipeExpressionParameter condition) {
        this.condition = condition;
    }

    @SetJIPipeDocumentation(name = "Reference channel index", description = "Depends from which slice the transformation is sourced.<br/>" +
            "For type 'Calculate transformation' this refers to a Z/C/T slice in the reference image.<br/>" +
            "For type 'Use transformation' this refers to a Z/C/T slice that already has a transformation.<br/>" +
            "Please note unless you only have 'Ignore' type rules there must be some alignment transformation that needs to be calculated.")
    @JIPipeParameter("reference-c-index")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AlignedImage5DSliceIndexExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per input Z/C/T slice")
    public JIPipeExpressionParameter getReferenceCIndex() {
        return referenceCIndex;
    }

    @JIPipeParameter("reference-c-index")
    public void setReferenceCIndex(JIPipeExpressionParameter referenceCIndex) {
        this.referenceCIndex = referenceCIndex;
    }

    @SetJIPipeDocumentation(name = "Reference frame index", description = "Depends from which slice the transformation is sourced.<br/>" +
            "For type 'Calculate transformation' this refers to a Z/C/T slice in the reference image.<br/>" +
            "For type 'Use transformation' this refers to a Z/C/T slice that already has a transformation.<br/>" +
            "Please note unless you only have 'Ignore' type rules there must be some alignment transformation that needs to be calculated.")
    @JIPipeParameter("reference-t-index")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AlignedImage5DSliceIndexExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per input Z/C/T slice")
    public JIPipeExpressionParameter getReferenceTIndex() {
        return referenceTIndex;
    }

    @JIPipeParameter("reference-t-index")
    public void setReferenceTIndex(JIPipeExpressionParameter referenceTIndex) {
        this.referenceTIndex = referenceTIndex;
    }

    @SetJIPipeDocumentation(name = "Reference Z index", description = "Depends from which slice the transformation is sourced.<br/>" +
            "For type 'Calculate transformation' this refers to a Z/C/T slice in the reference image.<br/>" +
            "For type 'Use transformation' this refers to a Z/C/T slice that already has a transformation.<br/>" +
            "Please note unless you only have 'Ignore' type rules there must be some alignment transformation that needs to be calculated.")
    @JIPipeParameter("reference-z-index")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AlignedImage5DSliceIndexExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per input Z/C/T slice")
    public JIPipeExpressionParameter getReferenceZIndex() {
        return referenceZIndex;
    }

    @JIPipeParameter("reference-z-index")
    public void setReferenceZIndex(JIPipeExpressionParameter referenceZIndex) {
        this.referenceZIndex = referenceZIndex;
    }

    @SetJIPipeDocumentation(name = "Type", description = "Determines if the slices matches by this rule are ignored, are aligned to the reference, " +
            "or follow a pre-calculated transformation")
    @JIPipeParameter("rule-type")
    public TurboRegRegistrationAlgorithmRuleType getRuleType() {
        return ruleType;
    }

    @JIPipeParameter("rule-type")
    public void setRuleType(TurboRegRegistrationAlgorithmRuleType ruleType) {
        this.ruleType = ruleType;
    }
}
