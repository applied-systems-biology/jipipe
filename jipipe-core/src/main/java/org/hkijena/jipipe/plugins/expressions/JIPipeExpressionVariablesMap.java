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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object that carries variables for expressions
 */
public class JIPipeExpressionVariablesMap extends HashMap<String, Object> {
    public JIPipeExpressionVariablesMap() {
    }

    public JIPipeExpressionVariablesMap(JIPipeExpressionVariablesMap other) {
        putAll(other);
    }

    public JIPipeExpressionVariablesMap(JIPipeSingleIterationStep iterationStep) {
        putCommonVariables(iterationStep);
    }

    public JIPipeExpressionVariablesMap(JIPipeMultiIterationStep iterationStep) {
        putCommonVariables(iterationStep);
    }

    public JIPipeExpressionVariablesMap(JIPipeAlgorithm node) {
        putCommonVariables(node);
    }

    /**
     * Sets a variable value.
     *
     * @param variableName The variable name
     * @param value        The variable value (null to remove a variable from the set).
     * @return this
     */
    public JIPipeExpressionVariablesMap set(String variableName, Object value) {
        this.put(variableName, value);
        return this;
    }

    /**
     * Puts annotations into the variables
     *
     * @param mergedTextAnnotations the annotations
     */
    public JIPipeExpressionVariablesMap putAnnotations(Map<String, JIPipeTextAnnotation> mergedTextAnnotations) {
        for (Entry<String, JIPipeTextAnnotation> entry : mergedTextAnnotations.entrySet()) {
            put(entry.getKey(), entry.getValue().getValue());
        }
        return this;
    }

    /**
     * Puts annotations into the variables
     *
     * @param textAnnotations the annotations
     * @return this
     */
    public JIPipeExpressionVariablesMap putAnnotations(List<JIPipeTextAnnotation> textAnnotations) {
        for (JIPipeTextAnnotation textAnnotation : textAnnotations) {
            put(textAnnotation.getName(), textAnnotation.getValue());
        }
        return this;
    }

    /**
     * Puts the project-related directories into the variables
     *
     * @param projectDir      the project dir
     * @param projectDataDirs project data dirs
     * @return this
     */
    public JIPipeExpressionVariablesMap putProjectDirectories(Path projectDir, Map<String, Path> projectDataDirs) {
        if (StringUtils.isNullOrEmpty(projectDir)) {
            projectDir = Paths.get("");
        }
        set("project_dir", projectDir.toAbsolutePath().toString());
        Map<String, String> projectDataDirs_ = new HashMap<>();
        for (Map.Entry<String, Path> entry : projectDataDirs.entrySet()) {
            if (!StringUtils.isNullOrEmpty(entry.getKey())) {
                if (entry.getValue().isAbsolute()) {
                    projectDataDirs_.put(entry.getKey(), entry.getValue().toString());
                } else {
                    projectDataDirs_.put(entry.getKey(), projectDir.resolve(entry.getValue()).toString());
                }

                // Shorthand property
                if (JIPipeExpressionParameter.isValidVariableName(entry.getKey())) {
                    set("project_data_dir." + entry.getKey(), projectDataDirs_.get(entry.getKey()));
                }
            }
        }
        set("project_data_dirs", projectDataDirs_);
        return this;
    }

    /**
     * Puts custom expression parameters (with default settings)
     *
     * @param customExpressionVariablesParameter the parameter
     * @return this
     */
    public JIPipeExpressionVariablesMap putCustomVariables(JIPipeCustomExpressionVariablesParameter customExpressionVariablesParameter) {
        customExpressionVariablesParameter.writeToVariables(this);
        return this;
    }

    public JIPipeExpressionVariablesMap putAnnotationsIfAbsent(Collection<JIPipeTextAnnotation> textAnnotations) {
        for (JIPipeTextAnnotation textAnnotation : textAnnotations) {
            putIfAbsent(textAnnotation.getName(), textAnnotation.getValue());
        }
        return this;
    }

    public JIPipeExpressionVariablesMap putGlobalVariables(JIPipeProject project) {
        if(project == null) {
            return this;
        }
        for (Entry<String, JIPipeParameterAccess> entry : project.getMetadata().getGlobalParameters().getParameters().entrySet()) {
            set("_global." + entry.getKey(), entry.getValue().get(Object.class));
        }
        return this;
    }

    public JIPipeExpressionVariablesMap putCommonVariables(JIPipeSingleIterationStep iterationStep) {
        if(iterationStep.getNode() instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm node = (JIPipeAlgorithm) iterationStep.getNode();
            putCommonVariables(node);
        }
        Map<String, JIPipeTextAnnotation> annotations = iterationStep.getMergedTextAnnotations();
        putAnnotations(annotations);
        putAnnotationsMap(annotations);
        return this;
    }

    private JIPipeExpressionVariablesMap putAnnotationsMap(Map<String, JIPipeTextAnnotation> annotations) {
        set("_local.annotations", JIPipeTextAnnotation.annotationListToMap(annotations.values(), JIPipeTextAnnotationMergeMode.OverwriteExisting));
        return this;
    }

    public JIPipeExpressionVariablesMap putCommonVariables(JIPipeMultiIterationStep iterationStep) {
        if(iterationStep.getNode() instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm node = (JIPipeAlgorithm) iterationStep.getNode();
            putCommonVariables(node);
        }
        Map<String, JIPipeTextAnnotation> annotations = iterationStep.getMergedTextAnnotations();
        putAnnotations(annotations);
        putAnnotationsMap(annotations);
        return this;
    }

    public JIPipeExpressionVariablesMap putCommonVariables(JIPipeAlgorithm node) {
        putCustomVariables(node.getDefaultCustomExpressionVariables());
        putProjectDirectories(node.getProjectDirectory(), node.getProjectDataDirs());
        putGlobalVariables(node.getProject());
        return this;
    }
}
