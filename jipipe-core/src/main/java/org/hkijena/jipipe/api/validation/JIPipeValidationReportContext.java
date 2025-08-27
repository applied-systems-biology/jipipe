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

package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.contexts.*;
import org.hkijena.jipipe.JIPipeDependency;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class JIPipeValidationReportContext {

    public static final UnspecifiedValidationReportContext UNSPECIFIED = new UnspecifiedValidationReportContext();

    private final JIPipeValidationReportContext parent;

    public JIPipeValidationReportContext() {
        if (getClass() != UnspecifiedValidationReportContext.class)
            this.parent = new UnspecifiedValidationReportContext();
        else
            this.parent = null;
    }

    public JIPipeValidationReportContext(JIPipeValidationReportContext parent) {
        if (getClass() != UnspecifiedValidationReportContext.class)
            this.parent = parent != null ? parent : new UnspecifiedValidationReportContext();
        else
            this.parent = null;
    }

    public JIPipeValidationReportContext getParent() {
        return parent;
    }

    public abstract String renderName();

    public String renderDetailedName() {
        return renderName();
    }

    public abstract Icon renderIcon();

    public List<JIPipeValidationReportContext> traverse() {
        List<JIPipeValidationReportContext> contexts = new ArrayList<>();
        JIPipeValidationReportContext currentContext = this;
        while (currentContext != null) {
            contexts.add(currentContext);
            currentContext = currentContext.getParent();
        }
        return contexts;
    }

    public List<NavigableJIPipeValidationReportContext> traverseNavigable() {
        List<NavigableJIPipeValidationReportContext> contexts = new ArrayList<>();
        JIPipeValidationReportContext currentContext = this;
        while (currentContext != null) {
            if (currentContext instanceof NavigableJIPipeValidationReportContext) {
                contexts.add((NavigableJIPipeValidationReportContext) currentContext);
            }
            currentContext = currentContext.getParent();
        }
        return contexts;
    }

    // Factory methods for creating context instances
    
    /**
     * Creates a project validation context.
     * @param project the project
     * @return new ProjectValidationReportContext instance
     */
    public ProjectValidationReportContext project(JIPipeProject project) {
        return new ProjectValidationReportContext(this, project);
    }
    
    /**
     * Creates a graph validation context.
     * @param graph the graph
     * @return new GraphValidationReportContext instance
     */
    public GraphValidationReportContext graph(JIPipeGraph graph) {
        return new GraphValidationReportContext(this, graph);
    }
    
    /**
     * Creates a graph node validation context.
     * @param graphNode the graph node
     * @return new GraphNodeValidationReportContext instance
     */
    public GraphNodeValidationReportContext node(JIPipeGraphNode graphNode) {
        return new GraphNodeValidationReportContext(this, graphNode);
    }
    
    /**
     * Creates a graph node slot validation context.
     * @param graphNode the graph node
     * @param slotName the slot name
     * @param slotType the slot type
     * @return new GraphNodeSlotValidationReportContext instance
     */
    public GraphNodeSlotValidationReportContext slot(JIPipeGraphNode graphNode, String slotName, JIPipeSlotType slotType) {
        return new GraphNodeSlotValidationReportContext(this, graphNode, slotName, slotType);
    }
    
    /**
     * Creates a parameter validation context.
     * @param parameterCollection the parameter collection
     * @param name the parameter name
     * @param key the parameter key
     * @return new ParameterValidationReportContext instance
     */
    public ParameterValidationReportContext parameter(JIPipeParameterCollection parameterCollection, String name, String key) {
        return new ParameterValidationReportContext(this, parameterCollection, name, key);
    }
    
    /**
     * Creates a project settings validation context.
     * @param project the project
     * @return new ProjectSettingsValidationReportContext instance
     */
    public ProjectSettingsValidationReportContext projectSettings(JIPipeProject project) {
        return new ProjectSettingsValidationReportContext(project);
    }
    
    /**
     * Creates an internal error validation context.
     * @return new InternalErrorValidationReportContext instance
     */
    public InternalErrorValidationReportContext internalError() {
        return new InternalErrorValidationReportContext(this);
    }
    
    /**
     * Creates an API error validation context.
     * @return new APIErrorValidationReportContext instance
     */
    public APIErrorValidationReportContext apiError() {
        return new APIErrorValidationReportContext(this);
    }
    
    /**
     * Creates a custom validation context.
     * @param name the context name
     * @return new CustomValidationReportContext instance
     */
    public CustomValidationReportContext custom(String name) {
        return new CustomValidationReportContext(this, name);
    }
    
    /**
     * Creates a custom validation context with icon.
     * @param name the context name
     * @param icon the context icon
     * @return new CustomValidationReportContext instance
     */
    public CustomValidationReportContext custom(String name, Icon icon) {
        return new CustomValidationReportContext(this, name, icon);
    }
    
    /**
     * Creates a Java extension validation context.
     * @param extension the JIPipe dependency/extension
     * @return new JavaExtensionValidationReportContext instance
     */
    public JavaExtensionValidationReportContext extension(JIPipeDependency extension) {
        return new JavaExtensionValidationReportContext(this, extension);
    }
    
    /**
     * Creates a JSON node info validation context.
     * @param nodeInfo the JSON node information
     * @return new JsonNodeInfoValidationReportContext instance
     */
    public JsonNodeInfoValidationReportContext jsonNode(JsonNodeInfo nodeInfo) {
        return new JsonNodeInfoValidationReportContext(this, nodeInfo);
    }
    
    // Common builder starters for frequent use cases
    
    /**
     * Creates a builder for null check validation reports
     * @param objectName the name of the object being checked
     * @return builder instance
     */
    public ValidationEntryBuilder assertNull(String objectName) {
        return error().title("Null check failed: " + objectName)
                     .explanation("The required object '" + objectName + "' is null.");
    }
    
    /**
     * Creates a builder for empty string validation reports
     * @param fieldName the name of the field being checked
     * @return builder instance
     */
    public ValidationEntryBuilder assertNotEmpty(String fieldName) {
        return error().title("Empty value: " + fieldName)
                     .explanation("The required field '" + fieldName + "' cannot be empty.");
    }
    
    /**
     * Creates a builder for file existence validation reports
     * @param filePath the path of the file being checked
     * @return builder instance
     */
    public ValidationEntryBuilder assertFileExists(String filePath) {
        return error().title("File not found: " + filePath)
                     .explanation("The required file '" + filePath + "' does not exist.");
    }
    
    /**
     * Creates a builder for directory existence validation reports
     * @param dirPath the path of the directory being checked
     * @return builder instance
     */
    public ValidationEntryBuilder assertDirectoryExists(String dirPath) {
        return error().title("Directory not found: " + dirPath)
                     .explanation("The required directory '" + dirPath + "' does not exist.");
    }
    
    // Builder pattern for creating validation report entries
    
    /**
     * Creates a builder for error-level validation reports
     * @return builder instance
     */
    public ValidationEntryBuilder error() {
        return new ValidationEntryBuilder(JIPipeValidationReportEntryLevel.Error, this);
    }
    
    /**
     * Creates a builder for warning-level validation reports
     * @return builder instance
     */
    public ValidationEntryBuilder warning() {
        return new ValidationEntryBuilder(JIPipeValidationReportEntryLevel.Warning, this);
    }
    
    /**
     * Creates a builder for custom level validation reports
     * @param level the validation level
     * @return builder instance
     */
    public ValidationEntryBuilder on(JIPipeValidationReportEntryLevel level) {
        return new ValidationEntryBuilder(level, this);
    }
    
    /**
     * Builder class for creating validation report entries with a fluent API
     */
    public static class ValidationEntryBuilder {
        private final JIPipeValidationReportEntryLevel level;
        private final JIPipeValidationReportContext context;
        private String title;
        private String explanation;
        private String solution;
        private String details;
        
        private ValidationEntryBuilder(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context) {
            this.level = level;
            this.context = context;
        }
        
        /**
         * Sets the title of the validation report entry
         * @param title the title
         * @return builder instance for chaining
         */
        public ValidationEntryBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Sets the explanation of the validation report entry
         * @param explanation the explanation
         * @return builder instance for chaining
         */
        public ValidationEntryBuilder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }
        
        /**
         * Sets the solution of the validation report entry
         * @param solution the solution
         * @return builder instance for chaining
         */
        public ValidationEntryBuilder solution(String solution) {
            this.solution = solution;
            return this;
        }
        
        /**
         * Sets the details of the validation report entry
         * @param details the details
         * @return builder instance for chaining
         */
        public ValidationEntryBuilder details(String details) {
            this.details = details;
            return this;
        }
        
        /**
         * Creates and adds a validation report entry to the provided report
         * @param report the report to add the entry to
         */
        public void report(JIPipeValidationReport report) {
            JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(level, context, title, explanation, solution, details);
            report.add(entry);
        }
        
        /**
         * Creates and returns a validation report entry without adding it to a report
         * @return the created validation report entry
         */
        public JIPipeValidationReportEntry build() {
            return new JIPipeValidationReportEntry(level, context, title, explanation, solution, details);
        }
    }

}
