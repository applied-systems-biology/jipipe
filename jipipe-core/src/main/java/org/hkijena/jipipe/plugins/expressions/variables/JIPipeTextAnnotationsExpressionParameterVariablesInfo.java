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

package org.hkijena.jipipe.plugins.expressions.variables;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;

import java.util.*;

/**
 * Adds the indication that annotations are available
 */
public class JIPipeTextAnnotationsExpressionParameterVariablesInfo implements JIPipeExpressionVariablesInfo {

    /**
     * Reusable annotation instance that advertises annotation variables.
     * Intended for parameters that are created dynamically (e.g. via
     * {@link org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection#addParameter})
     * and therefore cannot be annotated at a getter.
     */
    public static final AddJIPipeExpressionParameterVariable ANNOTATION_VARIABLE = new AddJIPipeExpressionParameterVariable() {
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return AddJIPipeExpressionParameterVariable.class;
        }

        @Override
        public String name() {
            return "";
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public String key() {
            return "";
        }

        @Override
        public Class<? extends JIPipeExpressionVariablesInfo> fromClass() {
            return JIPipeTextAnnotationsExpressionParameterVariablesInfo.class;
        }
    };

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Map<UUID, Map<String, JIPipeDataTable>> predecessorNodeCache = JIPipeExpressionVariablesInfo.findPredecessorNodeCache(parameterTree, parameterAccess);
        if (!predecessorNodeCache.isEmpty()) {
            Set<JIPipeExpressionParameterVariableInfo> variables = new HashSet<>();
            variables.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            for (Map.Entry<UUID, Map<String, JIPipeDataTable>> uuidMapEntry : predecessorNodeCache.entrySet()) {
                for (Map.Entry<String, JIPipeDataTable> dataTableEntry : uuidMapEntry.getValue().entrySet()) {
                    for (String annotationColumn : dataTableEntry.getValue().getTextAnnotationColumnNames()) {
                        variables.add(new JIPipeExpressionParameterVariableInfo(annotationColumn, annotationColumn + " (Cached annotation)",
                                "The text annotation \"" + annotationColumn + "\". Automatically detected from cached predecessor nodes."
                        ));
                    }
                }
            }

            return variables;
        } else {
            return Collections.singleton(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
        }
    }
}
