package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.util.List;

public interface IterationStepSolver {
    List<JIPipeMultiIterationStep> solve(JIPipeMultiIterationStepGenerator generator, JIPipeProgressInfo progressInfo);
}
