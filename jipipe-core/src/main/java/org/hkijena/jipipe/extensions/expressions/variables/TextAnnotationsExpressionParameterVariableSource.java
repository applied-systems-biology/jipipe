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

package org.hkijena.jipipe.extensions.expressions.variables;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.*;

/**
 * Adds the indication that annotations are available
 */
public class TextAnnotationsExpressionParameterVariableSource implements ExpressionParameterVariableSource {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Map<UUID, Map<String, JIPipeDataTable>> predecessorNodeCache = ExpressionParameterVariableSource.findPredecessorNodeCache(parameterTree, parameterAccess);
        if(!predecessorNodeCache.isEmpty()) {
            Set<ExpressionParameterVariable> variables = new HashSet<>();
            variables.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            for (Map.Entry<UUID, Map<String, JIPipeDataTable>> uuidMapEntry : predecessorNodeCache.entrySet()) {
                for (Map.Entry<String, JIPipeDataTable> dataTableEntry : uuidMapEntry.getValue().entrySet()) {
                    for (String annotationColumn : dataTableEntry.getValue().getTextAnnotationColumns()) {
                        variables.add(new ExpressionParameterVariable(annotationColumn + " (Annotation)",
                                "The text annotation \"" + annotationColumn + "\". Automatically detected from cached predecessor nodes.",
                                annotationColumn));
                    }
                }
            }

            return variables;
        }
        else {
            return Collections.singleton(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
        }
    }
}
