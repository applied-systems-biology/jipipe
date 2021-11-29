package org.hkijena.jipipe.extensions.utils.algorithms.processes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.OptionalDefaultExpressionParameter;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;

@JIPipeDocumentation(name = "Run process (Iterating)", description = "Executes a process.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Process")
public class RunProcessIteratingAlgorithm extends JIPipeIteratingAlgorithm {

    private ProcessEnvironment processEnvironment = new ProcessEnvironment();
    private OptionalDefaultExpressionParameter overrideArguments = new OptionalDefaultExpressionParameter(true, "ARRAY()");

    public RunProcessIteratingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RunProcessIteratingAlgorithm(RunProcessIteratingAlgorithm other) {
        super(other);
        this.processEnvironment = new ProcessEnvironment(other.processEnvironment);
        this.overrideArguments = new OptionalDefaultExpressionParameter(other.overrideArguments);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }

    @JIPipeDocumentation(name = "Process", description = "The process that should be executed")
    @JIPipeParameter(value = "process-environment", important = true)
    public ProcessEnvironment getProcessEnvironment() {
        return processEnvironment;
    }

    @JIPipeParameter("process-environment")
    public void setProcessEnvironment(ProcessEnvironment processEnvironment) {
        this.processEnvironment = processEnvironment;
    }

    @JIPipeDocumentation(name = "Override arguments", description = "If enabled, override arguments of the environment with the one provided by the expression. " +
            "Please note that this expression has access to annotations.")
    @JIPipeParameter("override-arguments")
    public OptionalDefaultExpressionParameter getOverrideArguments() {
        return overrideArguments;
    }

    @JIPipeParameter("override-arguments")
    public void setOverrideArguments(OptionalDefaultExpressionParameter overrideArguments) {
        this.overrideArguments = overrideArguments;
    }
}
