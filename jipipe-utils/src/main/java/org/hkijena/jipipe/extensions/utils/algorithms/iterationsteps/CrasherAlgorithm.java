package org.hkijena.jipipe.extensions.utils.algorithms.iterationsteps;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

@SetJIPipeDocumentation(name = "Crash on inputs", description = "Cancels the current run if data is processed. Use this node to inform users about unwanted conditions in your pipeline.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class CrasherAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String errorTitle = "Data was received in 'Crash on inputs'";
    private String errorExplanation = "";
    private String errorSolution = "";

    public CrasherAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CrasherAlgorithm(CrasherAlgorithm other) {
        super(other);
        this.errorTitle = other.errorTitle;
        this.errorExplanation = other.errorExplanation;
        this.errorSolution = other.errorSolution;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        throw new JIPipeValidationRuntimeException(new GraphNodeValidationReportContext(this),
                new IllegalArgumentException("Data was received in 'Crash on inputs'"),
                errorTitle, errorExplanation, errorSolution);
    }

    @SetJIPipeDocumentation(name = "Error title", description = "The title of the generated error message")
    @JIPipeParameter(value = "error-title", uiOrder = -99)
    public String getErrorTitle() {
        return errorTitle;
    }

    @JIPipeParameter("error-title")
    public void setErrorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
    }

    @SetJIPipeDocumentation(name = "Error explanation", description = "Explanation for the user")
    @JIPipeParameter("error-explanation")
    @StringParameterSettings(multiline = true)
    public String getErrorExplanation() {
        return errorExplanation;
    }

    @JIPipeParameter("error-explanation")
    public void setErrorExplanation(String errorExplanation) {
        this.errorExplanation = errorExplanation;
    }

    @SetJIPipeDocumentation(name = "Error solution", description = "Solution for the user")
    @JIPipeParameter("error-solution")
    @StringParameterSettings(multiline = true)
    public String getErrorSolution() {
        return errorSolution;
    }

    @JIPipeParameter("error-solution")
    public void setErrorSolution(String errorSolution) {
        this.errorSolution = errorSolution;
    }
}
